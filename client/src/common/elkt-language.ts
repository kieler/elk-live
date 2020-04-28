/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

monaco.languages.register({
    id: 'elkt',
    extensions: ['.elkt']
});

monaco.languages.setLanguageConfiguration('elkt', {
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
    
monaco.languages.setMonarchTokensProvider('elkt', <any>{
    keywords: [
        'graph', 'node', 'label', 'port', 'edge', 'layout', 'position', 'size', 'incoming', 'outgoing',
        'start', 'end', 'bends', 'section', 'true', 'false'
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
