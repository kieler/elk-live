package de.cau.cs.kieler.elkgraph.web

import com.google.inject.Inject
import de.cau.cs.kieler.elkgraph.web.LanguageServerEndpoint.LanguageMessageHandler
import io.typefox.sprotty.server.json.ActionTypeAdapter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.LinkedHashMap
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.MessageHandler
import javax.websocket.Session
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethodProvider
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor

class LanguageServerEndpoint extends Endpoint {
	
	@Inject LanguageServer languageServer
	
	override onOpen(Session session, EndpointConfig config) {
		val supportedMethods = new LinkedHashMap<String, JsonRpcMethod>
		supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(LanguageClient))
		if (languageServer instanceof JsonRpcMethodProvider)
			supportedMethods.putAll(languageServer.supportedMethods)
		
		val jsonHandler = new MessageJsonHandler(supportedMethods) {
			override getDefaultGsonBuilder() {
				ActionTypeAdapter.configureGson(super.defaultGsonBuilder)
			}
		}
		val out = new ByteArrayOutputStream
		val outgoingMessageStream = new StreamMessageConsumer(out, jsonHandler) {
			override consume(Message message) {
				super.consume(message)
				session.asyncRemote.sendText(out.toString)
				out.reset()
			}
		}
		val serverEndpoint = new RemoteEndpoint(outgoingMessageStream, ServiceEndpoints.toEndpoint(languageServer))
		jsonHandler.setMethodProvider(serverEndpoint)
		val incomingMessageStream = new StreamMessageProducer(null, jsonHandler)
		session.addMessageHandler(new LanguageMessageHandler(incomingMessageStream, serverEndpoint))
		
		val remoteProxy = ServiceEndpoints.toServiceObject(serverEndpoint, LanguageClient)
		if (languageServer instanceof LanguageClientAware)
			languageServer.connect(remoteProxy)
	}
	
	@FinalFieldsConstructor
	protected static class LanguageMessageHandler implements MessageHandler.Whole<String> {
		val StreamMessageProducer messageProducer
		val RemoteEndpoint serverEndpoint
		
		override onMessage(String message) {
			messageProducer.input = new ByteArrayInputStream(message.bytes)
			messageProducer.listen(serverEndpoint)
		}
	}
	
}