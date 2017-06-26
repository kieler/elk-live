import { Container, ContainerModule } from "inversify"
import {
    defaultModule, TYPES, ViewRegistry, WebSocketDiagramServer,
    boundsModule, fadeModule, viewportModule, selectModule, SGraphView, SGraphFactory, SLabelView
} from "sprotty/lib"
import { NodeView, PortView, EdgeView } from "./views"

const elkGraphModule = new ContainerModule((bind, unbind, isBound, rebind) => {
//    rebind(TYPES.ILogger).to(ConsoleLogger).inSingletonScope()
//    rebind(TYPES.LogLevel).toConstantValue(LogLevel.log)
    rebind(TYPES.IModelFactory).to(SGraphFactory).inSingletonScope()
    bind(TYPES.ModelSource).to(WebSocketDiagramServer).inSingletonScope()
})

export default () => {
    const container = new Container()
    container.load(defaultModule, selectModule, boundsModule, fadeModule, viewportModule, elkGraphModule)

    const viewRegistry = container.get<ViewRegistry>(TYPES.ViewRegistry)
    viewRegistry.register('graph', SGraphView)
    viewRegistry.register('node', NodeView)
    viewRegistry.register('port', PortView)
    viewRegistry.register('edge', EdgeView)
    viewRegistry.register('label', SLabelView)

    return container
}
