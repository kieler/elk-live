/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import java.io.File
import java.io.IOException
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.util.Optional
import java.util.logging.Level
import java.util.logging.Logger
import org.eclipse.elk.graph.ElkNode
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.URIConverter
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl

class ElkLayoutVersionWrapper {

    static val LOG = Logger.getLogger(ElkLayoutVersionWrapper.name)

    val Method LAYOUT_METHOD
    val Object LAYOUTER_INSTANCE

    new(String jarPath) {
        val methodAndInstance = try {
                // Note that the parent class loader is explicitly set to 'null'
                val classLoader = new URLClassLoader(#[new File(jarPath).toURI().toURL()], null)
                val clazz = classLoader.loadClass("de.cau.cs.kieler.elkgraph.web.version.ElkLayoutVersion")
                clazz.getMethod("layout", typeof(String)) -> clazz.newInstance()
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "Failed to instantiate layout wrapper for " + jarPath + ".", t)
                null -> null
            }
        LAYOUT_METHOD = methodAndInstance.key
        LAYOUTER_INSTANCE = methodAndInstance.value
    }

    def Optional<ElkNode> layout(ElkNode graph) {
        if (LAYOUTER_INSTANCE === null || LAYOUT_METHOD === null) {
            return Optional.empty
        }
		return serialize(graph).map[
			try {
				LAYOUT_METHOD.invoke(LAYOUTER_INSTANCE, it) as String
			} catch (InvocationTargetException ite) {
				// TODO pass to caller
				LOG.log(Level.WARNING, "", ite)
				null // translates into Optional.empty
			}
		].map[it.deserialize.orElse(null)]
    }

    protected def Optional<String> serialize(ElkNode laidOutGraph) {
        // We need to put the graph into a new (xmi) resource since 
        // it currently resides in an xtext resource which wouldn't 
        // yield the serialized output we desire.
        val rs = new ResourceSetImpl()
        val xmiR = rs.createResource(URI.createURI("dummy.elkg"))
        xmiR.contents += laidOutGraph

        val sw = new StringWriter()
        val os = new URIConverter.WriteableOutputStream(sw, "UTF-8")
        try {
            xmiR.save(os, null);
            return Optional.of(sw.toString())
        } catch (IOException e) {
            LOG.log(Level.WARNING, "elkg serialization failed (for layout wrapper).", e)
        }
        return Optional.empty
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
            LOG.log(Level.WARNING, "elkg deserialization failed (for layout wrapper).", e)
        }
        return Optional.empty
    }

}
