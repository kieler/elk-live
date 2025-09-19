/**
 * Copyright (c) 2025 Kiel University and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package de.cau.cs.kieler.elkgraph.web.version;

import org.eclipse.elk.core.IGraphLayoutEngine;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.data.ILayoutMetaDataProvider;
import org.eclipse.elk.core.data.LayoutAlgorithmData;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.core.util.persistence.ElkGraphResourceFactory;
import org.eclipse.elk.graph.ElkGraphPackage;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElkLayoutVersion implements IElkLayoutVersion {
    private static final Logger LOG = Logger.getLogger(ElkLayoutVersion.class.getName());
    private final IGraphLayoutEngine layoutEngine = new RecursiveGraphLayoutEngine();

    public ElkLayoutVersion() throws NoSuchMethodException, NoSuchFieldException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Map<String, Object> _extensionToFactoryMap = Registry.INSTANCE.getExtensionToFactoryMap();
        ElkGraphResourceFactory _elkGraphResourceFactory = new ElkGraphResourceFactory();
        _extensionToFactoryMap.put("elkg", _elkGraphResourceFactory);
        ElkGraphPackage.eINSTANCE.eClass();

        try {
            LayoutMetaDataService.getInstance();
        } catch (ServiceConfigurationError e) {
            Constructor<LayoutMetaDataService> ctor = LayoutMetaDataService.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Field instanceField = LayoutMetaDataService.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, ctor.newInstance());
            LayoutMetaDataService instance = LayoutMetaDataService.getInstance();
            CoreOptions coreOptions = new CoreOptions();
            instance.registerLayoutMetaDataProviders(coreOptions);
        }
        // If the first option is not layered, we have to do manual registration (I do not think that this is always true, but hey.
        final LayoutAlgorithmData[] findFirst = new LayoutAlgorithmData[1];
        LayoutMetaDataService.getInstance().getAlgorithmData().forEach(layoutAlgorithmData -> {
            if (layoutAlgorithmData.getId().contains("layered")) {
                findFirst[0] = layoutAlgorithmData;
            }
        });
        if (findFirst[0] == null) {
            LOG.info("Manually registering layout providers (required prior to 0.7.0).");
            Arrays.stream(new String[]{"org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider",
                    "org.eclipse.elk.alg.force.options.ForceMetaDataProvider",
                    "org.eclipse.elk.alg.force.options.StressMetaDataProvider",
                    "org.eclipse.elk.alg.mrtree.options.MrTreeMetaDataProvider",
                    "org.eclipse.elk.alg.radial.options.RadialMetaDataProvider",
                    "org.eclipse.elk.alg.common.compaction.options.PolyominoOptions",
                    "org.eclipse.elk.alg.disco.options.DisCoMetaDataProvider",
                    "org.eclipse.elk.alg.spore.options.SporeMetaDataProvider",
                    "org.eclipse.elk.alg.rectpacking.options.RectPackingMetaDataProvider",
                    "org.eclipse.elk.alg.vertiflex.options.VertiFlexMetaDataProvider"
            }).toList().forEach((providerClassName) -> {
                try {
                    Class<?> clazz = Class.forName(providerClassName);
                    ILayoutMetaDataProvider newInstance = (ILayoutMetaDataProvider) clazz.getDeclaredConstructor().newInstance();
                    LayoutMetaDataService.getInstance().registerLayoutMetaDataProviders(newInstance);
                } catch (Throwable e) {
                    LOG.info(providerClassName + " could not be registered (this is not necessarily an error)");
                }
            });
        }
    }

    public String layout(String serializedGraph) {
        return this.deserialize(serializedGraph).map(this::layout).flatMap(this::serialize).orElse(null);
    }

    protected ElkNode layout(ElkNode elkGraph) {
        this.layoutEngine.layout(elkGraph, new BasicProgressMonitor());
        return elkGraph;
    }

    protected Optional<ElkNode> deserialize(String serializedGraph) {
        ResourceSetImpl rs = new ResourceSetImpl();
        Resource r = rs.createResource(URI.createURI("dummy.elkg"));

        try {
            r.load(new URIConverter.ReadableInputStream(serializedGraph), null);
            if (!r.getContents().isEmpty()) {
                return Optional.of((ElkNode) IterableExtensions.head(r.getContents()));
            }
        } catch (Throwable var8) {
            if (!(var8 instanceof IOException e)) {
                throw Exceptions.sneakyThrow(var8);
            }

            LOG.log(Level.WARNING, "elkg deserialization failed (for concrete layout version).", e);
        }

        return Optional.empty();
    }

    protected Optional<String> serialize(ElkNode laidOutGraph) {
        Resource r = laidOutGraph.eResource();
        StringWriter sw = new StringWriter();
        URIConverter.WriteableOutputStream os = new URIConverter.WriteableOutputStream(sw, "UTF-8");

        try {
            r.save(os, null);
            return Optional.of(sw.toString());
        } catch (Throwable e) {
            if (e instanceof IOException) {
                LOG.log(Level.WARNING, "elkg serialization failed (for concrete layout version).", e);
                return Optional.empty();
            } else {
                throw Exceptions.sneakyThrow(e);
            }
        }
    }
}
