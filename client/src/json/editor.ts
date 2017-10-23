/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import 'reflect-metadata'

import { TYPES, LocalModelSource } from 'sprotty/lib'
import { getParameters, setupModelLink } from "../url-parameters"
import createContainer from '../sprotty-config'
import { ElkGraphJsonToSprotty } from './elkgraph-to-sprotty'
import ELK from 'elkjs/lib/elk-api'

import JSON5 = require('json5')
import LZString = require('lz-string')

const urlParameters = getParameters()

let initialContent: string
if (urlParameters.compressedContent !== undefined) {
    initialContent = LZString.decompressFromEncodedURIComponent(urlParameters.compressedContent)
} else if (urlParameters.initialContent !== undefined) {
    initialContent = decodeURIComponent(urlParameters.initialContent)
} else {
    initialContent = `{
  id: "root",
  properties: { 'algorithm': 'layered' },
  children: [
    { id: "n1", width: 30, height: 30 },
    { id: "n2", width: 30, height: 30 },
    { id: "n3", width: 30, height: 30 }
  ],
  edges: [
    { id: "e1", sources: [ "n1" ], targets: [ "n2" ] },
    { id: "e2", sources: [ "n1" ], targets: [ "n3" ] } 
  ]
}`
}

// Create Monaco editor
monaco.languages.register({
    id: 'json',
    extensions: ['.json'],
    aliases: ['JSON', 'json'],
    mimetypes: ['application/json'],
})
const editor = monaco.editor.create(document.getElementById('monaco-editor')!, {
    model: monaco.editor.createModel(initialContent, 'json', monaco.Uri.parse('inmemory:/model.json'))
})
editor.updateOptions({
    minimap: { enabled: false }
})

// Create Sprotty viewer
const sprottyContainer = createContainer()
sprottyContainer.bind(TYPES.ModelSource).to(LocalModelSource).inSingletonScope()
const modelSource = sprottyContainer.get<LocalModelSource>(TYPES.ModelSource)

// Set up ELK
const elk = new ELK({
    workerUrl: './elk/elk-worker.min.js'
})

// Register listener
editor.getModel().onDidChangeContent(e => updateModel())

// Initial layout
updateModel()

setupModelLink(editor, (event) => {
    return {
        compressedContent: LZString.compressToEncodedURIComponent(editor.getValue())
    }
})

function updateModel() {
    try {
        let json = JSON5.parse(editor.getValue())
        monaco.editor.setModelMarkers(editor.getModel(), "", [])
        elk.layout(json)
            .then(g => {
                let sGraph = new ElkGraphJsonToSprotty().transform(g);
                modelSource.updateModel(sGraph)
            })
            .catch(e => {
                let markers = [ errorToMarker(e) ]
                monaco.editor.setModelMarkers(editor.getModel(), "", markers)
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
