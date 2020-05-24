/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import { IConnection } from "monaco-languageclient/lib/connection";
import { DiagramServer, ActionMessage, ActionHandlerRegistry, Action } from "sprotty";

const DIAGRAM_ENDPOINT_NOTIFICATION = 'diagram/accept';
const DID_CLOSE_NOTIFICATION = 'diagram/didClose';

export class ChangeLayoutVersionAction implements Action {
    static readonly KIND = 'versionChange';
    readonly kind = ChangeLayoutVersionAction.KIND;

    constructor(public readonly version?: string) {}
}

export class LanguageDiagramServer extends DiagramServer {
    protected connection?: IConnection;

    initialize(registry: ActionHandlerRegistry): void {
        super.initialize(registry);
        registry.register(ChangeLayoutVersionAction.KIND, this);
    }

    listen(connection: IConnection) {
        connection.onNotification(DIAGRAM_ENDPOINT_NOTIFICATION, (message: ActionMessage) => {
            this.messageReceived(message)
            const loading = document.getElementById('loading-sprotty');
            if (loading != undefined) {
                loading.style.display = 'none';
            }
        });
        this.connection = connection;
    }

    disconnect() {
        if (this.connection !== undefined) {
            this.connection.sendNotification(DID_CLOSE_NOTIFICATION, this.clientId);
            this.connection = undefined;
        }
    }

    protected sendMessage(message: ActionMessage): void {
        if (this.connection !== undefined) {
            this.connection.sendNotification(DIAGRAM_ENDPOINT_NOTIFICATION, message);
        }
    }
}
