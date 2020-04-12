/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import java.util.Map
import java.util.logging.Logger
import org.eclipse.elk.core.util.persistence.ElkGraphResourceFactory
import org.eclipse.elk.graph.ElkGraphPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtend.lib.annotations.Accessors

final class ElkLayoutVersionRegistry {

    static val LOG = Logger.getLogger(ElkLayoutVersionRegistry.name)

    @Accessors
    static val Map<String, ElkLayoutVersionWrapper> versionToWrapper = {
        
        // Make sure the elkg xmi format is properly registered
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("elkg", new ElkGraphResourceFactory());
        ElkGraphPackage.eINSTANCE.eClass();
        
        val elkJars = System.getProperty("elkJars")
        if (elkJars.nullOrEmpty) {
            LOG.severe("Make sure the system property 'elkJars' is set properly.")
            return newImmutableMap()
        }
        val strings = System.getProperty("elkJars").split(',')
        return newImmutableMap(strings.map [ jarPath |
            jarPath.substring(jarPath.lastIndexOf('-') - 5, jarPath.lastIndexOf('-')) ->
                new ElkLayoutVersionWrapper(jarPath)
        ])
    }

}
