// vue.config.js
const path = require('path')

module.exports = {
  // Root-relative assets keep history-mode routes like /read-v2/:bookId refreshable.
  publicPath: '/',

  pluginOptions: {
    i18n: {
      locale: 'en',
      fallbackLocale: 'en',
      localeDir: 'locales',
      enableInSFC: false,
    },
  },

  devServer: {
    allowedHosts: 'all',
    historyApiFallback: true,
    client: {
      webSocketURL: 'ws://0.0.0.0:8081/ws',
    },
  },

  // SV-700: the full fork-ts-checker type-check is too slow on the small CI
  // runner. When SHELFVIEW_SKIP_TSCHECK=true (set only in CI build), drop the
  // type-check plugin so CI verifies bundling fast. Local dev/build keep it.
  chainWebpack: config => {
    if (process.env.SHELFVIEW_SKIP_TSCHECK === 'true' && config.plugins.has('fork-ts-checker')) {
      config.plugins.delete('fork-ts-checker')
    }

    // Trim the Material Design Icons stylesheet to the icons that are actually
    // named. A `pre` loader sees the file before css-loader does, so the rules
    // are gone before anything else parses them. See webpack/mdi-subset-loader.js
    // for why this is a loader and not a checked-in generated stylesheet.
    config.module
      .rule('mdi-subset')
      .test(/materialdesignicons\.css$/)
      .pre()
      .use('mdi-subset-loader')
      .loader(path.resolve(__dirname, 'webpack/mdi-subset-loader.js'))

    // The type check dominates build time here: measured 19 min with it and
    // 56 s without, on the same tree. Worse, at the plugin's 2048 MB default it
    // runs out of memory partway through and gives up, and the build still
    // prints "Build complete" — so a green build says nothing about types.
    // SHELFVIEW_TSCHECK_MEMORY raises the limit so the check can actually
    // finish; 8192 was enough for the current tree, 16384 for a larger one.
    // Note it then reports the repo's pre-existing type errors and fails the
    // build (see SV-1250).
    if (process.env.SHELFVIEW_TSCHECK_MEMORY && config.plugins.has('fork-ts-checker')) {
      config.plugin('fork-ts-checker').tap(args => {
        args[0] = args[0] || {}
        args[0].typescript = Object.assign({}, args[0].typescript, {
          memoryLimit: parseInt(process.env.SHELFVIEW_TSCHECK_MEMORY, 10),
        })
        return args
      })
    }
  },

  // custom rule for readium and r2d2bc css that needs to be made available, but untouched
  configureWebpack: {
    module: {
      rules: [
        {
          test: [
            /readium\/.*\.css.resource$/,
            /r2d2bc\/.*\.css.resource$/,
          ],
          type: 'asset/resource',
          generator: {
            filename: 'css/[hash].css[query]',
          },
        },
      ],
    },
  },
}
