/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import com.google.inject.Guice
import java.io.IOException
import java.net.InetSocketAddress
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.websocket.Endpoint
import javax.websocket.server.ServerEndpointConfig
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.log.Slf4jLog
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import org.eclipse.xtext.ide.server.ILanguageServerShutdownAndExitHandler
import org.eclipse.xtext.ide.server.ServerModule
import org.eclipse.xtext.resource.IResourceServiceProvider
import org.eclipse.xtext.util.Modules2

/**
 * Main class for launching the ELK Graph server.
 */
@FinalFieldsConstructor
class ServerLauncher {
	
	enum Mode {
		CHECK,
		SIGTERM,
		USER
	}
	
	def static void main(String[] args) {
		val setup = new ElkGraphLanguageServerSetup
		setup.setupLanguages()

		val options = new Options() => [
			addOption("r", "root", true, "Root path of the server's content.")
			addOption("m", "mode", true, "Mode to start in (" + Mode.values.join(", ") + ").")
		]

		try {
			val parsedOptions = new DefaultParser().parse(options, args)

			val rootPath = parsedOptions.getOptionValue("root", "../..")
			val mode = Mode.valueOf(parsedOptions.getOptionValue("mode", Mode.USER.toString))
			val launcher = new ServerLauncher(rootPath, mode, setup)
			launcher.start()

		} catch (ParseException e) {
			new HelpFormatter().printHelp(" ", options);
		}
	}

	val String rootPath
	val Mode mode
	val ElkGraphLanguageServerSetup setup
	
	private def createDiagramServerInjector() {
		val injector = Guice.createInjector(setup.languageServerModule)
		setup.setupLanguageServer(injector)
		return injector
	}
	
	private def createLanguageServerInjector() {
		Guice.createInjector(Modules2.mixin(
			new ServerModule,
			[bind(Endpoint).to(LanguageServerEndpoint)], // not really nice to re-use this but hey ...
			[bind(IResourceServiceProvider.Registry).toProvider(IResourceServiceProvider.Registry.RegistryProvider)],
			[bind(ILanguageServerShutdownAndExitHandler).to(ILanguageServerShutdownAndExitHandler.NullImpl)]
		))
	}
	
	def void start() {
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
		
		val container = WebSocketServerContainerInitializer.configureContext(webAppContext)

		// Configure web socket to provide access to a diagram server for elkt
		val diagramServerEndpointConfigBuilder = ServerEndpointConfig.Builder.create(LanguageServerEndpoint,
			'/elkgraph')
		diagramServerEndpointConfigBuilder.configurator(new ServerEndpointConfig.Configurator {
			override <T> getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
				return createDiagramServerInjector.getInstance(Endpoint) as T
			}
		})
		container.addEndpoint(diagramServerEndpointConfigBuilder.build())

		// Configure a second web socket to provide a plain language server for any known xtext language
		// Two remarks: 
		//  1) It is probably possible to re-use the diagram server above. However, to avoid any unexpected behavior,
		//     we separate it. (For instance, during initial tries it looked like a 'ElkGraphDiagramModule' must be added 
		//     to the injector even for elkj in order to have a functioning content assist.)
		//  2) The default behavior to load xtext languages using service loaders, i.e. re-binding 
		//     'IResourceServiceProvider.Registry' causes the diagram server to fail as soon as the language
		//     server has been used once. It looks like the automated registration is somehow interfering with the 
		//     existing one (I presume because it re-registers the elkt language without the diagram server parts). 
		val languageServerEndpointConfigBuilder = ServerEndpointConfig.Builder.create(LanguageServerEndpoint,
			'/elkgraphjson')
		languageServerEndpointConfigBuilder.configurator(new ServerEndpointConfig.Configurator {
			override <T> getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
				return createLanguageServerInjector.getInstance(Endpoint) as T
			}
		})
		container.addEndpoint(languageServerEndpointConfigBuilder.build())

		// Define endpoint for format conversions
		webAppContext.addServlet(new ServletHolder(new HttpServlet {
			override protected doPost(HttpServletRequest req,
				HttpServletResponse resp) throws ServletException, IOException {
				ElkGraphConversions.handleRequest(req, resp)
			}
		}), "/conversion")

		// Start the server
		try {
			server.start()

			switch mode {
				case CHECK: {
					log.info("CHECK mode: terminating immediately.")
					server.stop()
				}
				case SIGTERM: {
					Runtime.runtime.addShutdownHook(new Thread([
						server.stop()
						log.info('Server stopped.')
					], "ShutdownHook"))
				}
				case USER: {
					log.info('Press enter to stop the server...')
					new Thread[
						val key = System.in.read()
						server.stop()
						if (key == -1)
							log.warn('The standard input stream is empty')
					].start()
				}
			}

			server.join()

		} catch (Exception exception) {
			log.warn('Shutting down due to exception', exception)
			System.exit(1)
		}
	}
	
}
