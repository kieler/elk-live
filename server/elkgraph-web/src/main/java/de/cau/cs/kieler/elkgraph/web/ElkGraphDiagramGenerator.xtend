/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import java.util.List
import java.util.logging.Level
import java.util.logging.Logger
import org.eclipse.elk.core.IGraphLayoutEngine
import org.eclipse.elk.core.RecursiveGraphLayoutEngine
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.core.util.BasicProgressMonitor
import org.eclipse.elk.graph.ElkEdge
import org.eclipse.elk.graph.ElkGraphElement
import org.eclipse.elk.graph.ElkNode
import org.eclipse.elk.graph.ElkShape
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.sprotty.BoundsAware
import org.eclipse.sprotty.Dimension
import org.eclipse.sprotty.Point
import org.eclipse.sprotty.SEdge
import org.eclipse.sprotty.SGraph
import org.eclipse.sprotty.SLabel
import org.eclipse.sprotty.SModelElement
import org.eclipse.sprotty.SNode
import org.eclipse.sprotty.SPort
import org.eclipse.sprotty.xtext.IDiagramGenerator
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.elk.core.UnsupportedConfigurationException

/**
 * Transforms ELK graphs into sprotty models to be transferred to clients.
 */
class ElkGraphDiagramGenerator implements IDiagramGenerator {

    static val LOG = Logger.getLogger(ElkGraphDiagramGenerator.name)

	val IGraphLayoutEngine layoutEngine = new RecursiveGraphLayoutEngine
	
    @Accessors(PUBLIC_SETTER) 
    int defaultPortSize = 5

    @Accessors(PUBLIC_SETTER)
    int defaultNodeSize = 30

    override generate(Context context) {
        val originalGraph = context.resource.contents.head
        if (originalGraph instanceof ElkNode) { 
            try {
                // Add some sizes values to make the results look nice by default
                val elkGraph = EcoreUtil.copy(originalGraph)
                applyDefaults(elkGraph)

                val layoutVersion = context.state.options.get("layoutVersion")
                
                val laidOutGraph = if (layoutVersion == "snapshot") {
                    layoutEngine.layout(elkGraph, new BasicProgressMonitor)
                    elkGraph
                } else if (ElkLayoutVersionRegistry.versionToWrapper.containsKey(layoutVersion)) {
                    val wrapper = ElkLayoutVersionRegistry.versionToWrapper.get(layoutVersion)
                    val result = wrapper.layout(elkGraph)
                    if (!result.isPresent) {
                    	throw new RuntimeException("Layout failed for version " + layoutVersion + ".")
                    }
                    result.get
                } else {
                    throw new UnsupportedConfigurationException("Unknown layouter version: " + layoutVersion + ".")
                }
                
                val sgraph = new SGraph
                sgraph.type = 'graph'
                sgraph.id = elkGraph.id
                processContent(laidOutGraph, sgraph)
                return sgraph
            } catch (RuntimeException exc) {
                LOG.log(Level.SEVERE, "Failed to generate ELK graph.", exc)
                return showError(exc)
            }
        }
    }

    /**
     * Transform all content of the given parent node.
     */
	protected def void processContent(ElkNode parent, SModelElement container) {
        for (elkPort : parent.ports) {
            val sport = new SPort
            sport.type = 'port'
            sport.id = elkPort.id
            transferBounds(elkPort, sport)
            container.addChild(sport)
            processLabels(elkPort, sport)
        }
        for (elkNode : parent.children) {
            val snode = new SNode
            snode.type = 'node'
            snode.id = elkNode.id
            transferBounds(elkNode, snode)
            container.addChild(snode)
            processLabels(elkNode, snode)
            processContent(elkNode, snode)
        }
        for (elkEdge : parent.containedEdges) {
            if (elkEdge.sources.size == 1 && elkEdge.targets.size == 1) {
                val sedge = new SEdge
                sedge.type = 'edge'
                sedge.id = elkEdge.id
                sedge.sourceId = elkEdge.sources.head.id
                sedge.targetId = elkEdge.targets.head.id
                transferEdgeLayout(elkEdge, sedge)
                container.addChild(sedge)
                processLabels(elkEdge, sedge)
            } else {
                for (source : elkEdge.sources) {
                    for (target : elkEdge.targets) {
                        val sedge = new SEdge
                        sedge.type = 'edge'
                        sedge.id = elkEdge.id + '_' + source.id + '_' + target.id
                        sedge.sourceId = source.id
                        sedge.targetId = target.id
                        transferEdgeLayout(elkEdge, sedge)
                        container.addChild(sedge)
                        processLabels(elkEdge, sedge)
                    }
                }
            }
        }
    }

    /**
     * Transform all labels of the given graph element.
     */
    protected def void processLabels(ElkGraphElement element, SModelElement container) {
        for (elkLabel : element.labels) {
            val slabel = new SLabel
            slabel.type = 'label'
            slabel.id = elkLabel.id
            slabel.text = elkLabel.text
            transferBounds(elkLabel, slabel)
            container.addChild(slabel)
            processLabels(elkLabel, slabel)
        }
    }

	/**
	 * Apply default layout information to all contents of the given parent node.
	 */
	private def void applyDefaults(ElkNode parent) {
		for (port : parent.ports) {
			if (port.width <= 0)
				port.width = defaultPortSize
			if (port.height <= 0)
				port.height = defaultPortSize
			computeLabelSizes(port)
		}
		for (node : parent.children) {
			if (node.width <= 0)
				node.width = defaultNodeSize
			if (node.height <= 0)
				node.height = defaultNodeSize
			computeLabelSizes(node)
			applyDefaults(node)
		}
		for (edge : parent.containedEdges) {
			computeLabelSizes(edge)
		}
	}
	
	/**
	 * Compute sizes for all labels of an element. <em>Note:</em> Sizes are hard-coded here, so don't expect
	 * the result to be rendered properly on all clients!
	 */
	private def computeLabelSizes(ElkGraphElement element) {
		for (label : element.labels) {
			if (!label.text.nullOrEmpty) {
				if (label.width <= 0)
					label.width = label.text.length * 9
				if (label.height <= 0)
					label.height = 16
			}
		}
	}
	
    /**
     * Add a child element to the sprotty model.
     */
    private def void addChild(SModelElement container, SModelElement child) {
        if (container.children === null)
            container.children = newArrayList
        container.children.add(child)
    }

    /**
     * Transfer bounds to a sprotty model element.
     */
    private def void transferBounds(ElkShape shape, BoundsAware bounds) {
        bounds.position = new Point(shape.x, shape.y)
        if (shape.width > 0 || shape.height > 0)
            bounds.size = new Dimension(shape.width, shape.height)
    }

    /**
     * Transfer an edge layout to a sprotty edge.
     */
    private def void transferEdgeLayout(ElkEdge elkEdge, SEdge sEdge) {
        sEdge.routingPoints = newArrayList
        for (section : elkEdge.sections) {
            sEdge.routingPoints += new Point(section.startX, section.startY)
            for (bendPoint : section.bendPoints) {
                sEdge.routingPoints += new Point(bendPoint.x, bendPoint.y)
            }
            sEdge.routingPoints += new Point(section.endX, section.endY)
        }
        val junctionPoints = elkEdge.getProperty(CoreOptions.JUNCTION_POINTS)
        junctionPoints.forEach [ point, index |
            val sJunction = new SNode
            sJunction.type = 'junction'
            sJunction.id = elkEdge.id + '_j' + index
            sJunction.position = new Point(point.x, point.y)
            sEdge.addChild(sJunction)
        ]
    }

    /**
     * Compute a unique identifier for the given element.
     */
    private def String getId(ElkGraphElement element) {
        val container = element.eContainer
        if (container instanceof ElkGraphElement) {
            var identifier = element.identifier
            if (identifier === null) {
                val feature = element.eContainingFeature
                val list = container.eGet(feature) as List<? extends ElkGraphElement>
                identifier = feature.name + '#' + list.indexOf(element)
            }
            return container.id + '.' + identifier
        } else {
            return element.identifier ?: 'graph'
        }
    }

    private def showError(Throwable throwable) {
        val sgraph = new SGraph
        sgraph.type = 'graph'
        val label = new SLabel
        label.type = 'label'
        label.id = 'error'
        label.text = throwable.class.simpleName + ': ' + throwable.message
        label.position = new Point(20, 20)
        sgraph.addChild(label)
        return sgraph
    }

}
