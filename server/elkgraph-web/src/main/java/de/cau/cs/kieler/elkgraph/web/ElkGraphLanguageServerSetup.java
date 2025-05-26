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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.websocket.Endpoint;
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
import org.eclipse.xtext.ide.server.ILanguageServerAccess;
import org.eclipse.xtext.ide.server.ILanguageServerShutdownAndExitHandler;
import org.eclipse.xtext.ide.server.ServerModule;
import org.eclipse.xtext.ide.server.UriExtensions;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.util.Modules2;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

/**
 * Configuration of global settings, language server Guice module, and language server setup.
 */
@SuppressWarnings("all")
public class ElkGraphLanguageServerSetup extends DiagramLanguageServerSetup {
  /**
   * Initialize global settings for ELK and the ELK Graph language.
   */
  @Override
  public void setupLanguages() {
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
    new ElkGraphJsonIdeSetup().createInjectorAndDoEMFRegistration();
    Map<String, Object> _extensionToFactoryMap = Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap();
    ElkGraphResourceFactory _elkGraphResourceFactory = new ElkGraphResourceFactory();
    _extensionToFactoryMap.put("elkg", _elkGraphResourceFactory);
    ElkGraphPackage.eINSTANCE.eClass();
    LayoutMetaDataService.getInstance();
  }

  /**
   * Create a Guice module for the language server. The language server uses a Guice injector that is separate
   * from the injectors of each Xtext language.
   */
  @Override
  public com.google.inject.Module getLanguageServerModule() {
    ServerModule _serverModule = new ServerModule();
    DiagramServerModule _diagramServerModule = new DiagramServerModule();
    final com.google.inject.Module _function = (Binder it) -> {
      it.<Endpoint>bind(Endpoint.class).to(LanguageServerEndpoint.class);
    };
    final com.google.inject.Module _function_1 = (Binder it) -> {
      it.<ILanguageServerShutdownAndExitHandler>bind(ILanguageServerShutdownAndExitHandler.class).to(ILanguageServerShutdownAndExitHandler.NullImpl.class);
    };
    final com.google.inject.Module _function_2 = (Binder it) -> {
      it.<IResourceServiceProvider.Registry>bind(IResourceServiceProvider.Registry.class).toProvider(IResourceServiceProvider.Registry.RegistryProvider.class);
    };
    return Modules2.mixin(_serverModule, _diagramServerModule, _function, _function_1, _function_2);
  }

  /**
   * Set up the language server in the given injector. A build listener is added that creates a
   * {@link RequestModelAction} for each new document that is opened.
   */
  public void setupLanguageServer(final Injector injector) {
    final LanguageServer languageServer = injector.<LanguageServer>getInstance(LanguageServer.class);
    if ((languageServer instanceof DiagramLanguageServer)) {
      @Extension
      final UriExtensions uriExtensions = injector.<UriExtensions>getInstance(UriExtensions.class);
      final ILanguageServerAccess.IBuildListener _function = (List<IResourceDescription.Delta> deltas) -> {
        final Function1<IResourceDescription.Delta, Boolean> _function_1 = (IResourceDescription.Delta it) -> {
          IResourceDescription _old = it.getOld();
          return Boolean.valueOf((_old == null));
        };
        final Consumer<IResourceDescription.Delta> _function_2 = (IResourceDescription.Delta delta) -> {
          ActionMessage _actionMessage = new ActionMessage();
          final Procedure1<ActionMessage> _function_3 = (ActionMessage it) -> {
            it.setClientId("sprotty");
            RequestModelAction _requestModelAction = new RequestModelAction();
            final Procedure1<RequestModelAction> _function_4 = (RequestModelAction it_1) -> {
              it_1.setDiagramType("graph");
              String _uriString = uriExtensions.toUriString(delta.getUri());
              Pair<String, String> _mappedTo = Pair.<String, String>of(DiagramOptions.OPTION_SOURCE_URI, _uriString);
              it_1.setOptions(Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(_mappedTo)));
            };
            RequestModelAction _doubleArrow = ObjectExtensions.<RequestModelAction>operator_doubleArrow(_requestModelAction, _function_4);
            it.setAction(_doubleArrow);
          };
          ActionMessage _doubleArrow = ObjectExtensions.<ActionMessage>operator_doubleArrow(_actionMessage, _function_3);
          ((DiagramLanguageServer)languageServer).accept(_doubleArrow);
        };
        IterableExtensions.<IResourceDescription.Delta>filter(deltas, _function_1).forEach(_function_2);
      };
      ((DiagramLanguageServer)languageServer).getLanguageServerAccess().addBuildListener(_function);
    }
  }
}
