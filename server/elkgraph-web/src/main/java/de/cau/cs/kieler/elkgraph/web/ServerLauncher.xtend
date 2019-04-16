/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Provider
import java.net.InetSocketAddress
import javax.websocket.Endpoint
import javax.websocket.server.ServerEndpointConfig
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.log.Slf4jLog
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer
import org.eclipse.sprotty.xtext.websocket.LanguageServerEndpoint

/**
 * Main class for launching the ELK Graph server.
 */
class ServerLauncher {
	
	def static void main(String[] args) {
		val setup = new ElkGraphLanguageServerSetup
		setup.setupLanguages()
		
		val injector = Guice.createInjector(setup.languageServerModule)
		val launcher = injector.getInstance(ServerLauncher)
		val rootPath = if (args.length >= 1) args.get(0) else '../..'
		launcher.start(rootPath)
	}

	@Inject Provider<Endpoint> endpointProvider
	
	def void start(String rootPath) {
		val log = new Slf4jLog(ServerLauncher.name)
		
		// Set up Jetty server
		val server = new Server(new InetSocketAddress(8080))
		val webAppContext = new WebAppContext => [
			resourceBase = rootPath + '/client/app'
			log.info('Serving client app from ' + resourceBase)
			welcomeFiles = #['index.html']
			setInitParameter('org.eclipse.jetty.servlet.Default.dirAllowed', 'false')
			setInitParameter('org.eclipse.jetty.servlet.Default.useFileMappedBuffer', 'false')
		]
		server.handler = webAppContext
		
		// Configure web socket
		val container = WebSocketServerContainerInitializer.configureContext(webAppContext)
		val endpointConfigBuilder = ServerEndpointConfig.Builder.create(LanguageServerEndpoint, '/elkgraph')
		endpointConfigBuilder.configurator(new ServerEndpointConfig.Configurator {
			override <T> getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
				endpointProvider.get as T
			}
		})
		container.addEndpoint(endpointConfigBuilder.build())
		
		// Start the server
		try {
			server.start()
			log.info('Press enter to stop the server...')
			new Thread[
		    	val key = System.in.read()
		    	server.stop()
		    	if (key == -1)
		    		log.warn('The standard input stream is empty')
		    ].start()
			server.join()
		} catch (Exception exception) {
			log.warn('Shutting down due to exception', exception)
			System.exit(1)
		}
	}
	
}
