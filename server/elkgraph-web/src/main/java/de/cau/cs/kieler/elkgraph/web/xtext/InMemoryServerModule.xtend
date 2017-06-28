package de.cau.cs.kieler.elkgraph.web.xtext

import com.google.inject.AbstractModule
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.xtext.ide.server.IWorkspaceConfigFactory
import org.eclipse.xtext.resource.IResourceServiceProvider

class InMemoryServerModule extends AbstractModule {
	
	override protected configure() {
		bind(LanguageServer).to(LanguageServerImpl)
		bind(IResourceServiceProvider.Registry).toProvider(IResourceServiceProvider.Registry.RegistryProvider)
		bind(IWorkspaceConfigFactory).to(InMemoryWorkspaceConfigFactory)
		bind(org.eclipse.xtext.ide.server.ProjectManager).to(ProjectManager)
	}
	
}