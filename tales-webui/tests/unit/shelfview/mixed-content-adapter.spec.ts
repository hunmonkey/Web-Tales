import {MixedContentAdapter} from '@/shelfview/adapters/mixed-content-adapter'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {SHELFVIEW_LOCATOR_SCHEMA_VERSION, ShelfViewTextLocator} from '@/shelfview/models/shelfview-locator'
import {SHELFVIEW_TEXT_CONTENT_SCHEMA_VERSION, ShelfViewTextContent} from '@/shelfview/models/shelfview-text'

const manifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'Mixed Book',
  kind: 'MIXED_CONTENT_SEQUENCE',
  sourceFormat: 'ZIP',
  contentFingerprint: 'tales-file-state:book-1',
  pageCount: 3,
  items: [
    {
      itemId: 'image-item',
      index: 1,
      kind: 'FIXED_PAGE_SEQUENCE',
      label: '002.jpg',
      contentFingerprint: 'tales-file-state:book-1:image-item',
      sourceFormat: 'JPG',
      pageCount: 1,
      pages: {
        endpointTemplate: '/api/shelfview/v1/books/book-1/mixed/image-item/pages/{page}?zero_based=true',
      },
    },
    {
      itemId: 'text-item',
      index: 0,
      kind: 'TEXT_REFLOW',
      label: '001.txt',
      contentFingerprint: 'tales-file-state:book-1:text-item',
      sourceFormat: 'TXT',
      text: {
        endpoint: '/api/shelfview/v1/books/book-1/mixed/text-item/text',
        detectedEncoding: 'UTF-8',
        normalizationVersion: 1,
      },
    },
  ],
  capabilities: {
    search: false,
    textSelection: true,
    doublePage: true,
    continuousScroll: true,
    rtl: false,
    imageFilters: true,
  },
}

describe('MixedContentAdapter', () => {
  it('groups paragraphs into pages for a text item once a page budget is set', async () => {
    const adapter = createAdapter()

    await adapter.open(manifest)
    expect(adapter.getCurrentContentCount()).toBe(2)

    // A budget wide enough for both paragraphs turns the item into one page,
    // so paging leaves the item instead of stopping at every paragraph.
    adapter.setPagination(500)
    expect(adapter.getCurrentContentCount()).toBe(1)
    expect(adapter.getCurrentContentIndex()).toBe(0)

    await adapter.next()
    expect(adapter.isCurrentFixedPage()).toBe(true)

    await adapter.previous()
    expect(adapter.isCurrentText()).toBe(true)
    expect(adapter.getCurrentContentCount()).toBe(1)

    // Clearing the budget restores the original one-paragraph-per-page reading.
    adapter.setPagination(0)
    expect(adapter.getCurrentContentCount()).toBe(2)
  })

  it('opens mixed items in manifest order and moves from text to fixed page content', async () => {
    const adapter = createAdapter()

    await adapter.open(manifest)

    expect(adapter.isCurrentText()).toBe(true)
    expect(adapter.getCurrentContentCount()).toBe(2)
    expect(adapter.getVisibleParagraphs('SINGLE_PAGE')).toEqual([{paragraphIndex: 0, text: 'Intro'}])
    expect(adapter.getCurrentLocator()).toMatchObject({
      kind: 'MIXED',
      itemId: 'text-item',
      itemIndex: 0,
      contentFingerprint: manifest.contentFingerprint,
      itemLocator: {
        kind: 'TEXT',
        paragraph: 0,
        contentFingerprint: 'tales-file-state:book-1:text-item',
      },
    })

    await adapter.next()
    expect(adapter.getCurrentLocator()).toMatchObject({
      itemId: 'text-item',
      itemLocator: {
        kind: 'TEXT',
        paragraph: 1,
      },
    })

    await adapter.next()
    expect(adapter.isCurrentFixedPage()).toBe(true)
    expect(adapter.getCurrentContentCount()).toBe(1)
    expect(adapter.getPageUrl(0)).toBe('page-002.jpg-0')
    expect(adapter.getCurrentLocator()).toMatchObject({
      kind: 'MIXED',
      itemId: 'image-item',
      itemIndex: 1,
      itemLocator: {
        kind: 'PAGE',
        page: 0,
        contentFingerprint: 'tales-file-state:book-1:image-item',
      },
    })
  })

  it('restores a wrapped locator for a later item', async () => {
    const adapter = createAdapter()

    await adapter.open(manifest)
    await adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'MIXED',
      itemId: 'image-item',
      itemIndex: 1,
      itemLocator: {
        schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
        kind: 'PAGE',
        page: 0,
        offset: 0,
        contentFingerprint: 'tales-file-state:book-1:image-item',
      },
      contentFingerprint: manifest.contentFingerprint,
    })

    expect(adapter.isCurrentFixedPage()).toBe(true)
    expect(adapter.getCurrentLocator()).toMatchObject({
      itemId: 'image-item',
      itemIndex: 1,
    })
  })

  it('rejects stale top-level mixed locators', async () => {
    const adapter = createAdapter()

    await adapter.open(manifest)

    await expect(adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'MIXED',
      itemId: 'text-item',
      itemIndex: 0,
      itemLocator: textLocator(0),
      contentFingerprint: 'stale',
    })).rejects.toThrow('different book version')
  })
})

describe('MixedContentAdapter item loading', () => {
  // image, text, image: the text item in the middle is the one that downloads.
  const threeItems: ShelfViewManifest = {
    ...manifest,
    items: [
      {...manifest.items![0], itemId: 'image-a', index: 0},
      {...manifest.items![1], itemId: 'text-item', index: 1},
      {...manifest.items![0], itemId: 'image-b', index: 2},
    ],
  }

  it('moves one item when the page is turned again while the next item is still loading', async () => {
    const pending: Array<() => void> = []
    const adapter = new MixedContentAdapter(
      textManifest => new Promise(resolve => pending.push(() => resolve(textContent(textManifest.contentFingerprint)))),
      (itemManifest, pageIndex) => `page-${itemManifest.title}-${pageIndex}`,
      url => ({url, cancel: jest.fn()}),
    )
    await adapter.open(threeItems)

    const first = adapter.next()
    const second = adapter.next()
    pending.forEach(resolve => resolve())
    await Promise.all([first, second])

    expect(adapter.getCurrentLocator().itemId).toBe('text-item')
    expect(adapter.isCurrentText()).toBe(true)
  })

  it('keeps the current item readable when the next item fails to load', async () => {
    const adapter = new MixedContentAdapter(
      async () => {
        throw new Error('offline')
      },
      (itemManifest, pageIndex) => `page-${itemManifest.title}-${pageIndex}`,
      url => ({url, cancel: jest.fn()}),
    )
    await adapter.open(threeItems)

    await expect(adapter.next()).rejects.toThrow('offline')

    expect(adapter.getCurrentLocator().itemId).toBe('image-a')
    expect(adapter.canGoNext()).toBe(true)
  })
})

function createAdapter(): MixedContentAdapter {
  return new MixedContentAdapter(
    async textManifest => textContent(textManifest.contentFingerprint),
    (itemManifest, pageIndex) => `page-${itemManifest.title}-${pageIndex}`,
    url => ({
      url,
      cancel: jest.fn(),
    }),
  )
}

function textLocator(paragraph: number): ShelfViewTextLocator {
  return {
    schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
    kind: 'TEXT' as const,
    paragraph,
    characterOffset: 0,
    progression: paragraph / 2,
    normalizationVersion: 1,
    contentFingerprint: 'tales-file-state:book-1:text-item',
  }
}

function textContent(contentFingerprint: string): ShelfViewTextContent {
  return {
    schemaVersion: SHELFVIEW_TEXT_CONTENT_SCHEMA_VERSION,
    bookId: 'book-1',
    contentFingerprint,
    encoding: 'UTF-8',
    normalizationVersion: 1,
    paragraphs: ['Intro', 'Image follows'],
  }
}
