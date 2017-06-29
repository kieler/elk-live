/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import io.typefox.sprotty.api.IDiagramSelectionListener
import io.typefox.sprotty.api.IDiagramServer
import io.typefox.sprotty.api.ILayoutEngine
import io.typefox.sprotty.api.IModelUpdateListener
import io.typefox.sprotty.api.IPopupModelFactory
import org.eclipse.xtext.ide.server.ILanguageServerExtension
import org.eclipse.xtext.service.AbstractGenericModule

class ElkGraphDiagramModule extends AbstractGenericModule {
	
	def Class<? extends ILanguageServerExtension> bindILanguageServerExtension() {
		DiagramLanguageServerImpl
	}
	
	def Class<? extends IDiagramServer.Provider> bindIDiagramServerProvider() {
		DiagramLanguageServerImpl
	}
	
	def Class<? extends IModelUpdateListener> bindIModelUpdateListener() {
		IModelUpdateListener.NullImpl
	}
	
	def Class<? extends ILayoutEngine> bindILayoutEngine() {
		ILayoutEngine.NullImpl
	}
	
	def Class<? extends IPopupModelFactory> bindIPopupModelFactory() {
		IPopupModelFactory.NullImpl
	}
	
	def Class<? extends IDiagramSelectionListener> bindIDiagramSelectionListener() {
		IDiagramSelectionListener.NullImpl
	}
	
}