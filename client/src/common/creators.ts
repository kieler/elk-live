/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import { Container } from 'inversify';
import { CloseAction, createConnection, ErrorAction, MonacoLanguageClient, MonacoServices } from 'monaco-languageclient';
import ReconnectingWebSocket from 'reconnecting-websocket';
import { createRandomId, IActionDispatcher, TYPES } from 'sprotty';
import { listen, MessageConnection } from 'vscode-ws-jsonrpc';
import createContainer from '../sprotty-config';
import { LanguageDiagramServer } from './language-diagram-server';


export function createSprottyViewer(requiresActionDispatcher = false): [Container, LanguageDiagramServer, IActionDispatcher] {
    const sprottyContainer = createContainer();
    sprottyContainer.bind(TYPES.ModelSource).to(LanguageDiagramServer).inSingletonScope();

    const diagramServer = sprottyContainer.get<LanguageDiagramServer>(TYPES.ModelSource);
    diagramServer.clientId = 'sprotty';

    const actionDispatcher = sprottyContainer.get<IActionDispatcher>(TYPES.IActionDispatcher);

    return [sprottyContainer, diagramServer, actionDispatcher];
}



export function createMonacoEditor(divId: string, modelExt: string = "elkt", initialContent: string = "") {
    // Create Monaco editor
    const modelUri = `inmemory:/${createRandomId(24)}.${modelExt}`;
    const editor = monaco.editor.create(document.getElementById(divId)!, {
        model: monaco.editor.createModel(initialContent, modelExt, monaco.Uri.parse(modelUri))
    });
    editor.updateOptions({
        minimap: { enabled: false }
    });
    // Resize the monaco editor upon window resize.
    // There's also an option 'automaticLayout: true' that could be passed to above 'create' method,
    // however, this cyclically checks the current state and thus is less performant. 
    window.onresize = () => editor.layout();
    MonacoServices.install(editor);

    return editor;
}

/**
 * Configuration options of a websocket-based language/diagram server.
 */
export interface LdsConfig {
    name: string;
    endpoint: string;
    documentSelector: string[];
    diagramServer?: LanguageDiagramServer;
}

export function openWebSocketElkGraph(config: LdsConfig) {

    const socketUrl = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/${config.endpoint}`;
    const socketOptions = {
        maxReconnectionDelay: 10000,
        minReconnectionDelay: 1000,
        reconnectionDelayGrowFactor: 1.3,
        connectionTimeout: 10000,
        maxRetries: 20,
        debug: false
    };
    const webSocket = new ReconnectingWebSocket(socketUrl, [], socketOptions);
    listen({
        webSocket: webSocket as any as WebSocket,
        onConnection: connection => {
            const languageClient = createElkLanguageClient(connection, config)
            const disposable = languageClient.start()
            connection.onClose(() => {
                if (config.diagramServer !== undefined) {
                    config.diagramServer.disconnect()
                }
                disposable.dispose()
            })
        }
    });
}

function createElkLanguageClient(messageConnection: MessageConnection, config: LdsConfig): MonacoLanguageClient {
    return new MonacoLanguageClient({
        name: config.name,
        clientOptions: {
            documentSelector: config.documentSelector,
            // Disable the default error handler
            errorHandler: {
                error: () => ErrorAction.Continue,
                closed: () => CloseAction.DoNotRestart
            }
        },
        // Create a language client connection from the JSON RPC connection on demand
        connectionProvider: {
            get: (errorHandler, closeHandler) => {
                const connection = createConnection(messageConnection, errorHandler, closeHandler)
                if (config.diagramServer !== undefined) {
                    config.diagramServer.listen(connection)
                }
                return Promise.resolve(connection)
            }
        }
    });
}
