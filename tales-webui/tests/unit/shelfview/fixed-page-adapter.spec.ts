import {FixedPageAdapter} from '@/shelfview/adapters/fixed-page-adapter'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {SHELFVIEW_LOCATOR_SCHEMA_VERSION} from '@/shelfview/models/shelfview-locator'

const manifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'Book One',
  kind: 'FIXED_PAGE_SEQUENCE',
  sourceFormat: 'CBZ',
  contentFingerprint: 'tales-file-state:book-1',
  pageCount: 3,
  pages: {
    endpointTemplate: '/api/v1/books/book-1/pages/{page}?zero_based=true',
  },
  capabilities: {
    search: false,
    textSelection: false,
    doublePage: true,
    continuousScroll: true,
    rtl: true,
    imageFilters: true,
  },
}

describe('FixedPageAdapter', () => {
  it('opens a fixed-page manifest at the first zero-based page', async () => {
    const {adapter, preloads} = createAdapter()

    await adapter.open(manifest)

    expect(adapter.getCurrentPageIndex()).toBe(0)
    expect(adapter.getCurrentPageUrl()).toBe('page-0')
    expect(adapter.getPageUrl(2)).toBe('page-2')
    expect(adapter.getPageUrl(3)).toBeUndefined()
    expect(adapter.getCurrentLocator()).toEqual({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'PAGE',
      page: 0,
      offset: 0,
      contentFingerprint: 'tales-file-state:book-1',
    })
    expect(adapter.getProgression()).toBe(1 / 3)
    expect(preloads.map(x => x.url)).toEqual(['page-1'])
  })

  it('moves next, previous and clamps locator page boundaries', async () => {
    const {adapter} = createAdapter()

    await adapter.open(manifest)

    await adapter.next()
    expect(adapter.getCurrentPageIndex()).toBe(1)

    await adapter.next()
    await adapter.next()
    expect(adapter.getCurrentPageIndex()).toBe(2)
    expect(adapter.canGoNext()).toBe(false)

    await adapter.previous()
    expect(adapter.getCurrentPageIndex()).toBe(1)

    await adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'PAGE',
      page: -20,
      offset: 0,
      contentFingerprint: manifest.contentFingerprint,
    })
    expect(adapter.getCurrentPageIndex()).toBe(0)

    await adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'PAGE',
      page: 20,
      offset: 0,
      contentFingerprint: manifest.contentFingerprint,
    })
    expect(adapter.getCurrentPageIndex()).toBe(2)
  })

  it('cancels stale prefetches when moving pages and destroying the adapter', async () => {
    const {adapter, preloads} = createAdapter()

    await adapter.open(manifest)
    const firstPrefetch = preloads[0]

    await adapter.next()
    expect(firstPrefetch.cancel).toHaveBeenCalled()
    expect(preloads.map(x => x.url)).toEqual(['page-1', 'page-2'])

    const secondPrefetch = preloads[1]
    adapter.destroy()

    expect(secondPrefetch.cancel).toHaveBeenCalled()
    expect(adapter.getProgression()).toBe(0)
    expect(adapter.getCurrentPageUrl()).toBeUndefined()
  })

  it('rejects non-page and stale locators', async () => {
    const {adapter} = createAdapter()

    await adapter.open(manifest)

    await expect(adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'TEXT',
      paragraph: 1,
      characterOffset: 0,
      progression: 0,
      normalizationVersion: 1,
      contentFingerprint: manifest.contentFingerprint,
    })).rejects.toThrow('FixedPageAdapter only accepts page locators.')

    await expect(adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'PAGE',
      page: 1,
      offset: 0,
      contentFingerprint: 'tales-file-state:old',
    })).rejects.toThrow('different book version')
  })

  it('does not expose a page URL for empty fixed-page manifests', async () => {
    const {adapter, preloads} = createAdapter()

    await adapter.open({...manifest, pageCount: 0})

    expect(adapter.getCurrentPageUrl()).toBeUndefined()
    expect(adapter.getProgression()).toBe(0)
    expect(preloads).toEqual([])
  })
})

function createAdapter() {
  const preloads: Array<{ url: string, cancel: jest.Mock }> = []
  const adapter = new FixedPageAdapter(
    (manifest, pageIndex) => `page-${pageIndex}`,
    url => {
      const preload = {
        url,
        cancel: jest.fn(),
      }
      preloads.push(preload)
      return preload
    },
  )

  return {adapter, preloads}
}
