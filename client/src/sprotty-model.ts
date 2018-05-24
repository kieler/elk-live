/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import {
    SNode, RectangularNode, RectangularPort,
    moveFeature, selectFeature, hoverFeedbackFeature, SEdge, editFeature
} from "sprotty/lib"

export class ElkNode extends RectangularNode {
    hasFeature(feature: symbol): boolean {
        if (feature === moveFeature)
            return false
        else
            return super.hasFeature(feature)
    }
}

export class ElkPort extends RectangularPort {
    hasFeature(feature: symbol): boolean {
        if (feature === moveFeature)
            return false
        else
            return super.hasFeature(feature)
    }
}

export class ElkEdge extends SEdge {
    hasFeature(feature: symbol): boolean {
        if (feature === editFeature)
            return false
        else
            return super.hasFeature(feature)
    }
}

export class ElkJunction extends SNode {
    hasFeature(feature: symbol): boolean {
        if (feature === moveFeature || feature === selectFeature || feature === hoverFeedbackFeature)
            return false
        else
            return super.hasFeature(feature)
    }
}
