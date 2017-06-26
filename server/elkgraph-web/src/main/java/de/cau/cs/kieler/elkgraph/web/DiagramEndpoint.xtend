package de.cau.cs.kieler.elkgraph.web

import io.typefox.sprotty.api.ActionMessage
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment
import java.util.function.Consumer

@JsonSegment('diagram')
interface DiagramEndpoint extends Consumer<ActionMessage> {

	@JsonNotification
	override accept(ActionMessage actionMessage);

}
