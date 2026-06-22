import {MediaDto} from '@/types/tales-books'

jest.mock('@/types/items', () => ({
  ItemTypes: {
    BOOK: 'BOOK',
  },
}))
jest.mock('js-file-downloader', () => jest.fn())
jest.mock('screenfull', () => ({
  isEnabled: false,
  isFullscreen: false,
  request: jest.fn(),
  exit: jest.fn(),
  on: jest.fn(),
  off: jest.fn(),
}))

import DivinaReader from '@/views/DivinaReader.vue'

const redirectToShelfViewIfNeeded = (DivinaReader as any).options.methods.redirectToShelfViewIfNeeded

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

describe('DivinaReader ShelfView redirect', () => {
  const originalFlag = process.env.VUE_APP_SHELFVIEW_READER

  afterEach(() => {
    process.env.VUE_APP_SHELFVIEW_READER = originalFlag
  })

  it('redirects supported fixed-page books to ShelfView when the flag is enabled', () => {
    process.env.VUE_APP_SHELFVIEW_READER = 'true'
    const replace = jest.fn()

    const redirected = redirectToShelfViewIfNeeded.call({
      bookId: 'book-1',
      book: {media: divinaMedia},
      $route: {query: {page: '3', context: 'series', contextId: 'series-1'}},
      $router: {replace},
    })

    expect(redirected).toBe(true)
    expect(replace).toHaveBeenCalledWith({
      name: 'read-book-v2',
      params: {bookId: 'book-1'},
      query: {page: '3', context: 'series', contextId: 'series-1'},
    })
  })

  it('keeps the legacy reader when forceLegacy is true', () => {
    process.env.VUE_APP_SHELFVIEW_READER = 'true'
    const replace = jest.fn()

    const redirected = redirectToShelfViewIfNeeded.call({
      bookId: 'book-1',
      book: {media: divinaMedia},
      $route: {query: {forceLegacy: 'true'}},
      $router: {replace},
    })

    expect(redirected).toBe(false)
    expect(replace).not.toHaveBeenCalled()
  })

  it('keeps EPUB on the existing reader path', () => {
    process.env.VUE_APP_SHELFVIEW_READER = 'true'
    const replace = jest.fn()

    const redirected = redirectToShelfViewIfNeeded.call({
      bookId: 'book-1',
      book: {media: epubMedia},
      $route: {query: {}},
      $router: {replace},
    })

    expect(redirected).toBe(false)
    expect(replace).not.toHaveBeenCalled()
  })
})
