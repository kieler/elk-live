/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web.version

interface IElkLayoutVersion {
    
    /**
     * @param serializedGraph serialized representation of the graph to be laid out, in elkg XMI format.
     * @return a serialized representation of the laid out graph, in elkg XMI format.
     */
    def String layout(String serializedGraph)
    
}
