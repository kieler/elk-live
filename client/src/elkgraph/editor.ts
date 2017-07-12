/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import 'reflect-metadata'
import { listen, MessageConnection } from 'vscode-ws-jsonrpc'
import {
    BaseLanguageClient, CloseAction, ErrorAction,
    createMonacoServices, createConnection
} from 'monaco-languageclient'
import { TYPES } from 'sprotty/lib'
import { getParameterByName, setupModelLink } from "../url-parameters"
import createContainer from '../sprotty-config'
import LanguageDiagramServer from './language-diagram-server'
const WebSocket = require('reconnecting-websocket')

let initialContent = getParameterByName('initialContent')
if (initialContent === undefined) {
    initialContent = `algorithm: layered

node n1
node n2
node n3
edge n1 -> n2
edge n1 -> n3`
}

// Create Sprotty viewer
const sprottyContainer = createContainer()
sprottyContainer.bind(TYPES.ModelSource).to(LanguageDiagramServer).inSingletonScope()
const diagramServer = sprottyContainer.get<LanguageDiagramServer>(TYPES.ModelSource)
diagramServer.clientId = 'inmemory:/model.elkt'

// Create Monaco editor
monaco.languages.register({
    id: 'elkt',
    extensions: ['.elkt']
})
const editor = monaco.editor.create(document.getElementById('monaco-editor')!, {
    model: monaco.editor.createModel(initialContent, 'elkt', monaco.Uri.parse(diagramServer.clientId))
})
setupModelLink(editor, (event) => {
    return {
        initialContent: editor.getValue()
    }
})

// Create the web socket
const socketUrl = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/elkgraph`
const socketOptions = {
    maxReconnectionDelay: 10000,
    minReconnectionDelay: 1000,
    reconnectionDelayGrowFactor: 1.3,
    connectionTimeout: 10000,
    maxRetries: Infinity,
    debug: false
}
const webSocket = new WebSocket(socketUrl, undefined, socketOptions)
const services = createMonacoServices()
listen({
    webSocket,
    onConnection: connection => {
        const languageClient = createLanguageClient(connection)
        const disposable = languageClient.start()
        connection.onClose(() => {
            diagramServer.disconnect()
            disposable.dispose()
        })
    }
})

function createLanguageClient(messageConnection: MessageConnection): BaseLanguageClient {
    return new BaseLanguageClient({
        name: 'ELK Graph Language Client',
        clientOptions: {
            documentSelector: ['elkt'],
            // Disable the default error handler
            errorHandler: {
                error: () => ErrorAction.Continue,
                closed: () => CloseAction.DoNotRestart
            }
        },
        services,
        // Create a language client connection from the JSON RPC connection on demand
        connectionProvider: {
            get: (errorHandler, closeHandler) => {
                const connection = createConnection(messageConnection, errorHandler, closeHandler)
                diagramServer.listen(connection)
                return Promise.resolve(connection)
            }
        }
    })
}

