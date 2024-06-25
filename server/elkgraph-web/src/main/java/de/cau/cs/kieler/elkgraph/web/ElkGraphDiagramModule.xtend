/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
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