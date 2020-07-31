/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web.version

import java.io.IOException
import java.io.StringWriter
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.util.Optional
import java.util.ServiceConfigurationError
import java.util.logging.Level
import java.util.logging.Logger
import org.eclipse.elk.core.IGraphLayoutEngine
import org.eclipse.elk.core.RecursiveGraphLayoutEngine
import org.eclipse.elk.core.data.ILayoutMetaDataProvider
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.core.util.BasicProgressMonitor
import org.eclipse.elk.core.util.persistence.ElkGraphResourceFactory
import org.eclipse.elk.graph.ElkGraphPackage
import org.eclipse.elk.graph.ElkNode
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.URIConverter
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl

/**
 * Transforms ELK graphs into sprotty models to be transferred to clients.
 */
class ElkLayoutVersion implements IElkLayoutVersion {

    static val LOG = Logger.getLogger(ElkLayoutVersion.name)

    val IGraphLayoutEngine layoutEngine = new RecursiveGraphLayoutEngine

    new() {
        // Register everything required to de-/serialize ELK graph's xmi representation
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("elkg", new ElkGraphResourceFactory());
        ElkGraphPackage.eINSTANCE.eClass();

        // Workaround for ELK #676 - service loaders and different class loaders           
        // Mimic the behavior of the 'getInstance()' method, without invoking service loading
        try {
        	LayoutMetaDataService.instance
        } catch (ServiceConfigurationError sce) {
        	val ctor = typeof(LayoutMetaDataService).getDeclaredConstructor()
        	ctor.accessible = true
        	val instanceField = typeof(LayoutMetaDataService).getDeclaredField("instance")
        	instanceField.accessible = true
        	instanceField.set(null, ctor.newInstance())
            
            // Important to register the core options here, the remaining providers 
            // will eventually be registered below
            LayoutMetaDataService.instance.registerLayoutMetaDataProviders(new CoreOptions());
        }
        
        // Starting with 0.7.0 a ServiceLoader is used, to automatically register all available layouters.
        // Prior to that it had to be done manually, which is why the following code is necessary.
        // Since some core algorithms are pre-registered, we check if 'layered' has been registered and if not, we 
        // assume none have been registered at all.
        val manualRegistrationRequired = LayoutMetaDataService.instance.algorithmData.findFirst [
            it.id.contains("layered")
        ] === null
        if (manualRegistrationRequired) {
            LOG.info("Manually registering layout providers (required prior to 0.7.0).")
            #[
                "org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider",
                "org.eclipse.elk.alg.force.options.ForceMetaDataProvider",
                "org.eclipse.elk.alg.force.options.StressMetaDataProvider",
                "org.eclipse.elk.alg.mrtree.options.MrTreeMetaDataProvider",
                "org.eclipse.elk.alg.radial.options.RadialMetaDataProvider",
                "org.eclipse.elk.alg.common.compaction.options.PolyominoOptions",
                "org.eclipse.elk.alg.disco.options.DisCoMetaDataProvider",
                "org.eclipse.elk.alg.spore.options.SporeMetaDataProvider", // since 0.4.0
                "org.eclipse.elk.alg.rectpacking.options.RectPackingMetaDataProvider" // since 0.6.1 (broken in 0.6.0)
            ].forEach [ providerClassName |
                try {
                    val clazz = Class.forName(providerClassName)
                    LayoutMetaDataService.instance.registerLayoutMetaDataProviders(
                        clazz.newInstance as ILayoutMetaDataProvider)
                } catch (Throwable t) {
                    LOG.info(providerClassName + " could not be registered (this is not necessarily an error)")
                }
            ]
        }
    }

    override String layout(String serializedGraph) {
        return deserialize(serializedGraph)
                    .map[it.layout]
                    .map[it.serialize.orElse(null)]
                    .orElse(null)
    }

    protected def ElkNode layout(ElkNode elkGraph) {
        layoutEngine.layout(elkGraph, new BasicProgressMonitor)
        return elkGraph
    }

    protected def Optional<ElkNode> deserialize(String serializedGraph) {
        val rs = new ResourceSetImpl()
        val r = rs.createResource(URI.createURI("dummy.elkg"))
        try {
            r.load(new URIConverter.ReadableInputStream(serializedGraph), null)
            if (!r.contents.empty) {
                return Optional.of(r.contents.head as ElkNode)
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "elkg deserialization failed (for concrete layout version).", e)
        }
        return Optional.empty
    }

    protected def Optional<String> serialize(ElkNode laidOutGraph) {
        val r = laidOutGraph.eResource
        val sw = new StringWriter()
        val os = new URIConverter.WriteableOutputStream(sw, "UTF-8")
        try {
            r.save(os, null);
            return Optional.of(sw.toString())
        } catch (IOException e) {
            LOG.log(Level.WARNING, "elkg serialization failed (for concrete layout version).", e)
        }
        return Optional.empty
    }

}
