/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import { Container, ContainerModule } from "inversify"
import {
    TYPES, ViewRegistry, defaultModule, boundsModule, fadeModule, viewportModule, selectModule, moveModule, hoverModule,
    exportModule, SGraphView, ConsoleLogger, LogLevel, overrideViewerOptions, SvgExporter
} from "sprotty/lib"
import { ElkNodeView, ElkPortView, ElkEdgeView, ElkLabelView, JunctionView } from "./views"
import { ElkGraphFactory } from "./sprotty-model"

class FilteringSvgExporter extends SvgExporter {
    protected isExported(styleSheet: CSSStyleSheet): boolean {
        return styleSheet.href !== null && (styleSheet.href.endsWith('diagram.css') || styleSheet.href.endsWith('sprotty.css'))
    }
}

export default () => {
    const elkGraphModule = new ContainerModule((bind, unbind, isBound, rebind) => {
        rebind(TYPES.ILogger).to(ConsoleLogger).inSingletonScope()
        rebind(TYPES.LogLevel).toConstantValue(LogLevel.warn)
        rebind(TYPES.IModelFactory).to(ElkGraphFactory).inSingletonScope()
        rebind(TYPES.SvgExporter).to(FilteringSvgExporter).inSingletonScope()
    })
    const container = new Container()
    container.load(defaultModule, selectModule, boundsModule, moveModule, fadeModule, hoverModule, viewportModule, exportModule, elkGraphModule)
    overrideViewerOptions(container, {
        needsClientLayout: false
    })

    const viewRegistry = container.get<ViewRegistry>(TYPES.ViewRegistry)
    viewRegistry.register('graph', SGraphView)
    viewRegistry.register('node', ElkNodeView)
    viewRegistry.register('port', ElkPortView)
    viewRegistry.register('edge', ElkEdgeView)
    viewRegistry.register('label', ElkLabelView)
    viewRegistry.register('junction', JunctionView)

    return container
}
