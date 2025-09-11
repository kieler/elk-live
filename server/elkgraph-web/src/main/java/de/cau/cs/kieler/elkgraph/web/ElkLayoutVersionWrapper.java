/**
 * Copyright (c) 2025 Kiel University and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package de.cau.cs.kieler.elkgraph.web;

import org.eclipse.elk.graph.ElkNode;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElkLayoutVersionWrapper {
  private static final Logger LOG = Logger.getLogger(ElkLayoutVersionWrapper.class.getName());

  private final Method LAYOUT_METHOD;

  private final Object LAYOUTER_INSTANCE;

  public ElkLayoutVersionWrapper(final String jarPath) {
    Method layoutMethod = null;
    Object layouterInstance = null;
    try {
      // Note that the parent class loader is explicitly set to 'null'
      final URLClassLoader classLoader = new URLClassLoader(new URL[] { new File(jarPath).toURI().toURL() }, null);
      final Class<?> clazz = classLoader.loadClass("de.cau.cs.kieler.elkgraph.web.version.ElkLayoutVersion");
      layoutMethod = clazz.getMethod("layout", String.class);
      layouterInstance = clazz.getDeclaredConstructor().newInstance();
    } catch (final Throwable t) {
      ElkLayoutVersionWrapper.LOG.log(Level.WARNING, (("Failed to instantiate layout wrapper for " + jarPath) + "."), t);
      layoutMethod = null;
      layouterInstance = null;
    }
    this.LAYOUT_METHOD = layoutMethod;
    this.LAYOUTER_INSTANCE = layouterInstance;
  }

  public Optional<ElkNode> layout(final ElkNode graph) {
    if ((this.LAYOUTER_INSTANCE == null) || (this.LAYOUT_METHOD == null)) {
      return Optional.<ElkNode>empty();
    }
    return this.serialize(graph).map((it) -> {
      try {
        return (String) this.LAYOUT_METHOD.invoke(this.LAYOUTER_INSTANCE, it);
      } catch (InvocationTargetException | IllegalAccessException e) {
        // TODO pass to caller
        LOG.log(Level.WARNING, "", e);
        return null; // translates into Optional.empty
      }
    }).flatMap(this::deserialize);
  }

  protected Optional<String> serialize(final ElkNode laidOutGraph) {
    // We need to put the graph into a new (xmi) resource since
    // it currently resides in an xtext resource which wouldn't
    // yield the serialized output we desire.
    final ResourceSetImpl rs = new ResourceSetImpl();
    final Resource xmiR = rs.createResource(URI.createURI("dummy.elkg"));
    xmiR.getContents().add(laidOutGraph);
    final StringWriter sw = new StringWriter();
    final URIConverter.WriteableOutputStream os = new URIConverter.WriteableOutputStream(sw, "UTF-8");
    try {
      xmiR.save(os, null);
      return Optional.of(sw.toString());
    } catch (IOException e) {
      ElkLayoutVersionWrapper.LOG.log(Level.WARNING, "elkg serialization failed (for layout wrapper).", e);
    }
    return Optional.empty();
  }

  protected Optional<ElkNode> deserialize(final String serializedGraph) {
    final ResourceSetImpl rs = new ResourceSetImpl();
    final Resource r = rs.createResource(URI.createURI("dummy.elkg"));
    try {
      r.load(new URIConverter.ReadableInputStream(serializedGraph), null);
      if (!r.getContents().isEmpty()) {
        return Optional.of(((ElkNode) r.getContents().get(0)));
      }
    } catch (IOException e) {
      ElkLayoutVersionWrapper.LOG.log(Level.WARNING, "elkg deserialization failed (for layout wrapper).", e);
    }
    return Optional.empty();
  }
}
