package de.cau.cs.kieler.elkgraph.web

import com.google.inject.Inject
import com.google.inject.Provider
import io.typefox.sprotty.api.ActionMessage
import io.typefox.sprotty.api.IDiagramServer
import java.util.List
import java.util.Map
import org.apache.log4j.Logger
import org.eclipse.elk.graph.ElkNode
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.lsp4j.jsonrpc.Endpoint
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.ide.server.ILanguageServerAccess
import org.eclipse.xtext.ide.server.ILanguageServerAccess.IBuildListener
import org.eclipse.xtext.ide.server.ILanguageServerExtension
import org.eclipse.xtext.resource.IResourceDescription.Delta
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.validation.CheckMode
import org.eclipse.xtext.validation.IResourceValidator

class DiagramLanguageServerImpl implements DiagramEndpoint, ILanguageServerExtension, IDiagramServer.Provider, IBuildListener {
	
	static val LOG = Logger.getLogger(DiagramLanguageServerImpl)
	
	@Inject extension IResourceValidator

	@Inject ElkGraphDiagramGenerator diagramGenerator
	
	@Inject Provider<ElkGraphDiagramServer> diagramServerProvider
	
	val Map<String, IDiagramServer> diagramServers = newLinkedHashMap

	DiagramEndpoint _client

	extension ILanguageServerAccess languageServerAccess
	
	override initialize(ILanguageServerAccess access) {
		this.languageServerAccess = access
		access.addBuildListener(this)
	}

	protected def DiagramEndpoint getClient() {
		if (_client === null) {
			val client = languageServerAccess.languageClient
			if (client instanceof Endpoint) {
				_client = ServiceEndpoints.toServiceObject(client, DiagramEndpoint)
			}
		}
		return _client
	}
	
	override getDiagramServer(String clientId) {
		synchronized (diagramServers) {
			var server = diagramServers.get(clientId)
			if (server === null) {
				server = diagramServerProvider.get => [
					diagramLanguageServer = this
				]
				server.clientId = clientId
				server.remoteEndpoint = [ message |
					client?.accept(message)
				]
				diagramServers.put(clientId, server)
			}
			return server
		}
	}

	@JsonNotification
	override void accept(ActionMessage message) {
		val server = getDiagramServer(message.clientId)
		server.accept(message)
	}
	
	def void initializeModel(IDiagramServer diagramServer) {
		diagramServer.clientId.doRead [ context |
			val graph = context.resource.contents.head
			if (graph instanceof ElkNode && !context.cancelChecker.canceled) {
				return diagramGenerator.generateDiagram(graph as ElkNode)
			}
		].thenAccept[ newRoot |
			if (newRoot !== null)
				diagramServer.setModel(newRoot)
		]
	}

	override afterBuild(List<Delta> deltas) {
		for (uri : deltas.map[uri.toString]) {
			uri.doRead [ context |
				val resource = context.resource
				if (!resource.hasErrors(context.cancelChecker)) {
					val graph = resource.contents.head
					if (graph instanceof ElkNode) {
						val newRoot = diagramGenerator.generateDiagram(graph as ElkNode)
						return resource.URI.toString -> newRoot
					}
				}
			].thenAccept[ result |
				if (result !== null) {
					getDiagramServer(result.key).updateModel(result.value)
				}
			].exceptionally[ throwable |
				LOG.error('Error while processing build results', throwable)
				return null
			]
		}
	}
	
	protected def boolean hasErrors(Resource resource, CancelIndicator cancelIndicator) {
		resource.validate(CheckMode.NORMAL_AND_FAST, cancelIndicator).exists[
			severity === Severity.ERROR
		]
	} 

}
