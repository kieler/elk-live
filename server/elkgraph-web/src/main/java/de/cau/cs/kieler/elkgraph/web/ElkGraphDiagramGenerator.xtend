/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import io.typefox.sprotty.api.BoundsAware
import io.typefox.sprotty.api.Dimension
import io.typefox.sprotty.api.Point
import io.typefox.sprotty.api.SEdge
import io.typefox.sprotty.api.SGraph
import io.typefox.sprotty.api.SLabel
import io.typefox.sprotty.api.SModelElement
import io.typefox.sprotty.api.SNode
import io.typefox.sprotty.api.SPort
import io.typefox.sprotty.server.xtext.IDiagramGenerator
import java.util.List
import org.eclipse.elk.core.IGraphLayoutEngine
import org.eclipse.elk.core.RecursiveGraphLayoutEngine
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.core.util.BasicProgressMonitor
import org.eclipse.elk.graph.ElkEdge
import org.eclipse.elk.graph.ElkGraphElement
import org.eclipse.elk.graph.ElkNode
import org.eclipse.elk.graph.ElkShape
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtext.util.CancelIndicator

class ElkGraphDiagramGenerator implements IDiagramGenerator {
	
	val IGraphLayoutEngine layoutEngine = new RecursiveGraphLayoutEngine
	
	@Accessors(PUBLIC_SETTER)
	int defaultPortSize = 5
	
	@Accessors(PUBLIC_SETTER)
	int defaultNodeSize = 30
	
	def void layout(ElkNode elkGraph) {
		applyDefaults(elkGraph)
		layoutEngine.layout(elkGraph, new BasicProgressMonitor)
	}
	
	override generate(Resource resource, CancelIndicator cancelIndicator) {
		val originalGraph = resource.contents.head
		if (originalGraph instanceof ElkNode) {
			val elkGraph = EcoreUtil.copy(originalGraph)
			layout(elkGraph)
			val sgraph = new SGraph
			sgraph.type = 'graph'
			sgraph.id = elkGraph.id
			processContent(elkGraph, sgraph)
			return sgraph
		}
	}
	
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
	
	private def void addChild(SModelElement container, SModelElement child) {
		if (container.children === null)
			container.children = newArrayList
		container.children.add(child)
	}
	
	private def void transferBounds(ElkShape shape, BoundsAware bounds) {
		bounds.position = new Point(shape.x, shape.y)
		if (shape.width > 0 || shape.height > 0)
			bounds.size = new Dimension(shape.width, shape.height)
	}
	
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
		junctionPoints.forEach[ point, index |
			val sJunction = new SNode
			sJunction.type = 'junction'
			sJunction.id = elkEdge.id + '_j' + index
			sJunction.position = new Point(point.x, point.y)
			sEdge.addChild(sJunction)
		]
	}
	
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
	
}