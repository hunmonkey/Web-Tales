import {EpubWebPubAdapter} from '@/shelfview/adapters/epub-webpub-adapter'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'

const epubManifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'EPUB Book',
  kind: 'EPUB_WEBPUB',
  sourceFormat: 'EPUB',
  contentFingerprint: 'tales-file-state:book-1-epub',
  epub: {
    webpubManifestEndpoint: '/api/v1/books/book-1/manifest/epub',
    positionsEndpoint: '/api/v1/books/book-1/positions',
  },
  capabilities: {
    search: false,
    textSelection: true,
    doublePage: false,
    continuousScroll: true,
    rtl: true,
    imageFilters: false,
  },
}

const fixedPageManifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'Fixed Book',
  kind: 'FIXED_PAGE_SEQUENCE',
  sourceFormat: 'CBZ',
  contentFingerprint: 'tales-file-state:book-1',
  pageCount: 1,
  pages: {
    endpointTemplate: '/api/v1/books/book-1/pages/{page}?zero_based=true',
  },
  capabilities: {
    search: false,
    textSelection: false,
    doublePage: false,
    continuousScroll: false,
    rtl: false,
    imageFilters: true,
  },
}

describe('EpubWebPubAdapter', () => {
  it('opens an EPUB WebPub manifest and exposes the existing EPUB endpoints', async () => {
    const adapter = new EpubWebPubAdapter()

    await adapter.open(epubManifest)

    expect(adapter.getWebPubManifestEndpoint()).toBe('/api/v1/books/book-1/manifest/epub')
    expect(adapter.getPositionsEndpoint()).toBe('/api/v1/books/book-1/positions')

    adapter.destroy()

    expect(() => adapter.getWebPubManifestEndpoint()).toThrow('EpubWebPubAdapter is not open.')
  })

  it('rejects EPUB bridge manifests without required endpoints', async () => {
    const adapter = new EpubWebPubAdapter()

    await expect(adapter.open({
      ...epubManifest,
      epub: undefined,
    })).rejects.toThrow('EPUB WebPub manifest endpoints are missing.')
  })

  it('rejects non-EPUB bridge manifests', async () => {
    const adapter = new EpubWebPubAdapter()

    await expect(adapter.open(fixedPageManifest)).rejects.toThrow('EpubWebPubAdapter can only open EPUB WebPub manifests.')
  })
})
