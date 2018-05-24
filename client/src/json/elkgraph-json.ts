/*******************************************************************************
 * Copyright (c) 2017 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
export interface ElkPoint {
    x: number
    y: number
}

export interface ElkGraphElement {
    id: string
    labels?: ElkLabel[]
}

export interface ElkShape extends ElkGraphElement {
    x?: number
    y?: number
    width?: number
    height?: number
}

export interface ElkNode extends ElkShape {
    children?: ElkNode[]
    ports?: ElkPort[]
    edges?: ElkEdge[]
}

export interface ElkPort extends ElkShape { }

export interface ElkLabel extends ElkShape {
    text: string
}

export interface ElkEdge extends ElkGraphElement {
    junctionPoints?: ElkPoint[]
}

export interface ElkPrimitiveEdge extends ElkEdge {
    source: string
    sourcePort?: string
    target: string
    targetPort?: string
    sourcePoint?: ElkPoint
    targetPoint?: ElkPoint
    bendPoints?: ElkPoint[]
}

export function isPrimitive(edge: ElkEdge): edge is ElkPrimitiveEdge {
    return (edge as ElkPrimitiveEdge).source !== undefined && (edge as ElkPrimitiveEdge).target !== undefined;
}

export interface ElkExtendedEdge extends ElkEdge {
    sources: string[]
    targets: string[]
    sections: ElkEdgeSection[]
}

export function isExtended(edge: ElkEdge): edge is ElkExtendedEdge {
    return (edge as ElkExtendedEdge).sources !== undefined && (edge as ElkExtendedEdge).targets !== undefined;
}

export interface ElkEdgeSection extends ElkGraphElement {
    startPoint: ElkPoint
    endPoint: ElkPoint
    bendPoints?: ElkPoint[]
    incomingShape?: string
    outgoingShape?: string
    incomingSections?: string[]
    outgoingSections?: string[]
}