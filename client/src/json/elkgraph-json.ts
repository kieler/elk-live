/*******************************************************************************
 * Copyright (c) 2017 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
export class ElkPoint {
    x: number
    y: number
}

export abstract class ElkGraphElement {
    id: string
    labels?: ElkLabel[]
}

export abstract class ElkShape extends ElkGraphElement {
    x?: number
    y?: number
    width?: number
    height?: number
}

export class ElkNode extends ElkShape {
    children?: ElkNode[]
    ports?: ElkPort[]
    edges?: ElkEdge[]
}

export class ElkPort extends ElkShape { }

export class ElkLabel extends ElkShape {
    text: string
}

export abstract class ElkEdge extends ElkGraphElement { }

export class ElkPrimitiveEdge extends ElkEdge {
    source: string
    sourcePort?: string
    target: string
    targetPort?: string
    sourcePoint?: ElkPoint
    targetPoint?: ElkPoint
    bendPoints?: ElkPoint[]
}

export class ElkExtendedEdge extends ElkEdge {
    sources: string[]
    targets: string[]
    sections: ElkEdgeSection[]
}

export class ElkEdgeSection extends ElkGraphElement {
    startPoint: ElkPoint
    endPoint: ElkPoint
    bendPoints?: ElkPoint[]
    incomingShape?: string
    outgoingShape?: string
    incomingSections?: string[]
    outgoingSections?: string[]
}