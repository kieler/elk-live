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