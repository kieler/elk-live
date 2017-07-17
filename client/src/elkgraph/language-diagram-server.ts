/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import { IConnection } from "vscode-base-languageclient/lib/connection"
import { DiagramServer, ActionMessage } from "sprotty/lib"

const DIAGRAM_ENDPOINT_NOTIFICATION = 'diagram/accept'
const DID_CLOSE_NOTIFICATION = 'diagram/didClose'

export default class LanguageDiagramServer extends DiagramServer {
    protected connection?: IConnection

    listen(connection: IConnection) {
        connection.onNotification(DIAGRAM_ENDPOINT_NOTIFICATION, (message: ActionMessage) => {
            this.messageReceived(message)
        })
        this.connection = connection
    }

    disconnect() {
        if (this.connection !== undefined) {
            this.connection.sendNotification(DID_CLOSE_NOTIFICATION, this.clientId)
            this.connection = undefined
        }
    }

    protected sendMessage(message: ActionMessage): void {
        if (this.connection !== undefined) {
            this.connection.sendNotification(DIAGRAM_ENDPOINT_NOTIFICATION, message)
        }
    }
}
