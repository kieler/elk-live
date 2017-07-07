/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import 'reflect-metadata'
import { TYPES, LocalModelSource, SNodeSchema, SEdgeSchema, SGraphSchema } from 'sprotty/lib'
import { getParameterByName, setupModelLink } from "./url-parameters"
import createContainer from './di.config'

// Create Monaco editor
monaco.languages.register({
    id: 'json',
    extensions: ['.json'],
    aliases: ['JSON', 'json'],
    mimetypes: ['application/json'],
})
let initialContent = getParameterByName('initialContent')
if (initialContent === undefined) {
    initialContent = '{\n\t\n}'
}
const editor = monaco.editor.create(document.getElementById('monaco-editor')!, {
    model: monaco.editor.createModel(initialContent, 'json', monaco.Uri.parse('inmemory:/model.json'))
})
setupModelLink(editor, (event) => {
    return {
        initialContent: editor.getValue()
    }
})

// Create Sprotty viewer
const sprottyContainer = createContainer('local')
const modelSource = sprottyContainer.get<LocalModelSource>(TYPES.ModelSource)

// TODO add text change listener to Monaco editor
// TODO invoke elk layout algorithm on json model
// TODO implement transformation from json to sprotty model

// hard-coded sprotty model as example
modelSource.model = <SGraphSchema> {
    type: 'graph',
    id: 'graph',
    children: [
        <SNodeSchema> {
            type: 'node',
            id: 'node0',
            position: { x: 10, y: 10 },
            size: { width: 30, height: 30 }
        },
        <SNodeSchema> {
            type: 'node',
            id: 'node1',
            position: { x: 80, y: 10 },
            size: { width: 30, height: 30 }
        },
        <SEdgeSchema> {
            type: 'edge',
            id: 'edge0',
            sourceId: 'node0',
            targetId: 'node1'
        }
    ]
}
