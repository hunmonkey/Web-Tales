import {MediaDto} from '@/types/tales-books'

const mockD2ReaderLoad = jest.fn()

jest.mock('@d-i-t-a/reader', () => ({
  __esModule: true,
  default: {
    load: (...args: unknown[]) => mockD2ReaderLoad(...args),
  },
}))
jest.mock('screenfull', () => ({
  isEnabled: false,
  isFullscreen: false,
  request: jest.fn(),
  exit: jest.fn(),
  on: jest.fn(),
  off: jest.fn(),
}))
jest.mock('@/functions/epub-reader-assets', () => ({
  epubReaderStyleInjectables: jest.fn(() => []),
}))

import EpubReader from '@/views/EpubReader.vue'

const setup = (EpubReader as any).options.methods.setup
const beforeDestroy = (EpubReader as any).options.beforeDestroy[0]

const epubMedia: MediaDto = {
  status: 'READY',
  mediaType: 'application/epub+zip',
  pagesCount: 2,
  comment: '',
  mediaProfile: 'EPUB',
  epubDivinaCompatible: false,
}

describe('EpubReader D2 initialization', () => {
  beforeEach(() => {
    mockD2ReaderLoad.mockReset()
    mockD2ReaderLoad.mockResolvedValue({
      publicationLayout: 'reflowable',
      tableOfContents: [],
      landmarks: [],
      pageList: [],
    })
  })

  it('keeps EPUB on the legacy reader with isolated in-memory storage', async () => {
    const context = {
      bookId: 'book-1',
      book: undefined,
      series: undefined,
      settings: {appearance: 'readium-default-on'},
      fontFamiliesAdditional: [],
      tocs: {},
      fixedLayout: false,
      alwaysFullscreen: false,
      siblingNext: {},
      siblingPrevious: {},
      context: {},
      incognito: false,
      bookTitle: 'EPUB title',
      redirectUnsupportedMediaIfNeeded: jest.fn(() => false),
      enterFullscreen: jest.fn(),
      updateCurrentLocation: jest.fn(),
      keyPressed: jest.fn(),
      clickThrough: jest.fn(),
      updatePositionInfo: jest.fn(),
      updateChapterInfo: jest.fn(),
      updateDirection: jest.fn(),
      $route: {query: {}},
      $talesBooks: {
        getBook: jest.fn().mockResolvedValue({
          id: 'book-1',
          seriesId: 'series-1',
          media: epubMedia,
        }),
        getProgression: jest.fn().mockResolvedValue(undefined),
        getBookSiblingNext: jest.fn().mockRejectedValue(new Error('no sibling')),
        getBookSiblingPrevious: jest.fn().mockRejectedValue(new Error('no sibling')),
      },
      $talesSeries: {
        getOneSeries: jest.fn().mockResolvedValue({id: 'series-1'}),
      },
    }

    await setup.call(context, 'book-1')

    expect(context.redirectUnsupportedMediaIfNeeded).toHaveBeenCalled()
    expect(mockD2ReaderLoad).toHaveBeenCalledTimes(1)
    const config = mockD2ReaderLoad.mock.calls[0][0]
    expect(config).toEqual(expect.objectContaining({
      useStorageType: 'memory',
      requestConfig: {credentials: 'include'},
    }))
    expect(config).not.toHaveProperty('storageType')
    expect(config.url.toString()).toContain('/api/v1/books/book-1/manifest')
  })

  it('can be destroyed after a media fallback before D2 is loaded', () => {
    expect(() => beforeDestroy.call({d2Reader: {}})).not.toThrow()
  })
})
