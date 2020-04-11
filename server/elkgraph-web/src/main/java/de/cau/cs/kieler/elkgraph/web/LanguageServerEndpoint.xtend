/*******************************************************************************
 * Copyright (c) 2019 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import com.google.inject.Inject
import de.cau.cs.kieler.elkgraph.web.ElkDiagramServer.ChangeLayoutVersionAction
import java.util.Collection
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.websocket.WebSocketEndpoint
import org.eclipse.sprotty.server.json.ActionTypeAdapter
import org.eclipse.sprotty.server.json.EnumTypeAdapter

/**
 * WebSocket endpoint that connects to the Xtext language server.
 */
class LanguageServerEndpoint extends WebSocketEndpoint<LanguageClient> {

    @Inject LanguageServer languageServer

    override protected configure(Launcher.Builder<LanguageClient> builder) {
        builder.localService = languageServer
        builder.remoteInterface = LanguageClient
        builder.configureGson [ gsonBuilder |
            // ActionTypeAdapter#configureGson() cannot be used here since we want to register our 
            // own ChangeLayoutVersionAction
            val defaultFactory = new ActionTypeAdapter.Factory()
            defaultFactory.addActionKind(ChangeLayoutVersionAction.KIND, ChangeLayoutVersionAction)
            // The following two lines represent the implementation of ActionTypeAdapter#configureGson() 
            gsonBuilder.registerTypeAdapterFactory(defaultFactory)
            gsonBuilder.registerTypeAdapterFactory(new EnumTypeAdapter.Factory());
        ]
    }

    override protected connect(Collection<Object> localServices, LanguageClient remoteProxy) {
        localServices.filter(LanguageClientAware).forEach[connect(remoteProxy)]
    }

}
