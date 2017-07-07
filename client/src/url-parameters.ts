/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import ICodeEditor = monaco.editor.ICodeEditor
import IModelContentChangedEvent = monaco.editor.IModelContentChangedEvent2

export function getParameterByName(name: string): string | undefined {
    const results = new RegExp('[?&]' + name.replace(/[\[\]]/g, '\\$&') + '(=([^&#]*)|&|#|$)').exec(window.location.href)
    if (!results || !results[2])
        return undefined
    else
        return decodeURIComponent(results[2].replace(/\+/g, ' '))
}

export function setupModelLink(editor: ICodeEditor, getParameters: (event: IModelContentChangedEvent)=>{[key: string]: string}) {
    let contentChangeTimeout: number
    const listener = event => {
        if (contentChangeTimeout !== undefined) {
            window.clearTimeout(contentChangeTimeout)
        }
        contentChangeTimeout = window.setTimeout(() => {
            const anchor = document.getElementById('model-link')
            if (anchor !== null) {
                let newHref = window.location.href
                const queryIndex = newHref.indexOf('?')
                if (queryIndex > 0)
                    newHref = newHref.substring(0, queryIndex)
                const parameters = getParameters(event)
                let i = 0
                for (let param in parameters) {
                    const value = encodeURIComponent(parameters[param])
                    newHref += `${i === 0 ? '?' : '&'}${param}=${value}`
                    i++
                }
                anchor.setAttribute('href', newHref)
            }
        }, 400)
    }
    editor.onDidChangeModelContent(listener)
    listener({})
}
