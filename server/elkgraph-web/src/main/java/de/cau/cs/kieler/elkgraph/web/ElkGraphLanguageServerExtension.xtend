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

/**
 * The language server extension is created by the {@link LanguageServerImpl}, which is in turn
 * created by the {@link LanguageServerEndpoint}. This extension takes care of generating diagrams
 * for the ELK graphs written in the text editor.
 */
class ElkGraphLanguageServerExtension extends DiagramLanguageServerExtension {
	
	override protected initializeDiagramServer(IDiagramServer server) {
		super.initializeDiagramServer(server)
		val languageAware = server as LanguageAwareDiagramServer
		languageAware.needsClientLayout = false
	}
	
	/**
	 * The client identifier is hard-coded here so we can notify the client about a new diagram
	 * although we never received a request for it. This is possible because there is always
	 * exactly one client for each instance of this class.
	 */
	override protected findDiagramServersByUri(String uri) {
		#[getDiagramServer('sprotty')]
	}

}
