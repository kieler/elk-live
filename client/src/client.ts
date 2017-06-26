import "reflect-metadata"
import { listen, MessageConnection } from 'vscode-ws-jsonrpc'
import {
    BaseLanguageClient, CloseAction, ErrorAction,
    createMonacoServices, createConnection
} from 'monaco-languageclient'
import { TYPES } from 'sprotty/lib'
import createContainer from './di.config'
const WebSocket = require('reconnecting-websocket')

// create Monaco editor
monaco.languages.register({
    id: 'elkt',
    extensions: ['.elkt']
})
const initialContent = `node n1`
monaco.editor.create(document.getElementById('monaco-editor')!, {
    model: monaco.editor.createModel(initialContent, 'elkt', monaco.Uri.parse('inmemory://model.json'))
})

// Create the web socket
const socketUrl = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/`
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
        connection.onClose(() => disposable.dispose())
    }
})

function createLanguageClient(connection: MessageConnection): BaseLanguageClient {
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
                return Promise.resolve(createConnection(connection, errorHandler, closeHandler))
            }
        }
    })
}

// TODO
const sprottyContainer = createContainer()
sprottyContainer.get(TYPES.ModelSource)
