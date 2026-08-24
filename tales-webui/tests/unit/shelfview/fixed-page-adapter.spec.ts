import {FixedPageAdapter, defaultShelfViewPagePreloader} from '@/shelfview/adapters/fixed-page-adapter'
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
    // The current page leads the window: it is the one the reader is waiting for.
    expect(preloads.map(x => x.url)).toEqual(['page-0', 'page-1', 'page-2'])
  })

  it('prefetches two double page spreads ahead and stops at the last page', async () => {
    const {adapter, preloads} = createAdapter()

    await adapter.open({...manifest, pageCount: 6})
    expect(preloads.map(x => x.url)).toEqual(['page-0', 'page-1', 'page-2', 'page-3', 'page-4'])

    await adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'PAGE',
      page: 2,
      offset: 0,
      contentFingerprint: manifest.contentFingerprint,
    })
    // Pages 3 and 4 are already loading and stay; only what entered the window
    // is requested, so a page turn no longer restarts downloads in flight.
    expect(preloads.slice(5).map(x => x.url)).toEqual(['page-5'])

    await adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'PAGE',
      page: 5,
      offset: 0,
      contentFingerprint: manifest.contentFingerprint,
    })
    expect(preloads.slice(6).map(x => x.url)).toEqual([])
  })

  it('keeps the previous page prefetched so turning back is not a blank screen', async () => {
    const {adapter, preloads} = createAdapter()

    await adapter.open({...manifest, pageCount: 6})
    await adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'PAGE',
      page: 3,
      offset: 0,
      contentFingerprint: manifest.contentFingerprint,
    })

    expect(preloads.filter(x => x.url === 'page-2').length).toBe(1)
    expect(preloads.find(x => x.url === 'page-2')!.cancel).not.toHaveBeenCalled()
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

  it('keeps the page it moves to loading, and cancels only what leaves the window', async () => {
    const {adapter, preloads} = createAdapter()

    await adapter.open({...manifest, pageCount: 6})
    const pageBeingTurnedTo = preloads.find(x => x.url === 'page-1')!

    await adapter.next()
    // Arriving at page 1 must not cancel page 1. Cancelling aborts a download
    // still in flight, and this is the page the reader is about to show, so the
    // turn used to throw away the very page it was waiting for.
    expect(pageBeingTurnedTo.cancel).not.toHaveBeenCalled()
    expect(preloads.map(x => x.url)).toEqual(['page-0', 'page-1', 'page-2', 'page-3', 'page-4', 'page-5'])

    await adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'PAGE',
      page: 5,
      offset: 0,
      contentFingerprint: manifest.contentFingerprint,
    })
    // Only the last page and the one behind it are still in the window.
    expect(preloads.filter(x => x.cancel.mock.calls.length > 0).map(x => x.url))
      .toEqual(['page-0', 'page-1', 'page-2', 'page-3'])

    adapter.destroy()

    expect(preloads.find(x => x.url === 'page-5')!.cancel).toHaveBeenCalled()
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

describe('defaultShelfViewPagePreloader', () => {
  const originalImage = global.Image
  let image: MockImage | undefined

  beforeEach(() => {
    global.Image = jest.fn(() => {
      image = new MockImage()
      return image as unknown as HTMLImageElement
    }) as unknown as typeof Image
  })

  afterEach(() => {
    global.Image = originalImage
    image = undefined
  })

  it('prefetches a same-origin page in the same fetch mode the reader displays it in', () => {
    // jsdom serves the test document from http://localhost.
    defaultShelfViewPagePreloader('/api/v1/books/book-1/pages/0')

    expect(image?.crossOrigin).toBeNull()
    expect(image?.src).toBe('/api/v1/books/book-1/pages/0')
  })

  it('prefetches a cross-origin page with credentials so the canvas can read it', () => {
    defaultShelfViewPagePreloader('http://api.test/api/v1/books/book-1/pages/0')

    expect(image?.crossOrigin).toBe('use-credentials')
  })

  it('stops a cancelled prefetch without pointing the image at the document', () => {
    const preload = defaultShelfViewPagePreloader('/api/v1/books/book-1/pages/0')

    preload.cancel()

    expect(image?.src).toBe('')
  })
})

class MockImage {
  onload: (() => void) | null = null
  onerror: (() => void) | null = null
  crossOrigin: string | null = null
  private imageSrc = ''

  set src(value: string) {
    this.imageSrc = value
  }

  get src(): string {
    return this.imageSrc
  }

  removeAttribute(name: string) {
    if (name === 'src') this.imageSrc = ''
  }
}

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
