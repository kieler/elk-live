/**
 * Copyright (c) 2020 Kiel University and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package de.cau.cs.kieler.elkgraph.web;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Pair;

@SuppressWarnings("all")
public class ElkLayoutVersionWrapper {
  private static final Logger LOG = Logger.getLogger(ElkLayoutVersionWrapper.class.getName());

  private final Method LAYOUT_METHOD;

  private final Object LAYOUTER_INSTANCE;

  public ElkLayoutVersionWrapper(final String jarPath) {
    Pair<Method, Object> _xtrycatchfinallyexpression = null;
    try {
      Pair<Method, Object> _xblockexpression = null;
      {
        URL _uRL = new File(jarPath).toURI().toURL();
        final URLClassLoader classLoader = new URLClassLoader(new URL[] { _uRL }, null);
        final Class<?> clazz = classLoader.loadClass("de.cau.cs.kieler.elkgraph.web.version.ElkLayoutVersion");
        Method _method = clazz.getMethod("layout", String.class);
        Object _newInstance = clazz.newInstance();
        _xblockexpression = Pair.<Method, Object>of(_method, _newInstance);
      }
      _xtrycatchfinallyexpression = _xblockexpression;
    } catch (final Throwable _t) {
      if (_t instanceof Throwable) {
        final Throwable t = (Throwable)_t;
        Pair<Method, Object> _xblockexpression_1 = null;
        {
          ElkLayoutVersionWrapper.LOG.log(Level.WARNING, (("Failed to instantiate layout wrapper for " + jarPath) + "."), t);
          _xblockexpression_1 = Pair.<Method, Object>of(null, null);
        }
        _xtrycatchfinallyexpression = _xblockexpression_1;
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
    final Pair<Method, Object> methodAndInstance = _xtrycatchfinallyexpression;
    this.LAYOUT_METHOD = methodAndInstance.getKey();
    this.LAYOUTER_INSTANCE = methodAndInstance.getValue();
  }

  public Optional<ElkNode> layout(final ElkNode graph) {
    if (((this.LAYOUTER_INSTANCE == null) || (this.LAYOUT_METHOD == null))) {
      return Optional.<ElkNode>empty();
    }
    final Function<String, String> _function = (String it) -> {
      try {
        String _xtrycatchfinallyexpression = null;
        try {
          Object _invoke = this.LAYOUT_METHOD.invoke(this.LAYOUTER_INSTANCE, it);
          _xtrycatchfinallyexpression = ((String) _invoke);
        } catch (final Throwable _t) {
          if (_t instanceof InvocationTargetException) {
            final InvocationTargetException ite = (InvocationTargetException)_t;
            Object _xblockexpression = null;
            {
              ElkLayoutVersionWrapper.LOG.log(Level.WARNING, "", ite);
              _xblockexpression = null;
            }
            _xtrycatchfinallyexpression = ((String)_xblockexpression);
          } else {
            throw Exceptions.sneakyThrow(_t);
          }
        }
        return _xtrycatchfinallyexpression;
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    final Function<String, ElkNode> _function_1 = (String it) -> {
      return this.deserialize(it).orElse(null);
    };
    return this.serialize(graph).<String>map(_function).<ElkNode>map(_function_1);
  }

  protected Optional<String> serialize(final ElkNode laidOutGraph) {
    final ResourceSetImpl rs = new ResourceSetImpl();
    final Resource xmiR = rs.createResource(URI.createURI("dummy.elkg"));
    EList<EObject> _contents = xmiR.getContents();
    _contents.add(laidOutGraph);
    final StringWriter sw = new StringWriter();
    final URIConverter.WriteableOutputStream os = new URIConverter.WriteableOutputStream(sw, "UTF-8");
    try {
      xmiR.save(os, null);
      return Optional.<String>of(sw.toString());
    } catch (final Throwable _t) {
      if (_t instanceof IOException) {
        final IOException e = (IOException)_t;
        ElkLayoutVersionWrapper.LOG.log(Level.WARNING, "elkg serialization failed (for layout wrapper).", e);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
    return Optional.<String>empty();
  }

  protected Optional<ElkNode> deserialize(final String serializedGraph) {
    final ResourceSetImpl rs = new ResourceSetImpl();
    final Resource r = rs.createResource(URI.createURI("dummy.elkg"));
    try {
      URIConverter.ReadableInputStream _readableInputStream = new URIConverter.ReadableInputStream(serializedGraph);
      r.load(_readableInputStream, null);
      boolean _isEmpty = r.getContents().isEmpty();
      boolean _not = (!_isEmpty);
      if (_not) {
        EObject _head = IterableExtensions.<EObject>head(r.getContents());
        return Optional.<ElkNode>of(((ElkNode) _head));
      }
    } catch (final Throwable _t) {
      if (_t instanceof IOException) {
        final IOException e = (IOException)_t;
        ElkLayoutVersionWrapper.LOG.log(Level.WARNING, "elkg deserialization failed (for layout wrapper).", e);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
    return Optional.<ElkNode>empty();
  }
}
