const path = require('path');
const CopyWebpackPlugin = require('copy-webpack-plugin');

const buildRoot = path.resolve(__dirname, 'lib');
const appRoot = path.resolve(__dirname, 'app');
const monacoEditorPath = 'node_modules/monaco-editor-core/dev/vs';
const bootstrapDistPath = 'node_modules/bootstrap/dist';
const sprottyCssPath = 'node_modules/sprotty/css';

module.exports = {
    entry: path.resolve(buildRoot, 'main.js'),
    output: {
        filename: 'bundle.js',
        path: appRoot
    },
    module: {
        noParse: /vscode-languageserver-types/
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
        new CopyWebpackPlugin([
            {
                from: monacoEditorPath,
                to: 'vs'
            }
        ]),
        new CopyWebpackPlugin([
            {
                from: bootstrapDistPath,
                to: 'bootstrap'
            }
        ]),
        new CopyWebpackPlugin([
            {
                from: sprottyCssPath,
                to: 'sprotty'
            }
        ])
    ]
}