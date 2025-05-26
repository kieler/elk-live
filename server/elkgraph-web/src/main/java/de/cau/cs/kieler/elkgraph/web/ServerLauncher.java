/**
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
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
import java.io.IOException;
import java.net.InetSocketAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor;
import org.eclipse.xtext.ide.server.ILanguageServerShutdownAndExitHandler;
import org.eclipse.xtext.ide.server.ServerModule;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.util.Modules2;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

/**
 * Main class for launching the ELK Graph server.
 */
@FinalFieldsConstructor
@SuppressWarnings("all")
public class ServerLauncher {
  public enum Mode {
    CHECK,

    SIGTERM,

    USER;
  }

  public static void main(final String[] args) {
    final ElkGraphLanguageServerSetup setup = new ElkGraphLanguageServerSetup();
    setup.setupLanguages();
    Options _options = new Options();
    final Procedure1<Options> _function = (Options it) -> {
      it.addOption("r", "root", true, "Root path of the server\'s content.");
      String _join = IterableExtensions.join(((Iterable<?>)Conversions.doWrapArray(ServerLauncher.Mode.values())), ", ");
      String _plus = ("Mode to start in (" + _join);
      String _plus_1 = (_plus + ").");
      it.addOption("m", "mode", true, _plus_1);
    };
    final Options options = ObjectExtensions.<Options>operator_doubleArrow(_options, _function);
    try {
      final CommandLine parsedOptions = new DefaultParser().parse(options, args);
      final String rootPath = parsedOptions.getOptionValue("root", "../..");
      final ServerLauncher.Mode mode = ServerLauncher.Mode.valueOf(parsedOptions.getOptionValue("mode", ServerLauncher.Mode.USER.toString()));
      final ServerLauncher launcher = new ServerLauncher(rootPath, mode, setup);
      launcher.start();
    } catch (final Throwable _t) {
      if (_t instanceof ParseException) {
        new HelpFormatter().printHelp(" ", options);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }

  private final String rootPath;

  private final ServerLauncher.Mode mode;

  private final ElkGraphLanguageServerSetup setup;

  private Injector createDiagramServerInjector() {
    final Injector injector = Guice.createInjector(this.setup.getLanguageServerModule());
    this.setup.setupLanguageServer(injector);
    return injector;
  }

  private Injector createLanguageServerInjector() {
    ServerModule _serverModule = new ServerModule();
    final com.google.inject.Module _function = (Binder it) -> {
      it.<Endpoint>bind(Endpoint.class).to(LanguageServerEndpoint.class);
    };
    final com.google.inject.Module _function_1 = (Binder it) -> {
      it.<IResourceServiceProvider.Registry>bind(IResourceServiceProvider.Registry.class).toProvider(IResourceServiceProvider.Registry.RegistryProvider.class);
    };
    final com.google.inject.Module _function_2 = (Binder it) -> {
      it.<ILanguageServerShutdownAndExitHandler>bind(ILanguageServerShutdownAndExitHandler.class).to(ILanguageServerShutdownAndExitHandler.NullImpl.class);
    };
    return Guice.createInjector(
      Modules2.mixin(_serverModule, _function, _function_1, _function_2));
  }

  public void start() {
    try {
      String _name = ServerLauncher.class.getName();
      final Slf4jLog log = new Slf4jLog(_name);
      InetSocketAddress _inetSocketAddress = new InetSocketAddress(8080);
      final Server server = new Server(_inetSocketAddress);
      WebAppContext _webAppContext = new WebAppContext();
      final Procedure1<WebAppContext> _function = (WebAppContext it) -> {
        it.setResourceBase((this.rootPath + "/client/app"));
        String _resourceBase = it.getResourceBase();
        String _plus = ("Serving client app from " + _resourceBase);
        log.info(_plus);
        it.setWelcomeFiles(new String[] { "index.html" });
        it.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        it.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
      };
      final WebAppContext webAppContext = ObjectExtensions.<WebAppContext>operator_doubleArrow(_webAppContext, _function);
      server.setHandler(webAppContext);
      final ServerContainer container = WebSocketServerContainerInitializer.configureContext(webAppContext);
      final ServerEndpointConfig.Builder diagramServerEndpointConfigBuilder = ServerEndpointConfig.Builder.create(LanguageServerEndpoint.class, 
        "/elkgraph");
      diagramServerEndpointConfigBuilder.configurator(new ServerEndpointConfig.Configurator() {
        @Override
        public <T extends Object> T getEndpointInstance(final Class<T> endpointClass) throws InstantiationException {
          Endpoint _instance = ServerLauncher.this.createDiagramServerInjector().<Endpoint>getInstance(Endpoint.class);
          return ((T) _instance);
        }
      });
      container.addEndpoint(diagramServerEndpointConfigBuilder.build());
      final ServerEndpointConfig.Builder languageServerEndpointConfigBuilder = ServerEndpointConfig.Builder.create(LanguageServerEndpoint.class, 
        "/elkgraphjson");
      languageServerEndpointConfigBuilder.configurator(new ServerEndpointConfig.Configurator() {
        @Override
        public <T extends Object> T getEndpointInstance(final Class<T> endpointClass) throws InstantiationException {
          Endpoint _instance = ServerLauncher.this.createLanguageServerInjector().<Endpoint>getInstance(Endpoint.class);
          return ((T) _instance);
        }
      });
      container.addEndpoint(languageServerEndpointConfigBuilder.build());
      ServletHolder _servletHolder = new ServletHolder(new HttpServlet() {
        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
          ElkGraphConversions.handleRequest(req, resp);
        }
      });
      webAppContext.addServlet(_servletHolder, "/conversion");
      try {
        server.start();
        final ServerLauncher.Mode mode = this.mode;
        if (mode != null) {
          switch (mode) {
            case CHECK:
              log.info("CHECK mode: terminating immediately.");
              server.stop();
              break;
            case SIGTERM:
              Runtime _runtime = Runtime.getRuntime();
              final Runnable _function_1 = () -> {
                try {
                  server.stop();
                  log.info("Server stopped.");
                } catch (Throwable _e) {
                  throw Exceptions.sneakyThrow(_e);
                }
              };
              Thread _thread = new Thread(_function_1, "ShutdownHook");
              _runtime.addShutdownHook(_thread);
              break;
            case USER:
              log.info("Press enter to stop the server...");
              final Runnable _function_2 = () -> {
                try {
                  final int key = System.in.read();
                  server.stop();
                  if ((key == (-1))) {
                    log.warn("The standard input stream is empty");
                  }
                } catch (Throwable _e) {
                  throw Exceptions.sneakyThrow(_e);
                }
              };
              new Thread(_function_2).start();
              break;
            default:
              break;
          }
        }
        server.join();
      } catch (final Throwable _t) {
        if (_t instanceof Exception) {
          final Exception exception = (Exception)_t;
          log.warn("Shutting down due to exception", exception);
          System.exit(1);
        } else {
          throw Exceptions.sneakyThrow(_t);
        }
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  public ServerLauncher(final String rootPath, final ServerLauncher.Mode mode, final ElkGraphLanguageServerSetup setup) {
    super();
    this.rootPath = rootPath;
    this.mode = mode;
    this.setup = setup;
  }
}
