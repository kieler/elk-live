import { IConnection } from "vscode-base-languageclient/lib/connection"
import { DiagramServer, ActionMessage } from "sprotty/lib"

const DIAGRAM_ENDPOINT_NOTIFICATION = 'diagram/accept'

export default class LanguageDiagramServer extends DiagramServer {
    protected connection?: IConnection

    listen(connection: IConnection) {
        connection.onNotification(DIAGRAM_ENDPOINT_NOTIFICATION, (message: ActionMessage) => {
            this.messageReceived(message)
        })
        this.connection = connection
    }

    disconnect() {
        this.connection = undefined
    }

    protected sendMessage(message: ActionMessage): void {
        if (this.connection !== undefined) {
            this.connection.sendNotification(DIAGRAM_ENDPOINT_NOTIFICATION, message)
        }
    }
}
