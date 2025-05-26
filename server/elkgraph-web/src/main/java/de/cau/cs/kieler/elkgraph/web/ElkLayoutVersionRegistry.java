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

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.eclipse.elk.core.util.persistence.ElkGraphResourceFactory;
import org.eclipse.elk.graph.ElkGraphPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.lib.StringExtensions;

@SuppressWarnings("all")
public final class ElkLayoutVersionRegistry {
  private static final Logger LOG = Logger.getLogger(ElkLayoutVersionRegistry.class.getName());

  @Accessors
  private static final Map<String, ElkLayoutVersionWrapper> versionToWrapper = new Function0<Map<String, ElkLayoutVersionWrapper>>() {
    @Override
    public Map<String, ElkLayoutVersionWrapper> apply() {
      Map<String, Object> _extensionToFactoryMap = Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap();
      ElkGraphResourceFactory _elkGraphResourceFactory = new ElkGraphResourceFactory();
      _extensionToFactoryMap.put("elkg", _elkGraphResourceFactory);
      ElkGraphPackage.eINSTANCE.eClass();
      final String elkJars = System.getProperty("elkJars");
      boolean _isNullOrEmpty = StringExtensions.isNullOrEmpty(elkJars);
      if (_isNullOrEmpty) {
        ElkLayoutVersionRegistry.LOG.severe("Make sure the system property \'elkJars\' is set properly.");
        return CollectionLiterals.<String, ElkLayoutVersionWrapper>newImmutableMap();
      }
      final String[] strings = System.getProperty("elkJars").split(",");
      final Function1<String, Pair<String, ElkLayoutVersionWrapper>> _function = (String jarPath) -> {
        final String filePath = Paths.get(jarPath).getFileName().toString();
        String _substring = filePath.substring(0, filePath.lastIndexOf("-"));
        ElkLayoutVersionWrapper _elkLayoutVersionWrapper = new ElkLayoutVersionWrapper(jarPath);
        return Pair.<String, ElkLayoutVersionWrapper>of(_substring, _elkLayoutVersionWrapper);
      };
      final Map<String, ElkLayoutVersionWrapper> ret = CollectionLiterals.<String, ElkLayoutVersionWrapper>newImmutableMap(((Pair<? extends String, ? extends ElkLayoutVersionWrapper>[])Conversions.unwrapArray(ListExtensions.<String, Pair<String, ElkLayoutVersionWrapper>>map(((List<String>)Conversions.doWrapArray(strings)), _function), Pair.class)));
      String _join = IterableExtensions.join(ret.keySet(), ", ");
      String _plus = ("ELK layout versions found: " + _join);
      ElkLayoutVersionRegistry.LOG.info(_plus);
      return ret;
    }
  }.apply();

  @Pure
  public static Map<String, ElkLayoutVersionWrapper> getVersionToWrapper() {
    return ElkLayoutVersionRegistry.versionToWrapper;
  }
}
