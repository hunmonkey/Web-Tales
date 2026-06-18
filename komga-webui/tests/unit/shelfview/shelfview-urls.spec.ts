import {shelfViewEndpointUrl, shelfViewPageUrl} from '@/shelfview/functions/shelfview-urls'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'

const manifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'Book One',
  kind: 'FIXED_PAGE_SEQUENCE',
  sourceFormat: 'CBZ',
  contentFingerprint: 'komga-file-state:book-1',
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
    expect(shelfViewPageUrl(manifest, 0, 'http://api.test')).toBe(
      'http://api.test/api/v1/books/book-1/pages/0?zero_based=true',
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
      'http://api.test/api/v1/books/book-1/pages/1?zero_based=true',
    )
  })
})
