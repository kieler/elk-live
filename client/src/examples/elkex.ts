/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import { groupBy } from "../common/util";

export class ElkExample {

    static readonly KEYS: Set<string> = new Set(["graph", "doc", "category", "label"])

    readonly path: string;
    label: string;
    graph: string;
    doc: string;
    category: string[];

    constructor(path: string, serialized: string) {
        this.path = path;

        const sections = serialized.split(/\/\/\s*elkex:/)
        sections.map(s => s.trim())
            .filter(s => ElkExample.KEYS.has(this.firstWord(s)))
            .map(s => [this.firstWord(s), this.content(s)])
            .forEach(pair => {
                const key = pair[0].trim()
                let value = pair[1];
                if (key !== "graph") {
                    value = this.removeComments(value);
                }
                value = value.trim();
                switch (key) {
                    case "category":
                        this[key] = value.split('>').map(s => s.trim());
                        break;
                    default:
                        this[key] = value;
                }
            })

        const missingKeys = Array.from(ElkExample.KEYS)
            .map(k => this[k] === undefined ? k : undefined)
            .filter(e => e !== undefined);
        if (missingKeys.length > 0) {
            throw new Error(`Example specification '${path}' is missing the following elements: ${missingKeys.join(', ')}`);
        }
    }

    private removeComments(s: string): string {
        return s.replace(/^( |\t)*\/\//gm, '')
            .replace(/\/\*/g, '')
            .replace(/\*\//g, '')
    }

    private firstWord(s: string): string {
        return s.substr(0, this.firstRealWhitespaceOrSymbol(s))
    }

    private content(s: string): string {
        return s.substr(this.firstNewlineSymbol(s) + 1, s.length)
    }

    private firstRealWhitespaceOrSymbol(s: string): number {
        return s.search(/(\s|\\r|\\n)/);
    }

    private firstNewlineSymbol(s: string): number {
        return s.search(/(\r|\n|\\r|\\n)/);
    }

}

export interface ExampleCategory {
    name: string;
    elements: ElkExample[];
    subCategories: ExampleCategory[];
}

export function createExampleCategoryTree(examples: ElkExample[], depth: number = 0, name: string = 'root'): ExampleCategory {
    const grouped = groupBy(examples, e => e.category[depth] || 'current')
    const subCategories = Object.keys(grouped)
        .filter(k => k !== 'current')
        .map(k => createExampleCategoryTree(grouped[k], depth + 1, k))
    const elements = grouped['current']

    return { name, elements, subCategories }
}
