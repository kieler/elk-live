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

/**
 * Finds the elk-logo element and updates its source based on the current theme.
 */
export function updateSmallLogoForTheme() {
    const logo = document.getElementById('elk-logo');
    if (!logo || !(logo instanceof HTMLImageElement)) return;
    const toggle = document.querySelector('#darkModeToggle');
    // Check whether data-dark attribute is set
    const isDarkMode = toggle?.getAttribute('data-dark') === '1';
    // Update logo based on dark mode status
    logo.src = isDarkMode ? "img/elk_small_light.svg" : "img/elk_small.svg";
}

/**
 * Finds the elk-logo element and updates its source based on the current theme.
 */
export function updateLogoForTheme() {
    const logo = document.getElementById('elk-logo');
    if (!logo || !(logo instanceof HTMLImageElement)) return;
    const toggle = document.querySelector('#darkModeToggle');
    // Check whether data-dark attribute is set
    const isDarkMode = toggle?.getAttribute('data-dark') === '1';
    // Update logo based on dark mode status
    logo.src = isDarkMode ? "img/elk_light.svg" : "img/elk.svg";
}

/**
 * Updates the logo when the theme changes by observing changes to the body class.
 */
export function addDarkModeToggleObserver(small: boolean) {
    // Listen for class changes on body
    const observer = new MutationObserver(small? updateSmallLogoForTheme : updateLogoForTheme);
    observer.observe(document.body, { attributes: true, attributeFilter: ['class'] });
    // Initial set
    document.addEventListener('DOMContentLoaded', small? updateSmallLogoForTheme : updateLogoForTheme);
}
