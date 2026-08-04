import {shelfViewEndpointUrl, shelfViewPageUrl, shelfViewThumbnailUrl} from '@/shelfview/functions/shelfview-urls'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'

const manifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'Book One',
  kind: 'FIXED_PAGE_SEQUENCE',
  sourceFormat: 'CBZ',
  contentFingerprint: 'tales-file-state:book-1',
  pageCount: 2,
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

describe('ShelfView URL helpers', () => {
  it('resolves relative page endpoint templates against the API origin', () => {
    // The version query splits the browser cache when a book file is replaced.
    expect(shelfViewPageUrl(manifest, 0, 'http://api.test')).toBe(
      'http://api.test/api/v1/books/book-1/pages/0?zero_based=true&v=tales-file-state%3Abook-1',
    )
  })

  it('keeps absolute endpoints unchanged', () => {
    expect(shelfViewEndpointUrl('https://cdn.test/page/0.jpg', 'http://api.test')).toBe('https://cdn.test/page/0.jpg')
  })

  it('returns undefined when the manifest has no page endpoint', () => {
    expect(shelfViewPageUrl({...manifest, pages: undefined}, 0, 'http://api.test')).toBeUndefined()
  })

  it('resolves page indexes without converting them to one-based numbers', () => {
    expect(shelfViewPageUrl(manifest, 1, 'http://api.test')).toBe(
      'http://api.test/api/v1/books/book-1/pages/1?zero_based=true&v=tales-file-state%3Abook-1',
    )
  })

  it('resolves thumbnail templates that need one-based page numbers', () => {
    expect(shelfViewThumbnailUrl({
      ...manifest,
      pages: {
        endpointTemplate: '/api/v1/books/book-1/pages/{page}?zero_based=true',
        thumbnailEndpointTemplate: '/api/v1/books/book-1/pages/{pageOneBased}/thumbnail',
      },
    }, 1, 'http://api.test')).toBe(
      'http://api.test/api/v1/books/book-1/pages/2/thumbnail?v=tales-file-state%3Abook-1',
    )
  })
})
