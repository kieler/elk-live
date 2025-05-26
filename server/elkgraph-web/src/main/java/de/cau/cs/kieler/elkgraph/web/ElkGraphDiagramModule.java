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

import org.eclipse.sprotty.IDiagramServer;
import org.eclipse.sprotty.xtext.DefaultDiagramModule;
import org.eclipse.sprotty.xtext.IDiagramGenerator;
import org.eclipse.sprotty.xtext.IDiagramServerFactory;

/**
 * Guice bindings for the ELK diagram server.
 */
@SuppressWarnings("all")
public class ElkGraphDiagramModule extends DefaultDiagramModule {
  public Class<? extends IDiagramGenerator> bindIDiagramGenerator() {
    return ElkGraphDiagramGenerator.class;
  }

  @Override
  public Class<? extends IDiagramServerFactory> bindIDiagramServerFactory() {
    return ElkGraphDiagramServerFactory.class;
  }

  @Override
  public Class<? extends IDiagramServer> bindIDiagramServer() {
    return ElkDiagramServer.class;
  }
}
