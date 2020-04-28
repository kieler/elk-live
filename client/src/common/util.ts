/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
