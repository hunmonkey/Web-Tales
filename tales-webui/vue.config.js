// vue.config.js
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
