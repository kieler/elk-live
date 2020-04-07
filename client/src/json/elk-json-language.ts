/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

monaco.languages.register({
    id: 'json',
    extensions: ['.json']
});

monaco.languages.setLanguageConfiguration('json', {
    comments: {
        lineComment: "//",
        blockComment: ['/*', '*/']
    },
    brackets: [['{', '}'], ['[', ']']],
    autoClosingPairs: [
        {
            open: '{',
            close: '}'
        },
        {
            open: '[',
            close: ']'
        }]
});
    
monaco.languages.setMonarchTokensProvider('json', <any>{
    keywords: [
        'id', 'children', 'edges', 'ports', 'labels',
        'source', 'sources', 'target', 'targets',
        'text',
        'x', 'y', 'width', 'height',
        'layoutOptions', 'properties',
    ],

    typeKeywords: [],

    operators: [],

    symbols: /[=><!~?:&|+\-*\/\^%]+/,
    escapes: /\\(?:[btnfru\\"']|x[0-9A-Fa-f]{1,4}|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8})/,

    tokenizer: {
        root: [
            // Identifiers and keywords
            [/[a-z_$][\w$]*/, {
                cases: {
                    '@typeKeywords': 'keyword',
                    '@keywords': 'keyword',
                    '@default': 'identifier'
                }
            }],

            // Whitespace
            { include: '@whitespace' },

            // Delimiters and operators
            [/[{}()\[\]]/, '@brackets'],
            [/[<>](?!@symbols)/, '@brackets'],
            [/@symbols/, {
                cases: {
                    '@operators': 'operator',
                    '@default': ''
                }
            }]
        ],

        whitespace: [
            [/[ \t\r\n]+/, 'white'],
            [/\/\*/, 'comment', '@comment'],
            [/\/\/.*$/, 'comment'],
        ],

        comment: [
            [/[^\/*]+/, 'comment'],
            [/\/\*/, 'comment.invalid'],
            ["\\*/", 'comment', '@pop'],
            [/[\/*]/, 'comment']
        ],

        string: [
            [/[^\\"]+/, 'string'],
            [/@escapes/, 'string.escape'],
            [/\\./, 'string.escape.invalid'],
            [/"/, 'string', '@pop']
        ],
    },
});
