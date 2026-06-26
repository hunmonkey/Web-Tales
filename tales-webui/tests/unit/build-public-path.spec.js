const config = require('../../vue.config.js')

describe('build public path', () => {
  it('loads app assets from the site root on history-mode refreshes', () => {
    expect(config.publicPath).toBe('/')
  })
})
