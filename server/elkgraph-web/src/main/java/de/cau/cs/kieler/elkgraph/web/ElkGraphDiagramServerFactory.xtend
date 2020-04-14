/*******************************************************************************
 * Copyright (c) 2019 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import org.eclipse.sprotty.xtext.DiagramServerFactory

/**
 * Factory for Sprotty diagram servers.
 */
class ElkGraphDiagramServerFactory extends DiagramServerFactory {
	
	override getDiagramTypes() {
		#['graph']
	}
	
}