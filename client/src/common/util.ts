/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

export function groupBy<T>(iterable: T[], keyFun: (e) => string): { [key: string]: T[] } {
    return iterable.reduce((result, element) => {
        const key = keyFun(element);
        if (result[key] === undefined) {
            result[key] = [];
        }
        result[key].push(element);
        return result;
    }, {})
}
