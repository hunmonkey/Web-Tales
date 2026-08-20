// Strips the Material Design Icons glyph rules the application never asks for.
//
// `@mdi/font` ships 7,296 icons; this application names 130 of them. The unused
// rules measured 329 KB of the 612 KB vendor stylesheet, and 51 KB of its 83 KB
// gzipped - stylesheets block the first paint, so that is time on screen, not
// just bandwidth.
//
// Written as a loader rather than a generated file that gets committed, because
// a generated file goes stale the moment someone adds an icon and the icon then
// silently renders as an empty box. Here the icon list is rebuilt from the
// sources on every compile.
//
// The webfont itself is untouched: subsetting a woff2 needs a font tool, which
// would be a new dependency.
const fs = require('fs')
const path = require('path')

const SOURCE_ROOT = path.resolve(__dirname, '../src')

// Vuetify's own components ask for 33 icons by name at runtime - the checkbox
// tick, the pagination chevrons, the select caret. None of those names appear
// in src/, so scanning only the application would delete them.
const VUETIFY_MDI_PRESET = path.resolve(
  __dirname,
  '../node_modules/vuetify/lib/services/icons/presets/mdi.js',
)

const SCANNED_EXTENSIONS = ['.vue', '.ts', '.js', '.css', '.sass', '.scss', '.html']
const ICON_NAME = /mdi-[a-z0-9-]+/g

// Some icon names are assembled at runtime - `mdi-chevron-{{ open ? 'up' : 'down' }}`
// in ReadMore.vue is the one that exists today. Scanning for whole names sees
// only the `mdi-chevron-` stem and would drop both real icons, which then render
// as empty boxes. Anything whose stem runs straight into an interpolation is
// therefore treated as a prefix: every icon starting with it is kept. Keeping a
// few icons too many is the safe direction to be wrong in.
const DYNAMIC_ICON_STEM = /mdi-[a-z0-9-]*-(?=\{\{|\$\{|['"]\s*\+)/g

// Only a glyph rule looks like this. The helper classes that share the prefix
// (`.mdi-spin:before`, `.mdi-rotate-45`, `.mdi-18px`) use a single colon or no
// pseudo-element at all, and none of them declare `content`, so this pattern
// cannot reach them.
const GLYPH_RULE = /\.(mdi-[a-z0-9-]+)::before\s*\{\s*content:\s*"\\[0-9A-Fa-f]+";\s*\}\s*/g

function collectSourceFiles(directory, found) {
  for (const entry of fs.readdirSync(directory, {withFileTypes: true})) {
    const full = path.join(directory, entry.name)
    if (entry.isDirectory()) collectSourceFiles(full, found)
    else if (SCANNED_EXTENSIONS.includes(path.extname(entry.name))) found.push(full)
  }
  return found
}

module.exports = function mdiSubsetLoader(source) {
  // Re-run when a source file changes or a new one appears, so watch mode does
  // not serve a subset built from an older icon list.
  this.addContextDependency(SOURCE_ROOT)
  this.addDependency(VUETIFY_MDI_PRESET)

  const used = new Set()
  const stems = new Set()
  for (const file of [...collectSourceFiles(SOURCE_ROOT, []), VUETIFY_MDI_PRESET]) {
    const text = fs.readFileSync(file, 'utf8')
    let match
    while ((match = ICON_NAME.exec(text)) !== null) used.add(match[0])
    while ((match = DYNAMIC_ICON_STEM.exec(text)) !== null) stems.add(match[0])
  }

  const isUsed = name => used.has(name) || [...stems].some(stem => name.startsWith(stem))

  let kept = 0
  let dropped = 0
  const subset = source.replace(GLYPH_RULE, (rule, name) => {
    if (isUsed(name)) {
      kept += 1
      return rule
    }
    dropped += 1
    return ''
  })

  if (kept === 0) {
    // Every icon gone means the scan failed, not that the app uses none.
    this.emitError(new Error('mdi-subset-loader matched no used icons; refusing to ship an empty icon set'))
    return source
  }

  console.log(`\nmdi-subset-loader: kept ${kept} icon rules, dropped ${dropped}`)
  return subset
}
