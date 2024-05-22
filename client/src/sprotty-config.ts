/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
import { Container, ContainerModule } from "inversify";
import {
    TYPES, defaultModule, boundsModule, fadeModule, viewportModule, selectModule, moveModule, hoverModule,
    exportModule, SGraphView, ConsoleLogger, LogLevel, configureViewerOptions, SvgExporter, configureModelElement,
    SGraph, SGraphFactory, SLabel, edgeEditModule, undoRedoModule, updateModule, routingModule, modelSourceModule, labelEditModule
} from "sprotty";
import { ElkNodeView, ElkPortView, ElkEdgeView, ElkLabelView, JunctionView } from "./views";
import { ElkNode, ElkPort, ElkEdge, ElkJunction } from "./sprotty-model";

class FilteringSvgExporter extends SvgExporter {
    protected isExported(styleSheet: CSSStyleSheet): boolean {
        return styleSheet.href !== null && (styleSheet.href.endsWith('diagram.css') || styleSheet.href.endsWith('sprotty.css'));
    }
}

export default () => {
    const elkGraphModule = new ContainerModule((bind, unbind, isBound, rebind) => {
        rebind(TYPES.ILogger).to(ConsoleLogger).inSingletonScope();
        rebind(TYPES.LogLevel).toConstantValue(LogLevel.warn);
        rebind(TYPES.IModelFactory).to(SGraphFactory).inSingletonScope();
        rebind(TYPES.SvgExporter).to(FilteringSvgExporter).inSingletonScope();
        const context = { bind, unbind, isBound, rebind };
        configureModelElement(context, 'graph', SGraph, SGraphView);
        configureModelElement(context, 'node', ElkNode, ElkNodeView);
        configureModelElement(context, 'port', ElkPort, ElkPortView);
        configureModelElement(context, 'edge', ElkEdge, ElkEdgeView);
        configureModelElement(context, 'label', SLabel, ElkLabelView);
        configureModelElement(context, 'junction', ElkJunction, JunctionView);
        // Note that with our configuration (sprotty model update is initiated by the server after 
        // a 'textDocument/didChange' event of the monaco editor), the following values are never 
        // actually sent with any request to the server. Hence it is required to override the 
        // corresponding methods in the ElkDiagramServer manually.
        configureViewerOptions(context, {
            needsClientLayout: false,
            // The server-side layout is performed explicitly by the diagram generator, hence 
            // the "regular" layout mechanism must not be used
            needsServerLayout: false
        });
    })
    const container = new Container();
    container.load(defaultModule, selectModule, boundsModule, moveModule, fadeModule, hoverModule,
        updateModule, undoRedoModule, viewportModule, routingModule, exportModule, modelSourceModule,
        edgeEditModule, labelEditModule, elkGraphModule);
    return container;
}
