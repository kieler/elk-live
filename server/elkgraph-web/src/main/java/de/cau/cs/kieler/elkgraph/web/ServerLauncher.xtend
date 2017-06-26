package de.cau.cs.kieler.elkgraph.web

import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Provider
import java.net.InetSocketAddress
import javax.websocket.server.ServerEndpointConfig
import org.eclipse.elk.alg.layered.options.LayeredOptions
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.graph.text.ElkGraphRuntimeModule
import org.eclipse.elk.graph.text.ElkGraphStandaloneSetup
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.log.Slf4jLog
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer

class ServerLauncher {
	
	def static void main(String[] args) {
		val launcher = Guice.createInjector(new ServerModule).getInstance(ServerLauncher)
		launcher.initialize()
		launcher.start()
	}

	@Inject Provider<LanguageServerEndpoint> languageServerEndpointProvider
	
	def void initialize() {
		LayoutMetaDataService.instance.registerLayoutMetaDataProviders(
			new CoreOptions,
			new LayeredOptions
		)
		new ElkGraphStandaloneSetup {
			override createInjector() {
				Guice.createInjector(new ElkGraphRuntimeModule, new ElkGraphDiagramModule)
			}
		}.createInjectorAndDoEMFRegistration()
	}
	
	def void start() {
		val server = new Server(new InetSocketAddress(8080))
		val webAppContext = new WebAppContext => [
			resourceBase = '../../client/app'
			welcomeFiles = #['index.html']
			setInitParameter('org.eclipse.jetty.servlet.Default.dirAllowed', 'false')
			setInitParameter('org.eclipse.jetty.servlet.Default.useFileMappedBuffer', 'false')
		]
		server.handler = webAppContext
		
		val container = WebSocketServerContainerInitializer.configureContext(webAppContext)
		val endpointConfigBuilder = ServerEndpointConfig.Builder.create(LanguageServerEndpoint, '/')
		endpointConfigBuilder.configurator(new ServerEndpointConfig.Configurator {
			override <T> getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
				if (endpointClass.isAssignableFrom(LanguageServerEndpoint))
					languageServerEndpointProvider.get as T
				else
					super.getEndpointInstance(endpointClass)
			}
		})
		container.addEndpoint(endpointConfigBuilder.build())
		
		val log = new Slf4jLog(ServerLauncher.name)
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