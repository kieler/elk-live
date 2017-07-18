/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
const path = require('path');
const CopyWebpackPlugin = require('copy-webpack-plugin');

const buildRoot = path.resolve(__dirname, 'lib');
const appRoot = path.resolve(__dirname, 'app');
const monacoEditorPath = 'node_modules/monaco-editor-core/min/vs';
const bootstrapDistPath = 'node_modules/bootstrap/dist';
const jqueryDistPath = 'node_modules/jquery/dist';
const sprottyCssPath = 'node_modules/sprotty/css';
const elkWorkerPath = 'node_modules/elkjs/lib/elk-worker.min.js';

module.exports = function(env) {
    if (!env) {
        env = {}
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
        module: {
            noParse: /vscode-languageserver-types/,
            loaders: env.uglify ? [
                {
                    test: /.*\.js$/,
                    exclude: /vscode-base-languageclient/,
                    loader: 'uglify-loader'
                }
            ] : []
        },
        resolve: {
            extensions: ['.js'],
            alias: {
                'vs': path.resolve(__dirname, monacoEditorPath)
            }
        },
        devtool: 'source-map',
        target: 'web',
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