/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
const path = require('path');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
// to retrieve the explicit version numbers
const elkjsLatest = require('elkjs-latest/package.json');
const elkjsNext = require('elkjs-next/package.json');
const childProcess = require('child_process');
const fetch = require('node-fetch');
const fs = require('fs');
const globby = require('globby');

module.exports = async function (env) {
    if (!env) {
        env = {}
    }

    const buildRoot = path.resolve(__dirname, 'lib');
    const appRoot = path.resolve(__dirname, 'app');
    const monacoEditorPath = env.production ? 'node_modules/monaco-editor-core/min/vs' : 'node_modules/monaco-editor-core/dev/vs';
    const bootstrapDistPath = 'node_modules/bootstrap/dist';
    const jqueryDistPath = 'node_modules/jquery/dist';
    const autocompleteDistPath = 'node_modules/devbridge-autocomplete/dist';
    const sprottyCssPath = 'node_modules/sprotty/css';
    const elkWorkerPath3 = 'node_modules/elkjs-3/lib/elk-worker.min.js';
    const elkWorkerPath4 = 'node_modules/elkjs-4/lib/elk-worker.min.js';
    const elkWorkerPath5 = 'node_modules/elkjs-5/lib/elk-worker.min.js';
    const elkWorkerPath6 = 'node_modules/elkjs-6/lib/elk-worker.min.js';
    const elkWorkerPathLatest = 'node_modules/elkjs-latest/lib/elk-worker.min.js';
    const elkWorkerPathNext = 'node_modules/elkjs-next/lib/elk-worker.min.js';
    const currentGitCommit = childProcess.execSync('git rev-parse --short HEAD').toString().trim();

    const javaElkVersions = [ 'snapshot' ]; // latest snapshot/nightly at the time of building 
    // Query released ELK versions using maven's REST API
    try {
        const response = await fetch("https://search.maven.org/solrsearch/select?q=g:%22org.eclipse.elk%22+AND+a:%22org.eclipse.elk.core%22&core=gav&wt=json")
                                    .then(res => res.json());
        response.response.docs.forEach(doc => {
            javaElkVersions.push(doc.v);
        });
    } catch (error) {
        console.error("Unable to retrieve ELK releases, only the latest will be available (" + error.message + ").");
    }
    const javaElkVersionsOptions = javaElkVersions
                                     .map(version => `<option value="${version}">${version}</option>`)
                                     .join("");

    // Generate json files that hold the available models and examples
    const replaceLocalPaths = paths => paths.map(p => p.replace(`${appRoot}/elk-models/`, ''));
    const addQuotes = paths => paths.map(p => `"${p}"`);
    const models = await globby(`${appRoot}/elk-models/**/*.json`)
        .then(replaceLocalPaths)
        .then(addQuotes);
    fs.writeFileSync(`${buildRoot}/models/models.json`, `[ ${models.join(', ')} ]`);

    const examples = await globby(`${appRoot}/elk-models/examples/**/*.elkt`)
        .then(replaceLocalPaths)
        .then(addQuotes);
    fs.writeFileSync(`${buildRoot}/examples/examples.json`, `[ ${examples.join(', ')} ]`);

    const rules = [
        {
            test: /node_modules[\\|/](vscode-languageserver-types|vscode-uri|jsonc-parser)/,
            use: { loader: 'umd-compat-loader' }
        },
        {
            test: /\.elkt$/,
            use: { loader: path.resolve('./lib/examples/elkex-loader.js') }
        },
    ];
    if (env.production) {
        rules.push({
            test: /.*\.js$/,
            exclude: /node_modules[\\|/](vscode-base-languageclient|vscode-languageserver-protocol|vscode-languageserver-types|vscode-uri|snabbdom|reconnecting-websocket)/,
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
            conversion: path.resolve(buildRoot, 'conversion/main'),
            elkgraph: path.resolve(buildRoot, 'elkgraph/main'),
            examples: path.resolve(buildRoot, 'examples/main'),
            json: path.resolve(buildRoot, 'json/main'),
            models: path.resolve(buildRoot, 'models/main'),
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
            new HtmlWebpackPlugin({
                filename: 'elkgraph.html',
                template: 'src/elkgraph/elkgraph_template.html',
                inject: false,
                layoutOptionVersions: javaElkVersionsOptions,
                currentGitCommit: currentGitCommit,
            }),
            new HtmlWebpackPlugin({
                filename: 'examples.html',
                template: 'src/examples/examples_template.html',
                inject: false,
                layoutOptionVersions: javaElkVersionsOptions,
                currentGitCommit: currentGitCommit,
            }),
            new HtmlWebpackPlugin({
                filename: 'json.html',
                template: 'src/json/json_template.html',
                inject: false,
                nextVersion: elkjsNext.version,
                latestVersion: elkjsLatest.version,
                currentGitCommit: currentGitCommit,
            }),
            new HtmlWebpackPlugin({
                filename: 'models.html',
                template: 'src/models/models_template.html',
                inject: false,
                elkjsVersion: elkjsLatest.version,
                currentGitCommit: currentGitCommit,
            }),
            new HtmlWebpackPlugin({
                filename: 'conversion.html',
                template: 'src/conversion/conversion_template.html',
                inject: false,
                currentGitCommit: currentGitCommit,
            }),
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
                from: autocompleteDistPath,
                to: 'jquery-autocomplete'
            }]),
            new CopyWebpackPlugin([{
                from: sprottyCssPath,
                to: 'sprotty'
            }]),
            new CopyWebpackPlugin([{
                from: elkWorkerPath3,
                to: 'elk-3'
            }]),
            new CopyWebpackPlugin([{
                from: elkWorkerPath4,
                to: 'elk-4'
            }]),
            new CopyWebpackPlugin([{
                from: elkWorkerPath5,
                to: 'elk-5'
            }]),
            new CopyWebpackPlugin([{
                from: elkWorkerPath6,
                to: 'elk-6'
            }]),
            new CopyWebpackPlugin([{
                from: elkWorkerPathLatest,
                to: 'elk-latest'
            }]),
            new CopyWebpackPlugin([{
                from: elkWorkerPathNext,
                to: 'elk-next'
            }])
        ]
    }
}