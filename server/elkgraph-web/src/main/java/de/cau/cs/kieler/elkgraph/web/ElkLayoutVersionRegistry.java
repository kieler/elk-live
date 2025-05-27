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

import org.eclipse.elk.core.util.persistence.ElkGraphResourceFactory;
import org.eclipse.elk.graph.ElkGraphPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.xbase.lib.*;
import org.eclipse.xtext.xbase.lib.Functions.Function0;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class ElkLayoutVersionRegistry {
  private static final Logger LOG = Logger.getLogger(ElkLayoutVersionRegistry.class.getName());

  private static final Map<String, ElkLayoutVersionWrapper> versionToWrapper = new Function0<Map<String, ElkLayoutVersionWrapper>>() {
    @Override
    public Map<String, ElkLayoutVersionWrapper> apply() {
      // Make sure the elkg xmi format is properly registered
      Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("elkg", new ElkGraphResourceFactory());
      ElkGraphPackage.eINSTANCE.eClass();

      final String elkJars = System.getProperty("elkJars");
      if (elkJars == null || elkJars.isEmpty()) {
        ElkLayoutVersionRegistry.LOG.severe("Make sure the system property \'elkJars\' is set properly.");
        return CollectionLiterals.<String, ElkLayoutVersionWrapper>newImmutableMap();
      }
      // In here are strings in the form of taldfdhfdksfh/0.10.0-0.1.0
      final String[] strings = System.getProperty("elkJars").split(",");
      // Get the version, e.g. 0.10.0 or 0.9.0.
      final Map<String, ElkLayoutVersionWrapper> ret =
              CollectionLiterals.<String, ElkLayoutVersionWrapper>newImmutableMap(((Pair<? extends String, ? extends ElkLayoutVersionWrapper>[]) Conversions.unwrapArray(ListExtensions.<String, Pair<String, ElkLayoutVersionWrapper>>map(((List<String>)Conversions.doWrapArray(strings)),
                      (String jarPath) -> {
                final String filePath = Paths.get(jarPath).getFileName().toString();
                String substring = filePath.substring(0, filePath.lastIndexOf("-"));
                ElkLayoutVersionWrapper elkLayoutVersionWrapper = new ElkLayoutVersionWrapper(jarPath);
                return Pair.<String, ElkLayoutVersionWrapper>of(substring, elkLayoutVersionWrapper);
              }), Pair.class)));
      String _join = IterableExtensions.join(ret.keySet(), ", ");
      String _plus = ("ELK layout versions found: " + _join);
      ElkLayoutVersionRegistry.LOG.info(_plus);
      return ret;
    }
  }.apply();

  public static Map<String, ElkLayoutVersionWrapper> getVersionToWrapper() {
    return ElkLayoutVersionRegistry.versionToWrapper;
  }
}
