import {
    SGraphFactory, SModelElementSchema, SParentElement, SChildElement, SNode,
    selectFeature, boundsFeature, layoutFeature, fadeFeature, hoverFeedbackFeature, popupFeature
} from "sprotty/lib"

export class ElkGraphFactory extends SGraphFactory {

    createElement(schema: SModelElementSchema, parent?: SParentElement): SChildElement {
        if (this.isNodeSchema(schema))
            return this.initializeChild(new ElkNode(), schema, parent)
        else
            return super.createElement(schema, parent)
    }

}

export class ElkNode extends SNode {
    hasFeature(feature: symbol): boolean {
        return feature === selectFeature || feature === boundsFeature || feature === layoutFeature
            || feature === fadeFeature || feature === hoverFeedbackFeature || feature === popupFeature
    }
}
