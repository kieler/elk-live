/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import 'reflect-metadata';

import { listen, MessageConnection } from 'vscode-ws-jsonrpc';
import {
    MonacoLanguageClient, CloseAction, ErrorAction, MonacoServices, createConnection
} from 'monaco-languageclient';
import { TYPES, createRandomId } from 'sprotty';
import { getParameters, setupModelLink } from "../url-parameters";
import createContainer from '../sprotty-config';
import LanguageDiagramServer from './language-diagram-server';
import ReconnectingWebSocket from 'reconnecting-websocket';
import LZString = require('lz-string');

require('./elkt-language');

const urlParameters = getParameters();

let initialContent: string;
if (urlParameters.compressedContent !== undefined) {
    initialContent = LZString.decompressFromEncodedURIComponent(urlParameters.compressedContent);
} else if (urlParameters.initialContent !== undefined) {
    initialContent = decodeURIComponent(urlParameters.initialContent);
} else {
    initialContent = `algorithm: layered

node n1
node n2
node n3
edge n1 -> n2
edge n1 -> n3`;
}

// Create Sprotty viewer
const sprottyContainer = createContainer();
sprottyContainer.bind(TYPES.ModelSource).to(LanguageDiagramServer).inSingletonScope();
const diagramServer = sprottyContainer.get<LanguageDiagramServer>(TYPES.ModelSource);
diagramServer.clientId = 'sprotty'

// Create Monaco editor
const modelUri = `inmemory:/${createRandomId(24)}.elkt`;
const editor = monaco.editor.create(document.getElementById('monaco-editor')!, {
    model: monaco.editor.createModel(initialContent, 'elkt', monaco.Uri.parse(modelUri))
});
editor.updateOptions({
    minimap: { enabled: false }
});
setupModelLink(editor, (event) => {
    return {
        compressedContent: LZString.compressToEncodedURIComponent(editor.getValue())
    }
});
MonacoServices.install(editor);

// Create the web socket
const socketUrl = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/elkgraph`;
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
        const languageClient = createLanguageClient(connection)
        const disposable = languageClient.start()
        connection.onClose(() => {
            diagramServer.disconnect()
            disposable.dispose()
        })
    }
});

function createLanguageClient(messageConnection: MessageConnection): MonacoLanguageClient {
    return new MonacoLanguageClient({
        name: 'ELK Graph Language Client',
        clientOptions: {
            documentSelector: ['elkt'],
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
                diagramServer.listen(connection)
                return Promise.resolve(connection)
            }
        }
    });
}

