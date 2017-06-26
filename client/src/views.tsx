import * as snabbdom from "snabbdom-jsx"
import { VNode } from "snabbdom/vnode"
import {
    RenderingContext, SNode, SEdge, SPort, PolylineEdgeView, RectangularNodeView, angle, Point, toDegrees
} from "sprotty/lib"

const JSX = {createElement: snabbdom.svg}

export class NodeView extends RectangularNodeView {
    render(node: SNode, context: RenderingContext): VNode {
        return <g>
            <rect class-node={true} class-mouseover={node.hoverFeedback} class-selected={node.selected}
                    x="0" y="0" width={node.bounds.width} height={node.bounds.height}></rect>
        </g>
    }
}

export class PortView extends RectangularNodeView {
    render(port: SPort, context: RenderingContext): VNode {
        return <g>
            <rect class-port={true} class-mouseover={port.hoverFeedback} class-selected={port.selected}
                    x="0" y="0" width={port.bounds.width} height={port.bounds.height}></rect>
        </g>
    }
}

export class EdgeView extends PolylineEdgeView {
    protected renderAdditionals(edge: SEdge, segments: Point[], context: RenderingContext): VNode[] {
        const p1 = segments[segments.length - 2]
        const p2 = segments[segments.length - 1]
        return [
            <path class-edge={true} class-arrow={true} d="M 0,0 L 10,-4 L 10,4 Z"
                  transform={`rotate(${toDegrees(angle(p2, p1))} ${p2.x} ${p2.y}) translate(${p2.x} ${p2.y})`}/>
        ]
    }
}
