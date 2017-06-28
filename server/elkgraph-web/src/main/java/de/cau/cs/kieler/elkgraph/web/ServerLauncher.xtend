package de.cau.cs.kieler.elkgraph.web

import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Provider
import de.cau.cs.kieler.elkgraph.web.xtext.InMemoryServerModule
import java.net.InetSocketAddress
import javax.websocket.server.ServerEndpointConfig
import org.eclipse.elk.alg.layered.options.LayeredOptions
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.graph.ElkGraphPackage
import org.eclipse.elk.graph.text.ElkGraphRuntimeModule
import org.eclipse.elk.graph.text.ide.ElkGraphIdeModule
import org.eclipse.elk.graph.text.ide.ElkGraphIdeSetup
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.log.Slf4jLog
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer
import org.eclipse.xtext.ide.server.ServerModule
import org.eclipse.xtext.util.Modules2

class ServerLauncher {
	
	def static void main(String[] args) {
		val launcher = Guice.createInjector(Modules2.mixin(new ServerModule, new InMemoryServerModule)).getInstance(ServerLauncher)
		launcher.initialize()
		launcher.start()
	}

	@Inject Provider<LanguageServerEndpoint> languageServerEndpointProvider
	
	def void initialize() {
		// Initialize ELK meta data
		LayoutMetaDataService.instance.registerLayoutMetaDataProviders(
			new CoreOptions,
			new LayeredOptions
		)
		
		// Initialize the ELK Graph Xtext language
		ElkGraphPackage.eINSTANCE.getNsURI
		new ElkGraphIdeSetup {
			override createInjector() {
				Guice.createInjector(Modules2.mixin(new ElkGraphRuntimeModule, new ElkGraphIdeModule, new ElkGraphDiagramModule))
			}
		}.createInjectorAndDoEMFRegistration()
	}
	
	def void start() {
		// Set up Jetty server
		val server = new Server(new InetSocketAddress(8080))
		val webAppContext = new WebAppContext => [
			resourceBase = '../../client/app'
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
				if (endpointClass.isAssignableFrom(LanguageServerEndpoint))
					languageServerEndpointProvider.get as T
				else
					super.getEndpointInstance(endpointClass)
			}
		})
		container.addEndpoint(endpointConfigBuilder.build())
		
		// Start the server
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