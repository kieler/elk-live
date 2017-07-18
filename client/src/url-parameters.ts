/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import ICodeEditor = monaco.editor.ICodeEditor
import IModelContentChangedEvent = monaco.editor.IModelContentChangedEvent2

export function getParameters(): {[key: string]: string} {
    let search = window.location.search.substring(1)
    const result = {}
    while (search.length > 0) {
        const nextParamIndex = search.indexOf('&')
        let param: string
        if (nextParamIndex < 0) {
            param = search
            search = ''
        } else {
            param = search.substring(0, nextParamIndex)
            search = search.substring(nextParamIndex + 1)
        }
        const valueIndex = param.indexOf('=')
        if (valueIndex > 0 && valueIndex < param.length - 1) {
            result[param.substring(0, valueIndex)] = param.substring(valueIndex + 1)
        }
    }
    return result
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
                    newHref += `${i === 0 ? '?' : '&'}${param}=${parameters[param]}`
                    i++
                }
                anchor.setAttribute('href', newHref)
            }
        }, 400)
    }
    editor.onDidChangeModelContent(listener)
    listener({})
}
