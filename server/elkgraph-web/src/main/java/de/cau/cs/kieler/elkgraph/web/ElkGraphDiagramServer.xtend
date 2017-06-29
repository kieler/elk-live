package de.cau.cs.kieler.elkgraph.web

import io.typefox.sprotty.api.DefaultDiagramServer
import io.typefox.sprotty.api.RequestModelAction
import io.typefox.sprotty.api.SModelRoot
import org.eclipse.xtend.lib.annotations.Accessors

class ElkGraphDiagramServer extends DefaultDiagramServer {
	
	@Accessors
	DiagramLanguageServerImpl diagramLanguageServer
	
	override protected needsClientLayout(SModelRoot root) {
		false
	}
	
	override protected handle(RequestModelAction request) {
		diagramLanguageServer.initializeModel(this)
	}
	
}