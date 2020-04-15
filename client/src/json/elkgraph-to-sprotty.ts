/*******************************************************************************
 * Copyright (c) 2017 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import {
    SNodeSchema, SEdgeSchema, SPortSchema, SLabelSchema, SGraphSchema, Point, Dimension
} from 'sprotty';
import {
    ElkShape, ElkNode, ElkPort, ElkLabel, ElkEdge, ElkEdgeSection, ElkPrimitiveEdge, ElkGraphElement, isPrimitive, isExtended
} from './elkgraph-json';

export class ElkGraphJsonToSprotty {

    private nodeIds: Set<string> = new Set();
    private edgeIds: Set<string> = new Set();
    private portIds: Set<string> = new Set();
    private labelIds: Set<string> = new Set();
    private sectionIds: Set<string> = new Set();

    public transform(elkGraph: ElkNode): SGraphSchema {
        const sGraph = <SGraphSchema> {
            type: 'graph',
            id: elkGraph.id || 'root',
            children: []
        };

        if (elkGraph.children) {
            const children = elkGraph.children.map(n => this.transformElkNode(n));
            sGraph.children.push(...children);
        }
        if (elkGraph.edges) {
            const sEdges = elkGraph.edges.map(e => this.transformElkEdge(e));
            sGraph.children!.push(...sEdges);
        }

        return sGraph;
    }

    private transformElkNode(elkNode: ElkNode): SNodeSchema {
        this.checkAndRememberId(elkNode, this.nodeIds);
        
        const sNode = <SNodeSchema> {
            type: 'node',
            id: elkNode.id,
            position: this.pos(elkNode),
            size: this.size(elkNode),
            children: []
        };
        // children
        if (elkNode.children) {
            const sNodes = elkNode.children.map(n => this.transformElkNode(n));
            sNode.children!.push(...sNodes);
        }
        // ports
        if (elkNode.ports) {
            const sPorts = elkNode.ports.map(p => this.transformElkPort(p));
            sNode.children!.push(...sPorts);
        }
        // labels
        if (elkNode.labels) {
            const sLabels = elkNode.labels.map(l => this.transformElkLabel(l));
            sNode.children!.push(...sLabels);
        }
        // edges
        if (elkNode.edges) {
            const sEdges = elkNode.edges.map(e => this.transformElkEdge(e));
            sNode.children!.push(...sEdges);
        }
        return sNode;
    }

    private transformElkPort(elkPort: ElkPort): SPortSchema {
        this.checkAndRememberId(elkPort, this.portIds);

        const sPort = <SPortSchema> {
            type: 'port',
            id: elkPort.id,
            position: this.pos(elkPort),
            size: this.size(elkPort),
            children: []
        };
        // labels
        if (elkPort.labels) {
            const sLabels = elkPort.labels.map(l => this.transformElkLabel(l));
            sPort.children!.push(...sLabels);
        }
        return sPort;
    }

    private transformElkLabel(elkLabel: ElkLabel): SLabelSchema {
        this.checkAndRememberId(elkLabel, this.labelIds);

        return <SLabelSchema> {
            type: 'label',
            id: elkLabel.id,
            text: elkLabel.text,
            position: this.pos(elkLabel),
            size: this.size(elkLabel)
        };
    }

    /**
     * Due to ELK issue #553 the computed layout of primitive edges is not transferred 
     * back in the correct way. Instead of using the primitive edge format the edge sections
     * of the extended edge format are returned. 
    */
    private isBugged(elkEdge: ElkPrimitiveEdge): Boolean {
        return (elkEdge as any).sections !== undefined && (elkEdge as any).sections.length > 0
    }

    private transferSectionBendpoints(section: ElkEdgeSection, sEdge: SEdgeSchema) {
        this.checkAndRememberId(section, this.sectionIds);
        sEdge.routingPoints!.push(section.startPoint);
        if (section.bendPoints) {
            sEdge.routingPoints!.push(...section.bendPoints);
        }
        sEdge.routingPoints!.push(section.endPoint);
    }

    private transformElkEdge(elkEdge: ElkEdge): SEdgeSchema {
        this.checkAndRememberId(elkEdge, this.edgeIds);

        const sEdge = <SEdgeSchema> {
            type: 'edge',
            id: elkEdge.id,
            sourceId: '',
            targetId: '',
            routingPoints: [],
            children: []
        };
        if (isPrimitive(elkEdge)) {
            sEdge.sourceId = elkEdge.source;
            sEdge.targetId = elkEdge.target;
            // Workaround for ELK issue #553
            if (this.isBugged(elkEdge)) {
                const section = (elkEdge as any).sections[0];
                this.transferSectionBendpoints(section, sEdge);
            } else {
                if (elkEdge.sourcePoint)
                    sEdge.routingPoints!.push(elkEdge.sourcePoint);
                if (elkEdge.bendPoints)
                    sEdge.routingPoints!.push(...elkEdge.bendPoints);
                if (elkEdge.targetPoint)
                    sEdge.routingPoints!.push(elkEdge.targetPoint);
            }
        } else if (isExtended(elkEdge)) {
            sEdge.sourceId = elkEdge.sources[0];
            sEdge.targetId = elkEdge.targets[0];
            if (elkEdge.sections) {
                elkEdge.sections.forEach(section => this.transferSectionBendpoints(section, sEdge));
            }
        }
        if (elkEdge.junctionPoints)  {
            elkEdge.junctionPoints.forEach((jp, i) => {
                const sJunction = <SNodeSchema> {
                    type: 'junction',
                    id: elkEdge.id + "_j" + i,
                    position: jp
                };
                sEdge.children!.push(sJunction);
            });
        }

        // TODO labels
        return sEdge;
    }

    private pos(elkShape: ElkShape): Point {
        return { x: elkShape.x || 0, y: elkShape.y || 0 };
    }

    private size(elkShape: ElkShape): Dimension {
        return <Dimension> { width: elkShape.width || 0, height: elkShape.height || 0 };
    }

    private checkAndRememberId(e: ElkGraphElement, set: Set<string>) {
        if (e.id == undefined) {
            throw Error("An element is missing an id.");
        } else if (set.has(e.id)) {
            throw Error("Duplicate id: " + e.id + ".");
        } else {
            set.add(e.id);
        }
    }

}