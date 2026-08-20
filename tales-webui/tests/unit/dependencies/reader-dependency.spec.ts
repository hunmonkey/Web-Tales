import fs from 'fs'
import path from 'path'

describe('EPUB reader dependency', () => {
  const readerRoot = path.dirname(require.resolve('@d-i-t-a/reader/package.json'))
  const readerPackage = JSON.parse(fs.readFileSync(path.join(readerRoot, 'package.json'), 'utf8'))
  const pdfjsPackage = JSON.parse(fs.readFileSync(require.resolve('pdfjs-dist/package.json', {paths: [readerRoot]}), 'utf8'))
  const readerBundle = fs.readFileSync(path.join(readerRoot, 'dist', 'esm', 'index.js'), 'utf8')

  it('uses the maintained reader release with the patched PDF.js bundle', () => {
    expect(readerPackage.version).toBe('2.5.3')
    expect(pdfjsPackage.version).toBe('5.5.207')
    expect(readerBundle).toContain('apiVersion: "5.5.207"')
    expect(readerBundle).not.toContain('2.14.305')
  })
})
