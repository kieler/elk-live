/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
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