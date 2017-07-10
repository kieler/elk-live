/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import 'reflect-metadata'

import { TYPES, LocalModelSource } from 'sprotty/lib'
import { getParameterByName, setupModelLink } from "./url-parameters"
import createContainer from './di.config'
import { ElkGraphJsonToSprotty } from './elkgraph-to-sprotty'

import JSON5 = require('json5');
import elkjs = require('elkjs')

// Create Monaco editor
monaco.languages.register({
    id: 'json',
    extensions: ['.json'],
    aliases: ['JSON', 'json'],
    mimetypes: ['application/json'],
})
let initialContent = getParameterByName('initialContent')
if (initialContent === undefined) {
    initialContent =  `{
  id: "root",
  properties: { 'algorithm': 'layered' },
  children: [
    { id: "n1", width: 10, height: 10 },
    { id: "n2", width: 10, height: 10 },
    { id: "n3", width: 10, height: 10 }
  ],
  edges: [
    { id: "e1", sources: [ "n1" ], targets: [ "n2" ] },
    { id: "e2", sources: [ "n1" ], targets: [ "n3" ] } 
  ]
}`
}
const editor = monaco.editor.create(document.getElementById('monaco-editor')!, {
    model: monaco.editor.createModel(initialContent, 'json', monaco.Uri.parse('inmemory:/model.json'))
})
// initial layout
updateModel()

setupModelLink(editor, (event) => {
    return {
        initialContent: editor.getValue()
    }
})

// Create Sprotty viewer
const sprottyContainer = createContainer('local')
const modelSource = sprottyContainer.get<LocalModelSource>(TYPES.ModelSource)

// register listener
editor.getModel().onDidChangeContent(e => updateModel())

function updateModel() {
    try {
        let json = JSON5.parse(editor.getValue())
        monaco.editor.setModelMarkers(editor.getModel(), "", [])
        elkjs.layout({ 
            graph: json,
            callback: (err, graph) => {
                if (err === null) {
                    let sGraph = new ElkGraphJsonToSprotty().transform(graph);
                    modelSource.updateModel(sGraph)
                } else {
                    let markers = [ errorToMarker(err) ]
                    monaco.editor.setModelMarkers(editor.getModel(), "", markers)
                }
            } 
        })
    } catch (e) {
        let markers = [ errorToMarker(e) ]
        monaco.editor.setModelMarkers(editor.getModel(), "", markers)
     }
}

function errorToMarker(e: any): monaco.editor.IMarkerData {
    return <monaco.editor.IMarkerData> {
        severity: monaco.Severity.Error,
        startLineNumber: e.lineNumber || 0,
        startColumn: e.columnNumber || 0,
        message: e.message
    }
}
