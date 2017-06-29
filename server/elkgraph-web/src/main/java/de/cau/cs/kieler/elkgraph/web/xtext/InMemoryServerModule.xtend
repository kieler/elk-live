/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web.xtext

import com.google.inject.AbstractModule
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.xtext.ide.server.IWorkspaceConfigFactory
import org.eclipse.xtext.resource.IResourceServiceProvider

class InMemoryServerModule extends AbstractModule {
	
	override protected configure() {
		bind(IResourceServiceProvider.Registry).toProvider(IResourceServiceProvider.Registry.RegistryProvider)
		bind(LanguageServer).to(LanguageServerImpl)
		bind(IWorkspaceConfigFactory).to(InMemoryWorkspaceConfigFactory)
	}
	
}