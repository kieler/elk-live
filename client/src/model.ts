/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import {
    SGraphFactory, SModelElementSchema, SParentElement, SChildElement, SNode, SPort, moveFeature
} from "sprotty/lib"

export class ElkGraphFactory extends SGraphFactory {

    createElement(schema: SModelElementSchema, parent?: SParentElement): SChildElement {
        if (this.isNodeSchema(schema))
            return this.initializeChild(new ElkNode(), schema, parent)
        else if (this.isPortSchema(schema))
            return this.initializeChild(new ElkPort(), schema, parent)
        else
            return super.createElement(schema, parent)
    }

}

// Disable the 'move' feature

export class ElkNode extends SNode {
    hasFeature(feature: symbol): boolean {
        if (feature === moveFeature)
            return false
        else
            return super.hasFeature(feature)
    }
}

export class ElkPort extends SPort {
    hasFeature(feature: symbol): boolean {
        if (feature === moveFeature)
            return false
        else
            return super.hasFeature(feature)
    }
}
