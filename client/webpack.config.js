module.exports = {
    entry: './src/app.ts',
    devtool: 'source-map',
    output: {
        filename: 'app/bundle.js',
    },
    resolve: {
        extensions: ['.webpack.js', '.web.js', '.ts', '.tsx', '.js']
    },
    module: {
        loaders: [
            { test: /\.tsx?$/, loader: 'ts-loader' }
        ]
    },
    node : { fs: 'empty', net: 'empty' }
};
