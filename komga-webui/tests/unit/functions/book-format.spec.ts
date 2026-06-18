import {getBookReadRouteFromMedia, getLegacyBookReadRouteFromMedia} from '@/functions/book-format'
import {MediaDto} from '@/types/komga-books'

const divinaMedia: MediaDto = {
  status: 'READY',
  mediaType: 'application/zip',
  pagesCount: 2,
  comment: '',
  mediaProfile: 'DIVINA',
  epubDivinaCompatible: false,
  epubIsKepub: false,
}

const epubMedia: MediaDto = {
  status: 'READY',
  mediaType: 'application/epub+zip',
  pagesCount: 2,
  comment: '',
  mediaProfile: 'EPUB',
  epubDivinaCompatible: false,
  epubIsKepub: false,
}

describe('book read routes', () => {
  const originalFlag = process.env.VUE_APP_SHELFVIEW_READER

  afterEach(() => {
    process.env.VUE_APP_SHELFVIEW_READER = originalFlag
  })

  it('keeps the existing fixed-page reader by default', () => {
    delete process.env.VUE_APP_SHELFVIEW_READER

    expect(getBookReadRouteFromMedia(divinaMedia)).toBe('read-book')
  })

  it('routes fixed-page books to ShelfView when the flag is enabled', () => {
    process.env.VUE_APP_SHELFVIEW_READER = 'true'

    expect(getBookReadRouteFromMedia(divinaMedia)).toBe('read-book-v2')
  })

  it('keeps EPUB on the existing EPUB reader even when the flag is enabled', () => {
    process.env.VUE_APP_SHELFVIEW_READER = 'true'

    expect(getBookReadRouteFromMedia(epubMedia)).toBe('read-epub')
  })

  it('exposes the existing reader route for fallback links', () => {
    process.env.VUE_APP_SHELFVIEW_READER = 'true'

    expect(getLegacyBookReadRouteFromMedia(divinaMedia)).toBe('read-book')
  })
})
