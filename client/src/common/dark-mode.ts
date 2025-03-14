/*******************************************************************************
 * Copyright (c) 2025 Kiel University and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

import { setupDarkMode as setup } from "simple-dark-mode-toggle";

export function setupDarkMode() {
    const darkModeToggle = <HTMLElement>document.getElementById('darkModeToggle');
    setup(darkModeToggle, {
        darkClassName: 'elk-dark-mode',
        storageName: 'elk-live-dark-mode',
        darkByDefault: false,
    });
}
