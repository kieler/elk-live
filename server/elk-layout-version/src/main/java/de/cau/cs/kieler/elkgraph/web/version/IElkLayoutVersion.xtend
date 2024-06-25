/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web.version

interface IElkLayoutVersion {
    
    /**
     * @param serializedGraph serialized representation of the graph to be laid out, in elkg XMI format.
     * @return a serialized representation of the laid out graph, in elkg XMI format.
     */
    def String layout(String serializedGraph)
    
}
