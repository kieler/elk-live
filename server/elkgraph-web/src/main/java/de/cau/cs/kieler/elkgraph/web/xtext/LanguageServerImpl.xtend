package de.cau.cs.kieler.elkgraph.web.xtext

import org.eclipse.lsp4j.InitializeParams

class LanguageServerImpl extends org.eclipse.xtext.ide.server.LanguageServerImpl {
	
	override initialize(InitializeParams params) {
		if (params.rootUri === null)
			params.rootUri = InMemorySourceFolder.BASE_URI
		super.initialize(params)
	}
	
}