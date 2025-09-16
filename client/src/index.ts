/*******************************************************************************
 * Copyright (c) 2025 Kiel University and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

import { addDarkModeToggleObserver, setupDarkMode, updateLogoForTheme } from "./common/dark-mode";

setupDarkMode();

// Update dark theme logo
updateLogoForTheme();
addDarkModeToggleObserver(false);
console.log("Dark mode setup complete.");
