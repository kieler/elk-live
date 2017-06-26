package de.cau.cs.kieler.elkgraph.web

import com.google.inject.Inject
import com.google.inject.Singleton
import io.typefox.sprotty.api.ActionMessage
import io.typefox.sprotty.api.DefaultDiagramServer
import io.typefox.sprotty.api.IDiagramServer
import io.typefox.sprotty.api.SModelRoot
import java.util.List
import java.util.Map
import org.eclipse.elk.graph.ElkNode
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.lsp4j.jsonrpc.Endpoint
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.ide.server.ILanguageServerAccess
import org.eclipse.xtext.ide.server.ILanguageServerAccess.IBuildListener
import org.eclipse.xtext.ide.server.ILanguageServerExtension
import org.eclipse.xtext.ide.server.UriExtensions
import org.eclipse.xtext.resource.IResourceDescription.Delta
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.validation.CheckMode
import org.eclipse.xtext.validation.IResourceValidator

@Singleton
class DiagramLanguageServerImpl implements DiagramEndpoint, ILanguageServerExtension, IDiagramServer.Provider, IBuildListener {
	
	@Inject extension IResourceValidator

	@Inject extension UriExtensions
	
	@Inject ElkGraphDiagramGenerator diagramGenerator
	
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
				server = new DefaultDiagramServer
				server.initializeModel(clientId)
				server.remoteEndpoint = [ message |
					client?.accept(message)
				]
				diagramServers.put(clientId, server)
			}
			return server
		}
	}
	
	protected def void initializeModel(IDiagramServer diagramServer, String clientId) {
		diagramServer.clientId = clientId
		clientId.doRead [ context |
			val graph = context.resource.contents.head
			if (graph instanceof ElkNode && !context.cancelChecker.canceled) {
				val modelMapping = diagramGenerator.generateDiagram(graph as ElkNode)
				return modelMapping.get(graph) as SModelRoot
			}
		].thenAccept[ newRoot |
			if (newRoot !== null)
				diagramServer.setModel(newRoot)
		]
	}

	@JsonNotification
	override void accept(ActionMessage message) {
		val server = getDiagramServer(message.clientId)
		server.accept(message)
	}

	override afterBuild(List<Delta> deltas) {
		for (uri : deltas.map[uri.toPath]) {
			uri.doRead [ context |
				val resource = context.resource
				if (!resource.hasErrors(context.cancelChecker)) {
					val graph = resource.contents.head
					if (graph instanceof ElkNode) {
						val modelMapping = diagramGenerator.generateDiagram(graph as ElkNode)
						return resource.URI.toString -> modelMapping.get(graph) as SModelRoot
					}
				}
			].thenAccept[ result |
				if (result !== null) {
					synchronized (diagramServers) {
						diagramServers.values.filter[ server |
							server.clientId == result.key
						].forEach[
							updateModel(result.value)
						]
					}
				}
			]
		}
	}
	
	protected def boolean hasErrors(Resource resource, CancelIndicator cancelIndicator) {
		resource.validate(CheckMode.NORMAL_AND_FAST, cancelIndicator).exists[
			severity === Severity.ERROR
		]
	} 

}
