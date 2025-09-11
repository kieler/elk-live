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

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.util.persistence.ElkGraphResourceFactory;
import org.eclipse.elk.graph.ElkGraphPackage;
import org.eclipse.elk.graph.json.text.ide.ElkGraphJsonIdeSetup;
import org.eclipse.elk.graph.text.ElkGraphRuntimeModule;
import org.eclipse.elk.graph.text.ide.ElkGraphIdeModule;
import org.eclipse.elk.graph.text.ide.ElkGraphIdeSetup;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.sprotty.ActionMessage;
import org.eclipse.sprotty.DiagramOptions;
import org.eclipse.sprotty.RequestModelAction;
import org.eclipse.sprotty.xtext.launch.DiagramLanguageServerSetup;
import org.eclipse.sprotty.xtext.ls.DiagramLanguageServer;
import org.eclipse.sprotty.xtext.ls.DiagramServerModule;
import org.eclipse.xtext.ide.server.ILanguageServerShutdownAndExitHandler;
import org.eclipse.xtext.ide.server.ServerModule;
import org.eclipse.xtext.ide.server.UriExtensions;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.util.Modules2;
import org.eclipse.xtext.xbase.lib.*;

import javax.websocket.Endpoint;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Configuration of global settings, language server Guice module, and language server setup.
 */
public class ElkGraphLanguageServerSetup extends DiagramLanguageServerSetup {
  /**
   * Initialize global settings for ELK and the ELK Graph language.
   */
  @Override
  public void setupLanguages() {
    // Initialize the ELK Graph Xtext language
    ElkGraphPackage.eINSTANCE.getNsURI();
    new ElkGraphIdeSetup() {
      @Override
      public Injector createInjector() {
        ElkGraphRuntimeModule _elkGraphRuntimeModule = new ElkGraphRuntimeModule();
        ElkGraphIdeModule _elkGraphIdeModule = new ElkGraphIdeModule();
        ElkGraphDiagramModule _elkGraphDiagramModule = new ElkGraphDiagramModule();
        return Guice.createInjector(Modules2.mixin(_elkGraphRuntimeModule, _elkGraphIdeModule, _elkGraphDiagramModule));
      }
    }.createInjectorAndDoEMFRegistration();

    // Do _not_ use 'doSetup()' here as that wouldn't include the 'ElkGraphJsonIdeModule'
    // and thus wouldn't bind the proposal provider
    new ElkGraphJsonIdeSetup().createInjectorAndDoEMFRegistration();

    // Initialize ELKG Graph XMI format
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("elkg", new ElkGraphResourceFactory());
    ElkGraphPackage.eINSTANCE.eClass();

    // Make sure the meta data service has been constructed and ElkReflect has been registered
    LayoutMetaDataService.getInstance();
  }

  /**
   * Create a Guice module for the language server. The language server uses a Guice injector that is separate
   * from the injectors of each Xtext language.
   */
  @Override
  public com.google.inject.Module getLanguageServerModule() {
    return Modules2.mixin(new ServerModule(), new DiagramServerModule(),
            (Binder it) -> it.bind(Endpoint.class).to(LanguageServerEndpoint.class),
            (Binder it) -> it.bind(ILanguageServerShutdownAndExitHandler.class).to(ILanguageServerShutdownAndExitHandler.NullImpl.class),
            (Binder it) -> it.bind(IResourceServiceProvider.Registry.class).toProvider(IResourceServiceProvider.Registry.RegistryProvider.class)
    );
  }

  /**
   * Set up the language server in the given injector. A build listener is added that creates a
   * {@link RequestModelAction} for each new document that is opened.
   */
  public void setupLanguageServer(final Injector injector) {
    final LanguageServer languageServer = injector.getInstance(LanguageServer.class);
    if ((languageServer instanceof DiagramLanguageServer)) {
      @Extension
      final UriExtensions uriExtensions = injector.getInstance(UriExtensions.class);
      ((DiagramLanguageServer)languageServer).getLanguageServerAccess().addBuildListener((List<IResourceDescription.Delta> deltas) -> {
        for (IResourceDescription.Delta delta : deltas) {
          if (delta.getOld() == null) {
            ActionMessage actionMessage = new ActionMessage();
            actionMessage.setClientId("sprotty");
            RequestModelAction requestModelAction = new RequestModelAction();
            requestModelAction.setDiagramType("graph");
            HashMap<String, String> options = new HashMap<>();
            options.put(DiagramOptions.OPTION_SOURCE_URI, uriExtensions.toUriString(delta.getUri()));
            requestModelAction.setOptions(options);
            actionMessage.setAction(requestModelAction);
            ((DiagramLanguageServer)languageServer).accept(actionMessage);
          }
        }
      });
    }
  }
}
