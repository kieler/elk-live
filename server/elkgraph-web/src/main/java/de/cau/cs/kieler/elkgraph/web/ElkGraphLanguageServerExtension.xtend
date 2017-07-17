/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import io.typefox.sprotty.api.IDiagramServer
import io.typefox.sprotty.server.xtext.DiagramLanguageServerExtension
import io.typefox.sprotty.server.xtext.LanguageAwareDiagramServer

class ElkGraphLanguageServerExtension extends DiagramLanguageServerExtension {
	
	override protected initializeDiagramServer(IDiagramServer server) {
		super.initializeDiagramServer(server)
		val languageAware = server as LanguageAwareDiagramServer
		languageAware.needsClientLayout = false
	}
	
	override protected findDiagramServersByUri(String uri) {
		#[getDiagramServer('sprotty')]
	}

}
