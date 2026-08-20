import {oauth2ProviderIcon} from '@/types/social'

const mdiSubsetLoader = require('../../../webpack/mdi-subset-loader.js') as (this: any, source: string) => string

describe('MDI subset OAuth icons', () => {
  it('maps known providers and uses a stable icon for custom providers', () => {
    expect(oauth2ProviderIcon('Google')).toBe('mdi-google')
    expect(oauth2ProviderIcon('facebook')).toBe('mdi-facebook')
    expect(oauth2ProviderIcon('GITHUB')).toBe('mdi-github')
    expect(oauth2ProviderIcon('company-oidc')).toBe('mdi-login-variant')
  })

  it('keeps all mapped OAuth icons in the generated stylesheet', () => {
    const source = [
      '.mdi-google::before { content: "\\F0001"; }',
      '.mdi-facebook::before { content: "\\F0002"; }',
      '.mdi-github::before { content: "\\F0003"; }',
      '.mdi-login-variant::before { content: "\\F0004"; }',
      '.mdi-definitely-unused-sv-test::before { content: "\\F0005"; }',
    ].join('\n')
    const context = {
      addContextDependency: jest.fn(),
      addDependency: jest.fn(),
      emitError: jest.fn(),
    }
    const log = jest.spyOn(console, 'log').mockImplementation()

    const subset = mdiSubsetLoader.call(context, source)

    log.mockRestore()
    expect(context.emitError).not.toHaveBeenCalled()
    expect(subset).toContain('.mdi-google::before')
    expect(subset).toContain('.mdi-facebook::before')
    expect(subset).toContain('.mdi-github::before')
    expect(subset).toContain('.mdi-login-variant::before')
    expect(subset).not.toContain('.mdi-definitely-unused-sv-test::before')
  })
})
