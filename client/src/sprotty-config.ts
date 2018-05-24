/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import { Container, ContainerModule } from "inversify"
import {
    TYPES, defaultModule, boundsModule, fadeModule, viewportModule, selectModule, moveModule, hoverModule,
    exportModule, SGraphView, ConsoleLogger, LogLevel, configureViewerOptions, SvgExporter, configureModelElement,
    SGraph, SGraphFactory, SLabel, edgeEditModule
} from "sprotty/lib"
import { ElkNodeView, ElkPortView, ElkEdgeView, ElkLabelView, JunctionView } from "./views"
import { ElkNode, ElkPort, ElkEdge, ElkJunction } from "./sprotty-model"

class FilteringSvgExporter extends SvgExporter {
    protected isExported(styleSheet: CSSStyleSheet): boolean {
        return styleSheet.href !== null && (styleSheet.href.endsWith('diagram.css') || styleSheet.href.endsWith('sprotty.css'))
    }
}

export default () => {
    const elkGraphModule = new ContainerModule((bind, unbind, isBound, rebind) => {
        rebind(TYPES.ILogger).to(ConsoleLogger).inSingletonScope()
        rebind(TYPES.LogLevel).toConstantValue(LogLevel.warn)
        rebind(TYPES.IModelFactory).to(SGraphFactory).inSingletonScope()
        rebind(TYPES.SvgExporter).to(FilteringSvgExporter).inSingletonScope()
        const context = { bind, unbind, isBound, rebind };
        configureModelElement(context, 'graph', SGraph, SGraphView);
        configureModelElement(context, 'node', ElkNode, ElkNodeView);
        configureModelElement(context, 'port', ElkPort, ElkPortView);
        configureModelElement(context, 'edge', ElkEdge, ElkEdgeView);
        configureModelElement(context, 'label', SLabel, ElkLabelView);
        configureModelElement(context, 'junction', ElkJunction, JunctionView);
        configureViewerOptions(context, {
            needsClientLayout: false
        });
    })
    const container = new Container()
    container.load(defaultModule, selectModule, boundsModule, moveModule, fadeModule, hoverModule, viewportModule, exportModule,
        edgeEditModule, elkGraphModule)
    return container
}
