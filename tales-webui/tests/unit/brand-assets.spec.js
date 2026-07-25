const fs = require('fs')
const path = require('path')

const webuiRoot = path.resolve(__dirname, '../..')
const repositoryRoot = path.resolve(webuiRoot, '..')

describe('Tales brand assets', () => {
  const publicIcon = fs.readFileSync(path.join(webuiRoot, 'public/tales-icon.svg'), 'utf8')

  it('keeps the browser and desktop-package SVG icons identical', () => {
    const packageIcon = fs.readFileSync(path.join(repositoryRoot, 'res/tales-icon.svg'), 'utf8')

    expect(packageIcon).toBe(publicIcon)
  })

  it('uses the same core book silhouette in the in-app and public marks', () => {
    const logoComponent = fs.readFileSync(path.join(webuiRoot, 'src/components/LogoMark.vue'), 'utf8')
    const leftPage = 'M8 12C18 12 27 16 31 24C32.2 26.4 31.4 29.1 29 31.6C26.4 34.3 25 36.7 25 40V54C20 50.2 14.3 48.4 8 49V12Z'
    const rightPage = 'M56 12C46 12 37 16 33 24C31.8 26.4 32.6 29.1 35 31.6C37.6 34.3 39 36.7 39 40V54C44 50.2 49.7 48.4 56 49V12Z'

    expect(logoComponent).toContain(leftPage)
    expect(logoComponent).toContain(rightPage)
    expect(publicIcon).toContain(leftPage)
    expect(publicIcon).toContain(rightPage)
  })

  it('ships a square 1024px install icon and refreshes the offline cache', () => {
    const png = fs.readFileSync(path.join(webuiRoot, 'public/tales-icon-square.png'))
    const serviceWorker = fs.readFileSync(path.join(webuiRoot, 'public/shelfview-service-worker.js'), 'utf8')

    expect(png.toString('ascii', 1, 4)).toBe('PNG')
    expect(png.readUInt32BE(16)).toBe(1024)
    expect(png.readUInt32BE(20)).toBe(1024)
    expect(serviceWorker).toContain('const CACHE_NAME = \'tales-offline-v2\'')
  })
})
