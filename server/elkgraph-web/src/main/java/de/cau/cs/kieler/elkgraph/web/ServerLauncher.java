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
import org.apache.commons.cli.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Slf4jLog;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.eclipse.xtext.ide.server.ILanguageServerShutdownAndExitHandler;
import org.eclipse.xtext.ide.server.ServerModule;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.util.Modules2;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Main class for launching the ELK Graph server.
 */
public class ServerLauncher {
  public enum Mode {
    CHECK,

    SIGTERM,

    USER;
  }

  public static void main(final String[] args) {
    final ElkGraphLanguageServerSetup setup = new ElkGraphLanguageServerSetup();
    setup.setupLanguages();
    Options options = new Options();
    options.addOption("r", "root", true, "Root path of the server\'s content.");
    options.addOption("m", "mode", true, "Mode to start in (" + Mode.values().toString().join(", ") + ").");

    try {
      final CommandLine parsedOptions = new DefaultParser().parse(options, args);
      final String rootPath = parsedOptions.getOptionValue("root", "../..");
      final ServerLauncher.Mode mode = ServerLauncher.Mode.valueOf(parsedOptions.getOptionValue("mode", ServerLauncher.Mode.USER.toString()));
      final ServerLauncher launcher = new ServerLauncher(rootPath, mode, setup);
      launcher.start();
    } catch (ParseException e) {
      new HelpFormatter().printHelp(" ", options);
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
    ServerModule serverModule = new ServerModule();
    return Guice.createInjector(
      Modules2.mixin(serverModule,
              (Binder it) -> it.bind(Endpoint.class).to(LanguageServerEndpoint.class),
              (Binder it) -> it.bind(IResourceServiceProvider.Registry.class).toProvider(IResourceServiceProvider.Registry.RegistryProvider.class),
              (Binder it) -> it.bind(ILanguageServerShutdownAndExitHandler.class).to(ILanguageServerShutdownAndExitHandler.NullImpl.class)));
  }

  public void start() {
    final Slf4jLog log = new Slf4jLog(ServerLauncher.class.getName());
    try {
      final Server server = new Server(new InetSocketAddress(8080));
      // Configure web app context.
      final WebAppContext webAppContext = new WebAppContext();
      webAppContext.setResourceBase((this.rootPath + "/client/app"));
      log.info("Serving client app from " + webAppContext.getResourceBase());
      webAppContext.setWelcomeFiles(new String[]{"index.html"});
      webAppContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
      webAppContext.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");

      server.setHandler(webAppContext);
      final ServerContainer container = WebSocketServerContainerInitializer.configureContext(webAppContext);

      // Configure web socket to provide access to a diagram server for elkt
      final ServerEndpointConfig.Builder diagramServerEndpointConfigBuilder =
              ServerEndpointConfig.Builder.create(LanguageServerEndpoint.class, "/elkgraph");
      diagramServerEndpointConfigBuilder.configurator(new ServerEndpointConfig.Configurator() {
        @Override
        public <T extends Object> T getEndpointInstance(final Class<T> endpointClass) {
          return (T) ServerLauncher.this.createDiagramServerInjector().getInstance(Endpoint.class);
        }
      });
      container.addEndpoint(diagramServerEndpointConfigBuilder.build());

      // Configure a second web socket to provide a plain language server for any known xtext language
      // Two remarks:
      //  1) It is probably possible to re-use the diagram server above. However, to avoid any unexpected behavior,
      //     we separate it. (For instance, during initial tries it looked like a 'ElkGraphDiagramModule' must be added
      //     to the injector even for elkj in order to have a functioning content assist.)
      //  2) The default behavior to load xtext languages using service loaders, i.e. re-binding
      //     'IResourceServiceProvider.Registry' causes the diagram server to fail as soon as the language
      //     server has been used once. It looks like the automated registration is somehow interfering with the
      //     existing one (I presume because it re-registers the elkt language without the diagram server parts).
      final ServerEndpointConfig.Builder languageServerEndpointConfigBuilder =
              ServerEndpointConfig.Builder.create(LanguageServerEndpoint.class, "/elkgraphjson");
      languageServerEndpointConfigBuilder.configurator(new ServerEndpointConfig.Configurator() {
        @Override
        public <T> T getEndpointInstance(final Class<T> endpointClass) {
          return (T) ServerLauncher.this.createLanguageServerInjector().getInstance(Endpoint.class);
        }
      });
      container.addEndpoint(languageServerEndpointConfigBuilder.build());

      // Define endpoint for format conversions
      webAppContext.addServlet(new ServletHolder(new HttpServlet() {
        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
          ElkGraphConversions.handleRequest(req, resp);
        }
      }), "/conversion");
      try {
        server.start();
        final ServerLauncher.Mode mode = this.mode;
        if (mode != null) {
          switch (mode) {
            case CHECK -> {
              log.info("CHECK mode: terminating immediately.");
              server.stop();
            }
            case SIGTERM -> Runtime.getRuntime().addShutdownHook(new Thread(() -> {
              try {
                server.stop();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
              log.info("Server stopped.");
            }, "ShutdownHook"));
            case USER -> {
              log.info("Press enter to stop the server...");
              new Thread(() -> {
                try {
                  final int key = System.in.read();
                  server.stop();
                  if ((key == (-1))) {
                    log.warn("The standard input stream is empty");
                  }
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              }).start();
            }
            default -> {
            }
          }
        }
        server.join();
      } catch (Exception exception) {
        log.warn("Shutting down due to exception", exception);
        System.exit(1);
      }
    } catch (Exception e) {
      log.warn("Setup failed", e);
      System.exit(1);
    }
  }

  public ServerLauncher(final String rootPath, final ServerLauncher.Mode mode, final ElkGraphLanguageServerSetup setup) {
    super();
    this.rootPath = rootPath;
    this.mode = mode;
    this.setup = setup;
  }
}
