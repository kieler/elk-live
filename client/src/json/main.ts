/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

window.onload = () => {
    // Load Monaco code
    const w = window as any;
    w.require(['vs/editor/editor.main'], () => {
        // Load application code
        require('./editor');
        document.getElementById("loading-editor")!.style.display = 'none';
    });
};
