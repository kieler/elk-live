/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import org.eclipse.sprotty.xtext.DefaultDiagramModule
import org.eclipse.sprotty.xtext.IDiagramGenerator

/**
 * Guice bindings for the ELK diagram server.
 */
class ElkGraphDiagramModule extends DefaultDiagramModule {
	
	def Class<? extends IDiagramGenerator> bindIDiagramGenerator() {
		ElkGraphDiagramGenerator
	}
	
	override bindIDiagramServerFactory() {
		ElkGraphDiagramServerFactory
	}
	
    override bindIDiagramServer() {
        ElkDiagramServer
    }
    
}