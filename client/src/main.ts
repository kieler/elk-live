/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

window.onload = () => {
    const w = window as any
    // Load Monaco code
    w.require(['vs/editor/editor.main'], () => {
        // Load application code
        const editor = document.getElementById('monaco-editor')
        if (editor !== null) {
            const inputType = editor.getAttribute('data-input-type')
            if (inputType === 'elkt')
                require('./elkgraph-client')
            else if (inputType == 'json')
                require('./json-client')
        }
    })
}
