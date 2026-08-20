export function epubReaderStyleInjectables(): object[] {
  // Webpack discovers these resource files from the literal URL expressions.
  // The non-css extension prevents css-loader from consuming reader-injected styles.
  return [
    {
      type: 'style',
      url: new URL('../styles/readium/ReadiumCSS-before.css.resource', import.meta.url).toString(),
      r2before: true,
    },
    {
      type: 'style',
      url: new URL('../styles/readium/ReadiumCSS-default.css.resource', import.meta.url).toString(),
      r2default: true,
    },
    {
      type: 'style',
      url: new URL('../styles/readium/ReadiumCSS-after.css.resource', import.meta.url).toString(),
      r2after: true,
    },
    {type: 'style', url: new URL('../styles/r2d2bc/popup.css.resource', import.meta.url).toString()},
    {type: 'style', url: new URL('../styles/r2d2bc/popover.css.resource', import.meta.url).toString()},
    {type: 'style', url: new URL('../styles/r2d2bc/style.css.resource', import.meta.url).toString()},
  ]
}
