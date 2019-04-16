/*******************************************************************************
 * Copyright (c) 2019 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import org.eclipse.sprotty.ServerLayoutKind
import org.eclipse.sprotty.xtext.DiagramServerFactory
import org.eclipse.sprotty.xtext.LanguageAwareDiagramServer

class ElkGraphDiagramServerFactory extends DiagramServerFactory {
	
	override getDiagramTypes() {
		#['graph']
	}
	
	override createDiagramServer(String diagramType, String clientId) {
		super.createDiagramServer(diagramType, clientId) => [ server |
			if (server instanceof LanguageAwareDiagramServer) {
				server.needsClientLayout = false
				server.serverLayoutKind = ServerLayoutKind.AUTOMATIC
			}
		]
	}
	
}