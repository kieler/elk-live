/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
const path = require('path');
const CopyWebpackPlugin = require('copy-webpack-plugin');

module.exports = function(env) {
    if (!env) {
        env = {}
    }

    const buildRoot = path.resolve(__dirname, 'lib');
    const appRoot = path.resolve(__dirname, 'app');
    const monacoEditorPath = env.production ? 'node_modules/monaco-editor-core/min/vs' : 'node_modules/monaco-editor-core/dev/vs';
    const bootstrapDistPath = 'node_modules/bootstrap/dist';
    const jqueryDistPath = 'node_modules/jquery/dist';
    const sprottyCssPath = 'node_modules/sprotty/css';
    const elkWorkerPath = 'node_modules/elkjs/lib/elk-worker.min.js';

    const rules = [
        {
            test: /node_modules[\\|/](vscode-languageserver-types|vscode-uri|jsonc-parser)/,
            use: { loader: 'umd-compat-loader' }
        }
    ];
    if (env.production) {
        rules.push({
            test: /.*\.js$/,
            exclude: /node_modules[\\|/](vscode-base-languageclient|vscode-languageserver-protocol|vscode-languageserver-types|vscode-uri|snabbdom)/,
            loader: 'uglify-loader'
        });
    } else {
        rules.push({
            test: /\.js$/,
            enforce: 'pre',
            loader: 'source-map-loader'
        });
    }

    return {
        entry: {
            elkgraph: path.resolve(buildRoot, 'elkgraph/main'),
            json: path.resolve(buildRoot, 'json/main'),
            models: path.resolve(buildRoot, 'models/main')
        },
        output: {
            filename: '[name].bundle.js',
            path: appRoot
        },
        target: 'web',
        module: { rules },
        resolve: {
            extensions: ['.js'],
            alias: {
                'vs': path.resolve(__dirname, monacoEditorPath),
                'vscode': require.resolve('monaco-languageclient/lib/vscode-compatibility')
            }
        },
        devtool: 'source-map',
        node: {
            fs: 'empty',
            child_process: 'empty',
            net: 'empty',
            crypto: 'empty'
        },
        plugins: [
            new CopyWebpackPlugin([{
                from: monacoEditorPath,
                to: 'vs'
            }]),
            new CopyWebpackPlugin([{
                from: bootstrapDistPath,
                to: 'bootstrap'
            }]),
            new CopyWebpackPlugin([{
                from: jqueryDistPath,
                to: 'jquery'
            }]),
            new CopyWebpackPlugin([{
                from: sprottyCssPath,
                to: 'sprotty'
            }]),
            new CopyWebpackPlugin([{
                from: elkWorkerPath,
                to: 'elk'
            }])
        ]
    }
}