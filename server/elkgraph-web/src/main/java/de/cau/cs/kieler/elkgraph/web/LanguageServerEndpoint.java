/**
 * Copyright (c) 2019 TypeFox GmbH (http://www.typefox.io) and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package de.cau.cs.kieler.elkgraph.web;

import com.google.common.collect.Iterables;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import de.cau.cs.kieler.elkgraph.web.ElkDiagramServer;
import java.util.Collection;
import java.util.function.Consumer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.websocket.WebSocketEndpoint;
import org.eclipse.sprotty.server.json.ActionTypeAdapter;
import org.eclipse.sprotty.server.json.EnumTypeAdapter;

/**
 * WebSocket endpoint that connects to the Xtext language server.
 */
@SuppressWarnings("all")
public class LanguageServerEndpoint extends WebSocketEndpoint<LanguageClient> {
  @Inject
  private LanguageServer languageServer;

  @Override
  protected void configure(final Launcher.Builder<LanguageClient> builder) {
    builder.setLocalService(this.languageServer);
    builder.setRemoteInterface(LanguageClient.class);
    final Consumer<GsonBuilder> _function = (GsonBuilder gsonBuilder) -> {
      final ActionTypeAdapter.Factory defaultFactory = new ActionTypeAdapter.Factory();
      defaultFactory.addActionKind(ElkDiagramServer.ChangeLayoutVersionAction.KIND, ElkDiagramServer.ChangeLayoutVersionAction.class);
      gsonBuilder.registerTypeAdapterFactory(defaultFactory);
      EnumTypeAdapter.Factory _factory = new EnumTypeAdapter.Factory();
      gsonBuilder.registerTypeAdapterFactory(_factory);
    };
    builder.configureGson(_function);
  }

  @Override
  protected void connect(final Collection<Object> localServices, final LanguageClient remoteProxy) {
    final Consumer<LanguageClientAware> _function = (LanguageClientAware it) -> {
      it.connect(remoteProxy);
    };
    Iterables.<LanguageClientAware>filter(localServices, LanguageClientAware.class).forEach(_function);
  }
}
