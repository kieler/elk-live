/*******************************************************************************
 * Copyright (c) 2017 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import { SNodeSchema, SEdgeSchema, SPortSchema, SLabelSchema, SGraphSchema, Point, Dimension } from 'sprotty/lib'
import { ElkShape, ElkNode, ElkPort, ElkLabel, ElkExtendedEdge, ElkGraphElement } from './elkgraph-json'

export class ElkGraphJsonToSprotty {

    private nodeIds: Set<string> = new Set()
    private edgeIds: Set<string> = new Set()
    private portIds: Set<string> = new Set()
    private labelIds: Set<string> = new Set()
    private sectionIds: Set<string> = new Set()

    public transform(elkGraph: ElkNode): SGraphSchema {
        let sGraph = <SGraphSchema> {
            type: 'graph',
            id: elkGraph.id || 'root',
            children: []
        }

        if (elkGraph.children) {
            let children = elkGraph.children.map(n => this.transformElkNode(n))
            sGraph.children.push(...children)
        }
        if (elkGraph.edges) {
            let sEdges = elkGraph.edges.map((e: ElkExtendedEdge) => this.transformElkEdge(e))
            sGraph.children!.push(...sEdges)
        }

        return sGraph
    }

    private transformElkNode(elkNode: ElkNode): SNodeSchema {
        this.checkAndRememberId(elkNode, this.nodeIds)
        
        let sNode = <SNodeSchema> {
            type: 'node',
            id: elkNode.id,
            position: this.pos(elkNode),
            size: this.size(elkNode),
            children: []
        }
        // children
        if (elkNode.children) {
            let sNodes = elkNode.children.map(n => this.transformElkNode(n))
            sNode.children!.push(...sNodes)
        }
        // ports
        if (elkNode.ports) {
            let sPorts = elkNode.ports.map(p => this.transformElkPort(p))
            sNode.children!.push(...sPorts)
        }
        // labels
        if (elkNode.labels) {
            let sLabels = elkNode.labels.map(l => this.transformElkLabel(l))
            sNode.children!.push(...sLabels)
        }
        // edges
        if (elkNode.edges) {
            let sEdges = elkNode.edges.map((e: ElkExtendedEdge) => this.transformElkEdge(e))
            sNode.children!.push(...sEdges)
        }
        return sNode
    }

    private transformElkPort(elkPort: ElkPort): SPortSchema {
        this.checkAndRememberId(elkPort, this.portIds)

        let sPort = <SPortSchema> {
            type: 'port',
            id: elkPort.id,
            position: this.pos(elkPort),
            size: this.size(elkPort),
            children: []
        }
        // labels
        if (elkPort.labels) {
            let sLabels = elkPort.labels.map(l => this.transformElkLabel(l))
            sPort.children!.push(...sLabels)
        }
        return sPort
    }

    private transformElkLabel(elkLabel: ElkLabel): SLabelSchema {
        this.checkAndRememberId(elkLabel, this.labelIds)

        let sLabel = <SLabelSchema> {
            type: 'label',
            id: elkLabel.id,
            text: elkLabel.text,
            position: this.pos(elkLabel),
            size: this.size(elkLabel)
        }
        return sLabel
    }

    private transformElkEdge(elkEdge: ElkExtendedEdge): SEdgeSchema {
        this.checkAndRememberId(elkEdge, this.edgeIds)

        let sEdge = <SEdgeSchema> {
            type: 'edge',
            id: elkEdge.id,
            sourceId: elkEdge.sources[0],
            targetId: elkEdge.targets[0],
            routingPoints: []
        }
        if (elkEdge.sections) {
            elkEdge.sections.forEach(section => {
                this.checkAndRememberId(section, this.sectionIds)
                sEdge.routingPoints!.push(<Point> section.startPoint)
                if (section.bendPoints) {
                    let bends = section.bendPoints.map(bp => <Point> bp)
                    sEdge.routingPoints!.push(...bends)
                }
                sEdge.routingPoints!.push(<Point> section.endPoint)
            })
        }

        // TODO labels
        return sEdge
    }

    private pos(elkShape: ElkShape): Point {
        return <Point> { x: elkShape.x || 0, y: elkShape.y || 0 }
    }

    private size(elkShape: ElkShape): Dimension {
        return <Dimension> { width: elkShape.width || 0, height: elkShape.height || 0 }
    }

    private checkAndRememberId(e: ElkGraphElement, set: Set<string>) {
        if (e.id == undefined) {
            throw Error("An element is missing an id.")
        } else if (set.has(e.id)) {
            throw Error("Duplicate id: " + e.id + ".")
        } else {
            set.add(e.id)
        }
    }

}