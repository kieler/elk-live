package de.cau.cs.kieler.elkgraph.web

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import io.typefox.sprotty.api.BoundsAware
import io.typefox.sprotty.api.Dimension
import io.typefox.sprotty.api.Point
import io.typefox.sprotty.api.SEdge
import io.typefox.sprotty.api.SGraph
import io.typefox.sprotty.api.SLabel
import io.typefox.sprotty.api.SModelElement
import io.typefox.sprotty.api.SNode
import io.typefox.sprotty.api.SPort
import org.eclipse.elk.core.IGraphLayoutEngine
import org.eclipse.elk.core.RecursiveGraphLayoutEngine
import org.eclipse.elk.core.util.BasicProgressMonitor
import org.eclipse.elk.graph.ElkGraphElement
import org.eclipse.elk.graph.ElkNode
import org.eclipse.elk.graph.ElkShape
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtend.lib.annotations.Accessors

class ElkGraphDiagramGenerator {
	
	val IGraphLayoutEngine layoutEngine = new RecursiveGraphLayoutEngine
	
	@Accessors(PUBLIC_SETTER)
	int defaultPortSize = 5
	
	@Accessors(PUBLIC_SETTER)
	int defaultNodeSize = 20
	
	def void layout(ElkNode elkGraph) {
		applyDefaults(elkGraph)
		layoutEngine.layout(elkGraph, new BasicProgressMonitor)
	}
	
	private def void applyDefaults(ElkNode parent) {
		for (port : parent.ports) {
			if (port.width <= 0)
				port.width = defaultPortSize
			if (port.height <= 0)
				port.height = defaultPortSize
		}
		for (node : parent.children) {
			if (node.width <= 0)
				node.width = defaultNodeSize
			if (node.height <= 0)
				node.height = defaultNodeSize
		}
	}
	
	def BiMap<EObject, SModelElement> generateDiagram(ElkNode elkGraph) {
		layout(elkGraph)
		val mapping = HashBiMap.create()
		val sgraph = new SGraph
		sgraph.type = 'graph'
		sgraph.id = elkGraph.id
		mapping.put(elkGraph, sgraph)
		processContent(elkGraph, sgraph, mapping)
		return mapping
	}
	
	protected def void processContent(ElkNode parent, SModelElement container,
			BiMap<EObject, SModelElement> mapping) {
		for (elkPort : parent.ports) {
			val sport = new SPort
			sport.type = 'port'
			sport.id = elkPort.id
			transferBounds(elkPort, sport)
			container.children += sport
			mapping.put(elkPort, sport)
			processLabels(elkPort, sport, mapping)
		}
		for (elkNode : parent.children) {
			val snode = new SNode
			snode.type = 'node'
			snode.id = elkNode.id
			transferBounds(elkNode, snode)
			container.children += snode
			mapping.put(elkNode, snode)
			processLabels(elkNode, snode, mapping)
			processContent(elkNode, snode, mapping)
		}
		for (elkEdge : parent.containedEdges) {
			if (elkEdge.sources.size == 1 && elkEdge.targets.size == 1) {
				val sedge = new SEdge
				sedge.type = 'edge'
				sedge.id = elkEdge.id
				sedge.sourceId = elkEdge.sources.head.id
				sedge.targetId = elkEdge.targets.head.id
				container.children += sedge
				mapping.put(elkEdge, sedge)
				processLabels(elkEdge, sedge, mapping)
			} else {
				for (source : elkEdge.sources) {
					for (target : elkEdge.targets) {
						val sedge = new SEdge
						sedge.type = 'edge'
						sedge.id = elkEdge.id + '_' + source.id + '_' + target.id
						sedge.sourceId = source.id
						sedge.targetId = target.id
						container.children += sedge
						mapping.put(elkEdge, sedge)
						processLabels(elkEdge, sedge, mapping)
					}
				}
			}
		}
	}
	
	protected def void processLabels(ElkGraphElement element, SModelElement container,
			BiMap<EObject, SModelElement> mapping) {
		for (elkLabel : element.labels) {
			val slabel = new SLabel
			slabel.type = 'label'
			slabel.id = elkLabel.id
			transferBounds(elkLabel, slabel)
			container.children += slabel
			mapping.put(elkLabel, slabel)
			processLabels(elkLabel, slabel, mapping)
		}
	}
	
	private def void transferBounds(ElkShape shape, BoundsAware bounds) {
		bounds.position = new Point(shape.x, shape.y)
		if (shape.width > 0 || shape.height > 0)
			bounds.size = new Dimension(shape.width, shape.height)
	}
	
	private def String getId(ElkGraphElement element) {
		val container = element.eContainer
		if (container instanceof ElkGraphElement)
			container.id + '.' + element.identifier
		else
			element.identifier
	}
	
}