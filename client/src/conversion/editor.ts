/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import 'reflect-metadata';

// Load monaco editor languages
require('../json/elk-json-language');
require('../elkgraph/elkt-language');

const editorInput = monaco.editor.create(document.getElementById('monaco-editor-input')!);
editorInput.updateOptions({
    minimap: { enabled: false }
});
const editorOutput = monaco.editor.create(document.getElementById('monaco-editor-output')!);

// Resize the monaco editor upon window resize.
// There's also an option 'automaticLayout: true' that could be passed to above 'create' method,
// however, this cyclically checks the current state and thus is less performant. 
window.onresize = () => {
    editorInput.layout();
    editorOutput.layout();
}

function getSelection(element: HTMLSelectElement): string {
    return element.options[element.selectedIndex].value;
}

function setLanguageBasedOnFormatSelection(editor: monaco.editor.IStandaloneCodeEditor, select: HTMLSelectElement) {
    const selectedFormat = getSelection(select);
    switch (selectedFormat) {
        case "elkt":
        case "json":
            monaco.editor.setModelLanguage(editor.getModel()!, selectedFormat);
            break;
        case "elkg":
            monaco.editor.setModelLanguage(editor.getModel()!, "xml"); // TODO xml language is not part of the setup yet
            break;
        default: 
            console.error("Unknown format " + selectedFormat)
    }
}
const inputFormatSelect = <HTMLSelectElement>document.getElementById('in-format-select');
const outputFormatSelect = <HTMLSelectElement>document.getElementById('out-format-select');
setLanguageBasedOnFormatSelection(editorInput, inputFormatSelect)
setLanguageBasedOnFormatSelection(editorOutput, outputFormatSelect)

// Register listener
editorInput.getModel()!.onDidChangeContent(e => updateModel());
inputFormatSelect.onchange = e => {
    setLanguageBasedOnFormatSelection(editorInput, inputFormatSelect);
    updateModel();
}
outputFormatSelect.onchange = e => {
    setLanguageBasedOnFormatSelection(editorOutput, outputFormatSelect);
    updateModel();
}
const subPathLastSlashIndex = location.pathname.lastIndexOf('/')
const subPath = subPathLastSlashIndex > 0 
                    ? location.pathname.substr(0, subPathLastSlashIndex + 1)
                    : "";

const endpointInfoString = 
`Endpoint: /conversion

Headers:
    Method: POST
    Content-Type: text/plain

Parameters:
    inFormat:  Input graph format as provided as data.
    outFormat: Desired graph format to be returned by the server.

Example:
    curl -X POST "${location.protocol}//${location.host}/${subPath}conversion?inFormat=elkt&outFormat=json" \\
         -H 'Content-Type: text/plain' \\
         -d 'node n1 node n2 edge n1->n2'
`
const endpointInfoButton = <HTMLButtonElement>document.getElementById('http-endpoint-info')
endpointInfoButton.onclick = () => editorOutput.setValue(endpointInfoString);

function clearErrorMarkers() {
    monaco.editor.setModelMarkers(editorOutput.getModel(), "", []);
    monaco.editor.setModelMarkers(editorInput.getModel(), "", []);
}

function updateModel() {
    if (editorInput.getValue().trim() === "") {
        clearErrorMarkers();
        editorOutput.setValue("");
        return;
    }
    const inFormat = getSelection(inputFormatSelect);
    const outFormat = getSelection(outputFormatSelect);
    if (inFormat == outFormat) {
        clearErrorMarkers();
        editorOutput.setValue(editorInput.getValue());
        return;
    }

    const url = `${location.protocol}//${location.host}/${subPath}conversion`
    const query = `?inFormat=${inFormat}&outFormat=${outFormat}`
    fetch(`${url}${query}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'text/plain',
        },
        body: editorInput.getValue(),
    })
    .then(resp => {   
        if (resp.ok) {
            return resp.text();
        } else {
            // Forward any non-ok response to the 'catch' below
            return resp.text()
                       .then(msg => Promise.reject(msg));
        }
    })
    .then(data => {
        clearErrorMarkers();
        editorOutput.setValue(data);
    })
    .catch(error => {
        try {
            const serverError: ServerError = JSON.parse(error);
            const editor = serverError.type === "input" ? editorInput : editorOutput;
            if (editor === editorOutput) {
                editor.setValue(serverError.message);
            }

            let markers: monaco.editor.IMarkerData[] = [];
            if (serverError.diagnostics !== undefined) {
                markers = serverError.diagnostics.map(d => createErrorMarker(d));
            } else if (serverError.causes !== undefined) {
                markers = [createFullLengthErrorMarker(editor, serverError.causes.join("\n\n").replace(/\\n/g, "\n"))];
            } else {
                markers = [createFullLengthErrorMarker(editor, serverError.message)];
            }
            monaco.editor.setModelMarkers(editor.getModel(), "", markers);
        } catch (exception) {
            editorOutput.setValue(error);
            monaco.editor.setModelMarkers(editorOutput.getModel(), "", [createFullLengthErrorMarker(editorOutput, exception.message)]);
        }    
    });
}

class ServerError {
    message: string;
    type: string;
    diagnostics?: Diagnostic[];
    causes?: string[];

}
class Diagnostic {
    message: string;
    startLineNumber: number;
    endLineNumber?: number;
    startColumn: number;
    endColumn?: number;
}

function createFullLengthErrorMarker(editor: monaco.editor.IStandaloneCodeEditor, message?: string): monaco.editor.IMarkerData {
    const lineCount = editor.getModel().getLineCount();
    const endColumn = (lineCount == 0) ? 1 : editor.getModel().getLineLength(lineCount);
    return <monaco.editor.IMarkerData> {
        severity: monaco.MarkerSeverity.Error,
        startLineNumber: 1,
        endLineNumber: lineCount,
        startColumn: 1,
        endColumn: endColumn,
        message: message || "Conversion Failure."
    };
}

function createErrorMarker(diagnostic: Diagnostic): monaco.editor.IMarkerData {
    return <monaco.editor.IMarkerData> {
        severity: monaco.MarkerSeverity.Error,
        startLineNumber: diagnostic.startLineNumber,
        endLineNumber: diagnostic.endLineNumber,
        startColumn: diagnostic.startColumn,
        endColumn: diagnostic.endColumn,
        message: diagnostic.message,
    };
}
