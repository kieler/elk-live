/*******************************************************************************
 * Copyright (c) 2019 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import com.google.inject.Guice
import javax.websocket.Endpoint
import org.eclipse.elk.alg.common.compaction.options.PolyominoOptions
import org.eclipse.elk.alg.disco.options.DisCoMetaDataProvider
import org.eclipse.elk.alg.force.options.ForceMetaDataProvider
import org.eclipse.elk.alg.force.options.StressMetaDataProvider
import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider
import org.eclipse.elk.alg.mrtree.options.MrTreeMetaDataProvider
import org.eclipse.elk.alg.radial.options.RadialMetaDataProvider
import org.eclipse.elk.alg.spore.options.SporeMetaDataProvider
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.graph.ElkGraphPackage
import org.eclipse.elk.graph.text.ElkGraphRuntimeModule
import org.eclipse.elk.graph.text.ide.ElkGraphIdeModule
import org.eclipse.elk.graph.text.ide.ElkGraphIdeSetup
import org.eclipse.sprotty.xtext.launch.DiagramLanguageServerSetup
import org.eclipse.sprotty.xtext.ls.DiagramServerModule
import org.eclipse.sprotty.xtext.websocket.LanguageServerEndpoint
import org.eclipse.xtext.ide.server.ServerModule
import org.eclipse.xtext.resource.IResourceServiceProvider
import org.eclipse.xtext.util.Modules2

class ElkGraphLanguageServerSetup extends DiagramLanguageServerSetup {
	
	override setupLanguages() {
		// Initialize ELK meta data
		LayoutMetaDataService.instance.registerLayoutMetaDataProviders(
			new ForceMetaDataProvider,
			new LayeredMetaDataProvider,
			new MrTreeMetaDataProvider,
			new RadialMetaDataProvider,
			new StressMetaDataProvider,
			new PolyominoOptions, 
			new DisCoMetaDataProvider,
			new SporeMetaDataProvider
		)
		
		// Initialize the ELK Graph Xtext language
		ElkGraphPackage.eINSTANCE.getNsURI
		new ElkGraphIdeSetup {
			override createInjector() {
				Guice.createInjector(Modules2.mixin(new ElkGraphRuntimeModule, new ElkGraphIdeModule, new ElkGraphDiagramModule))
			}
		}.createInjectorAndDoEMFRegistration()
	}
	
	override getLanguageServerModule() {
		Modules2.mixin(
			new ServerModule,
			new DiagramServerModule,
			[ bind(Endpoint).to(LanguageServerEndpoint) ],
			[ bind(IResourceServiceProvider.Registry).toProvider(IResourceServiceProvider.Registry.RegistryProvider) ]
		)
	}
	
}