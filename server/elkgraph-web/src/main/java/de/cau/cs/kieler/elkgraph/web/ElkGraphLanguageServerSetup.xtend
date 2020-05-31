/*******************************************************************************
 * Copyright (c) 2019 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import com.google.inject.Guice
import com.google.inject.Injector
import javax.websocket.Endpoint
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.core.util.persistence.ElkGraphResourceFactory
import org.eclipse.elk.graph.ElkGraphPackage
import org.eclipse.elk.graph.json.text.ide.ElkGraphJsonIdeSetup
import org.eclipse.elk.graph.text.ElkGraphRuntimeModule
import org.eclipse.elk.graph.text.ide.ElkGraphIdeModule
import org.eclipse.elk.graph.text.ide.ElkGraphIdeSetup
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.sprotty.ActionMessage
import org.eclipse.sprotty.DiagramOptions
import org.eclipse.sprotty.RequestModelAction
import org.eclipse.sprotty.xtext.launch.DiagramLanguageServerSetup
import org.eclipse.sprotty.xtext.ls.DiagramLanguageServer
import org.eclipse.sprotty.xtext.ls.DiagramServerModule
import org.eclipse.xtext.ide.server.ILanguageServerShutdownAndExitHandler
import org.eclipse.xtext.ide.server.ServerModule
import org.eclipse.xtext.ide.server.UriExtensions
import org.eclipse.xtext.resource.IResourceServiceProvider
import org.eclipse.xtext.util.Modules2

/**
 * Configuration of global settings, language server Guice module, and language server setup.
 */
class ElkGraphLanguageServerSetup extends DiagramLanguageServerSetup {
	
	/**
	 * Initialize global settings for ELK and the ELK Graph language.
	 */
	override setupLanguages() {
		// Initialize the ELK Graph Xtext language
		ElkGraphPackage.eINSTANCE.getNsURI
		new ElkGraphIdeSetup {
			override createInjector() {
				Guice.createInjector(Modules2.mixin(new ElkGraphRuntimeModule, new ElkGraphIdeModule, new ElkGraphDiagramModule))
			}
		}.createInjectorAndDoEMFRegistration()
		
		// Do _not_ use 'doSetup()' here as that wouldn't include the 'ElkGraphJsonIdeModule'
		// and thus wouldn't bind the proposal provider
		new ElkGraphJsonIdeSetup().createInjectorAndDoEMFRegistration
		
		// Initialize ELKG Graph XMI format
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("elkg", new ElkGraphResourceFactory());
        ElkGraphPackage.eINSTANCE.eClass();
        
        // Make sure the meta data service has been constructed and ElkReflect has been registered
        LayoutMetaDataService.instance
	}
	
	/**
	 * Create a Guice module for the language server. The language server uses a Guice injector that is separate
	 * from the injectors of each Xtext language.
	 */
	override getLanguageServerModule() {
		Modules2.mixin(
			new ServerModule,
			new DiagramServerModule,
			[ bind(Endpoint).to(LanguageServerEndpoint) ],
			[ bind(ILanguageServerShutdownAndExitHandler).to(ILanguageServerShutdownAndExitHandler.NullImpl) ],
			[ bind(IResourceServiceProvider.Registry).toProvider(IResourceServiceProvider.Registry.RegistryProvider) ]
		)
	}
	
	/**
	 * Set up the language server in the given injector. A build listener is added that creates a
	 * {@link RequestModelAction} for each new document that is opened.
	 */
	def setupLanguageServer(Injector injector) {
		val languageServer = injector.getInstance(LanguageServer)
		if (languageServer instanceof DiagramLanguageServer) {
			val extension uriExtensions = injector.getInstance(UriExtensions)
			languageServer.languageServerAccess.addBuildListener[ deltas |
				deltas.filter[old === null].forEach[ delta |
					languageServer.accept(new ActionMessage => [
						clientId = 'sprotty'
						action = new RequestModelAction => [
							diagramType = 'graph'
							options = #{DiagramOptions.OPTION_SOURCE_URI -> delta.uri.toUriString}
						]
					])
				]
			]
		}
	}
	
}