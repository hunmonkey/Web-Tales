import {shallowMount, Wrapper} from '@vue/test-utils'
import Vue from 'vue'
import screenfull from 'screenfull'
import ShelfViewReaderShell from '@/shelfview/views/ShelfViewReaderShell.vue'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {ShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {BookDto} from '@/types/tales-books'
import {SeriesDto} from '@/types/tales-series'
import {SHELFVIEW_LOCATOR_SCHEMA_VERSION, ShelfViewPageLocator, ShelfViewTextLocator} from '@/shelfview/models/shelfview-locator'
import {ShelfViewTextContent} from '@/shelfview/models/shelfview-text'

jest.mock('@/functions/urls', () => ({
  __esModule: true,
  default: {
    originNoSlash: 'http://api.test',
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

const mockScreenfull = screenfull as unknown as {
  isEnabled: boolean,
  isFullscreen: boolean,
  request: jest.Mock,
  exit: jest.Mock,
  on: jest.Mock,
  off: jest.Mock,
}

const book = {
  id: 'book-1',
  seriesId: 'series-1',
  name: 'Book One',
  oneshot: false,
  media: {
    status: 'READY',
    mediaType: 'application/zip',
    mediaProfile: 'DIVINA',
    pagesCount: 2,
    comment: '',
    epubDivinaCompatible: false,
  },
  metadata: {
    title: 'Book One',
    number: '1',
  },
} as unknown as BookDto

const nextBook = {
  ...book,
  id: 'book-2',
  name: 'Book Two',
  metadata: {
    ...book.metadata,
    title: 'Book Two',
    number: '2',
  },
} as unknown as BookDto

const epubBook = {
  ...book,
  name: 'EPUB Book',
  media: {
    ...book.media,
    mediaType: 'application/epub+zip',
    mediaProfile: 'EPUB',
    pagesCount: 0,
    epubDivinaCompatible: false,
  },
  metadata: {
    ...book.metadata,
    title: 'EPUB Book',
  },
} as unknown as BookDto

const series = {
  id: 'series-1',
  metadata: {
    title: 'Series One',
  },
} as unknown as SeriesDto

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

const textManifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'Book One',
  kind: 'TEXT_REFLOW',
  sourceFormat: 'TXT',
  contentFingerprint: 'tales-file-state:book-1-text',
  text: {
    endpoint: '/api/shelfview/v1/books/book-1/text',
    detectedEncoding: 'UTF-8',
    normalizationVersion: 1,
  },
  capabilities: {
    search: false,
    textSelection: true,
    doublePage: false,
    continuousScroll: true,
    rtl: false,
    imageFilters: false,
  },
}

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
  contents: [
    {
      label: 'Chapter 1',
      locator: {
        schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
        kind: 'EPUB',
        href: 'Text/chapter.xhtml',
        title: 'Chapter 1',
        contentFingerprint: 'tales-file-state:book-1-epub',
      },
    },
  ],
  capabilities: {
    search: false,
    textSelection: true,
    doublePage: false,
    continuousScroll: true,
    rtl: true,
    imageFilters: false,
  },
}

const textContent: ShelfViewTextContent = {
  schemaVersion: 1,
  bookId: 'book-1',
  contentFingerprint: textManifest.contentFingerprint,
  encoding: 'UTF-8',
  normalizationVersion: 1,
  paragraphs: ['첫 번째 문단입니다.', '<script>alert("x")</script>', '마지막 문단입니다.'],
}

const mixedManifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'Mixed Book',
  kind: 'MIXED_CONTENT_SEQUENCE',
  sourceFormat: 'ZIP',
  contentFingerprint: 'tales-file-state:mixed-book',
  pageCount: 3,
  items: [
    {
      itemId: 'text-item',
      index: 0,
      kind: 'TEXT_REFLOW',
      label: '001.txt',
      contentFingerprint: 'tales-file-state:mixed-book:text-item',
      sourceFormat: 'TXT',
      text: {
        endpoint: '/api/shelfview/v1/books/book-1/mixed/text-item/text',
        detectedEncoding: 'UTF-8',
        normalizationVersion: 1,
      },
    },
    {
      itemId: 'image-item',
      index: 1,
      kind: 'FIXED_PAGE_SEQUENCE',
      label: '002.jpg',
      contentFingerprint: 'tales-file-state:mixed-book:image-item',
      sourceFormat: 'JPG',
      pageCount: 1,
      pages: {
        endpointTemplate: '/api/shelfview/v1/books/book-1/mixed/image-item/pages/{page}?zero_based=true',
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

const mixedTextContent: ShelfViewTextContent = {
  schemaVersion: 1,
  bookId: 'book-1',
  contentFingerprint: 'tales-file-state:mixed-book:text-item',
  encoding: 'UTF-8',
  normalizationVersion: 1,
  paragraphs: ['ZIP text opening', 'ZIP image follows'],
}

const pageLocator = (page: number): ShelfViewPageLocator => ({
  schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  kind: 'PAGE',
  page,
  offset: 0,
  contentFingerprint: manifest.contentFingerprint,
})

const textLocator = (
  paragraph: number,
  characterOffset = 0,
): ShelfViewTextLocator => ({
  schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  kind: 'TEXT',
  paragraph,
  characterOffset,
  progression: textProgression(paragraph, characterOffset),
  normalizationVersion: textManifest.text?.normalizationVersion ?? 1,
  contentFingerprint: textManifest.contentFingerprint,
})

function textProgression(
  paragraph: number,
  characterOffset: number,
): number {
  const text = textContent.paragraphs[paragraph] ?? ''
  const paragraphProgression = text.length > 0 ? characterOffset / text.length : 0
  return (paragraph + paragraphProgression) / textContent.paragraphs.length
}

const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))
const flushPromises = () => wait(0)
const mountedWrappers: Wrapper<Vue>[] = []
const overflowControlToggleTestIds = new Set([
  'shelfview-reader-layout-toggle',
  'shelfview-reader-text-theme-toggle',
  'shelfview-reader-rotate-toggle',
  'shelfview-reader-direction-toggle',
  'shelfview-reader-filter-toggle',
])

function setReaderStageRect(wrapper: Wrapper<Vue>) {
  const stage = wrapper.find('[data-testid="shelfview-reader-stage"]')
  Object.defineProperty(stage.element, 'getBoundingClientRect', {
    value: () => ({left: 0, top: 0, right: 1000, bottom: 700, width: 1000, height: 700}),
    configurable: true,
  })
  return stage
}

async function chooseControlOption(wrapper: Wrapper<Vue>, toggleTestId: string, optionTestId: string) {
  if (overflowControlToggleTestIds.has(toggleTestId) && !(wrapper.vm as any).overflowMenuOpen) {
    const moreToggle = wrapper.find('[data-testid="shelfview-reader-more-toggle"]')
    if (moreToggle.exists()) {
      await moreToggle.trigger('click')
      await Vue.nextTick()
    }
  }
  await wrapper.find(`[data-testid="${toggleTestId}"]`).trigger('click')
  await Vue.nextTick()
  await wrapper.find(`[data-testid="${optionTestId}"]`).trigger('click')
}

describe('ShelfViewReaderShell', () => {
  const originalReaderFlag = process.env.VUE_APP_SHELFVIEW_READER

  afterEach(() => {
    mountedWrappers.splice(0).forEach(wrapper => wrapper.destroy())
    jest.useRealTimers()
    process.env.VUE_APP_SHELFVIEW_READER = originalReaderFlag
    mockScreenfull.isEnabled = false
    mockScreenfull.isFullscreen = false
    mockScreenfull.request.mockClear()
    mockScreenfull.exit.mockClear()
    mockScreenfull.on.mockClear()
    mockScreenfull.off.mockClear()
  })

  it('loads the manifest and renders the first fixed page', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    expect(getManifest).toHaveBeenCalledWith('book-1')
    const page = wrapper.find('[data-testid="shelfview-reader-page"]')
    expect(page.exists()).toBe(true)
    expect(page.attributes('src')).toBe('http://api.test/api/v1/books/book-1/pages/0?zero_based=true')
    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1 / 2')
    expect(wrapper.find('[data-testid="shelfview-reader-previous"]').attributes('disabled')).toBe('disabled')
    expect(wrapper.find('[data-testid="shelfview-reader-next"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('tabindex')).toBe('0')
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('role')).toBe('application')
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('aria-label')).toContain('page 1 of 2')
    expect(wrapper.find('[data-testid="shelfview-reader-live-status"]').text()).toBe('Page 1 of 2')
  })

  it('starts with the reader toolbar visible and keeps it up while reading', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})

    await flushPromises()
    await Vue.nextTick()

    expect((wrapper.vm as any).chromeVisible).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-toolbar"]').attributes('style') ?? '').not.toContain('display: none')

    // flushPromises runs on a real timer, so the clock is only faked once the
    // reader is up, and nothing may hide the toolbar however long it sits idle.
    jest.useFakeTimers()
    jest.advanceTimersByTime(60000)
    await Vue.nextTick()

    expect((wrapper.vm as any).chromeVisible).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-toolbar"]').attributes('style') ?? '').not.toContain('display: none')
  })

  it('keeps only primary controls on the toolbar surface and moves the rest to overflow', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})

    await flushPromises()
    await Vue.nextTick()
    await wrapper.setData({chromeVisible: true})

    const primaryControls = wrapper.find('[data-testid="shelfview-reader-page-controls"]')
    expect(primaryControls.find('[data-testid="shelfview-reader-previous"]').exists()).toBe(true)
    expect(primaryControls.find('[data-testid="shelfview-reader-page-counter"]').exists()).toBe(true)
    expect(primaryControls.find('[data-testid="shelfview-reader-next"]').exists()).toBe(true)
    expect(primaryControls.find('[data-testid="shelfview-reader-fit-toggle"]').exists()).toBe(true)
    expect(primaryControls.find('[data-testid="shelfview-reader-more-toggle"]').exists()).toBe(true)
    expect(primaryControls.find('[data-testid="shelfview-reader-layout-toggle"]').exists()).toBe(false)
    expect(primaryControls.find('[data-testid="shelfview-reader-filter-toggle"]').exists()).toBe(false)
    expect(primaryControls.find('[data-testid="shelfview-reader-bookmarks-toggle"]').exists()).toBe(false)

    const overflow = wrapper.find('[data-testid="shelfview-reader-overflow"]')
    expect(overflow.attributes('style')).toContain('display: none')

    await wrapper.find('[data-testid="shelfview-reader-more-toggle"]').trigger('click')
    await Vue.nextTick()

    expect((wrapper.vm as any).overflowMenuOpen).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-overflow"]').attributes('style') ?? '').not.toContain('display: none')
    expect(wrapper.find('[data-testid="shelfview-reader-overflow"]').find('[data-testid="shelfview-reader-layout-toggle"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-overflow"]').find('[data-testid="shelfview-reader-filter-toggle"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-overflow"]').find('[data-testid="shelfview-reader-thumbnails-toggle"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-overflow"]').find('[data-testid="shelfview-reader-bookmarks-toggle"]').exists()).toBe(true)
  })

  it('keeps the reader toolbar up after a chrome panel closes', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})
    const vm = wrapper.vm as any

    await flushPromises()
    await Vue.nextTick()

    await wrapper.setData({overflowMenuOpen: true})
    vm.closeOverflowMenu()
    await Vue.nextTick()

    jest.useFakeTimers()
    jest.advanceTimersByTime(60000)
    await Vue.nextTick()

    expect(vm.chromeVisible).toBe(true)
  })

  it('hides the reader toolbar only when the reader asks and restores it the same way', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})
    const vm = wrapper.vm as any

    await flushPromises()
    await Vue.nextTick()

    vm.toggleChrome()
    await Vue.nextTick()

    expect(vm.chromeVisible).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-toolbar"]').attributes('style')).toContain('display: none')

    jest.useFakeTimers()
    jest.advanceTimersByTime(60000)
    await Vue.nextTick()

    expect(vm.chromeVisible).toBe(false)

    vm.toggleChrome()
    await Vue.nextTick()

    expect(vm.chromeVisible).toBe(true)
  })

  it('does not expose a legacy-reader fallback from ShelfView', async () => {
    const wrapper = mountShell({
      getManifest: jest.fn().mockResolvedValue(manifest),
      routeQuery: {context: 'SERIES', contextId: 'series-1', incognito: 'true'},
    })

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.html()).not.toContain('mdi-book-open-page-variant')
    expect(wrapper.text()).not.toContain('browse_book.read_book')
  })

  it('opens an EPUB bridge manifest without loading ShelfView saved state', async () => {
    const getManifest = jest.fn().mockResolvedValue(epubManifest)
    const getPosition = jest.fn().mockResolvedValue(undefined)
    const getBookmarks = jest.fn().mockResolvedValue([])
    const getHighlights = jest.fn().mockResolvedValue([])
    const getPreference = jest.fn().mockResolvedValue(undefined)
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {
        getPosition,
        getBookmarks,
        getHighlights,
        getPreference,
      },
      talesBooks: {
        getBook: jest.fn().mockResolvedValue(epubBook),
      },
      routeQuery: {context: 'SERIES', contextId: 'series-1', incognito: 'true'},
    })

    await flushPromises()
    await Vue.nextTick()

    expect(getManifest).toHaveBeenCalledWith('book-1')
    expect(wrapper.find('[data-testid="shelfview-reader-epub-bridge"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-empty"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-text"]').exists()).toBe(false)
    expect((wrapper.vm as any).adapter).toBeUndefined()
    expect((wrapper.vm as any).contentsAvailable).toBe(false)
    expect(getPosition).not.toHaveBeenCalled()
    expect(getBookmarks).not.toHaveBeenCalled()
    expect(getHighlights).not.toHaveBeenCalled()
    expect(getPreference).not.toHaveBeenCalled()

    await wrapper.find('[data-testid="shelfview-reader-epub-open"]').trigger('click')

    expect((wrapper.vm as any).$router.push).toHaveBeenCalledWith({
      name: 'read-epub',
      params: {bookId: 'book-1'},
      query: {
        context: 'SERIES',
        contextId: 'series-1',
        incognito: 'true',
      },
    })
  })

  it('keeps Divina-compatible EPUB fallback on the legacy fixed-page reader', async () => {
    const wrapper = mountShell({
      getManifest: jest.fn().mockResolvedValue(epubManifest),
      talesBooks: {
        getBook: jest.fn().mockResolvedValue({
          ...epubBook,
          media: {
            ...epubBook.media,
            epubDivinaCompatible: true,
          },
        }),
      },
    })

    await flushPromises()
    await Vue.nextTick()
    await wrapper.find('[data-testid="shelfview-reader-epub-open"]').trigger('click')

    expect((wrapper.vm as any).$router.push).toHaveBeenCalledWith({
      name: 'read-book',
      params: {bookId: 'book-1'},
      query: {
        context: undefined,
        contextId: undefined,
        incognito: undefined,
      },
    })
  })

  it('loads a text manifest and renders escaped paragraphs', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const getPreference = jest.fn().mockResolvedValue({
      scopeType: 'BOOK',
      scopeId: 'book-1',
      format: 'TEXT_REFLOW',
      effectiveSettings: {},
      sources: [],
    })
    const getTextContent = jest.fn().mockResolvedValue(textContent)
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {getPreference},
      shelfViewText: {getTextContent},
    })

    await flushPromises()
    await Vue.nextTick()

    expect(getTextContent).toHaveBeenCalledWith(textManifest)
    expect(getPreference).toHaveBeenCalledWith({
      scopeType: 'BOOK',
      scopeId: 'book-1',
      format: 'TEXT_REFLOW',
    })
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-text"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="shelfview-reader-text-paragraph"]').wrappers.map(paragraph => paragraph.text())).toEqual(['첫 번째 문단입니다.'])
    expect(wrapper.find('[data-testid="shelfview-reader-filter-toggle"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-fit-toggle"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-direction-toggle"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-rotate-toggle"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-slideshow-toggle"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-page-jump"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-fixed-settings-toggle"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-thumbnail-strip"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-text-theme-toggle"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-text-settings-toggle"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('aria-label')).toContain('paragraph 1 of 3')
    expect(wrapper.find('[data-testid="shelfview-reader-live-status"]').text()).toBe('Paragraph 1 of 3')

    await wrapper.setData({chromeVisible: true, overflowMenuOpen: true})
    await Vue.nextTick()

    const primaryControls = wrapper.find('[data-testid="shelfview-reader-page-controls"]')
    expect(primaryControls.find('[data-testid="shelfview-reader-text-theme-toggle"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-overflow"]').find('[data-testid="shelfview-reader-text-theme-toggle"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-overflow"]').find('[data-testid="shelfview-reader-text-settings-toggle"]').exists()).toBe(true)

    await chooseControlOption(wrapper, 'shelfview-reader-layout-toggle', 'shelfview-reader-layout-option-CONTINUOUS_SCROLL')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.findAll('[data-testid="shelfview-reader-text-paragraph"]').wrappers.map(paragraph => paragraph.text())).toEqual([
      '첫 번째 문단입니다.',
      '<script>alert("x")</script>',
      '마지막 문단입니다.',
    ])
    expect(wrapper.find('script').exists()).toBe(false)
  })

  it('applies and saves text display settings', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const savePreference = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {
        getPreference: jest.fn().mockResolvedValue({
          scopeType: 'BOOK',
          scopeId: 'book-1',
          format: 'TEXT_REFLOW',
          effectiveSettings: {
            layout: 'CONTINUOUS_SCROLL',
            textTheme: 'DARK',
            textFontFamily: 'READABLE_KR',
            textFontSize: 22,
            textLineHeight: 1.9,
            textLetterSpacing: 0.8,
            textWordSpacing: 2,
            textContentWidth: 680,
            textPageMargin: 64,
            textParagraphSpacing: 1.55,
            textAlign: 'JUSTIFY',
            textFontWeight: 500,
            textBrightness: 1.08,
          },
          sources: [],
        }),
        savePreference,
      },
    })

    await flushPromises()
    await Vue.nextTick()

    const text = wrapper.find('[data-testid="shelfview-reader-text"]')
    expect(text.classes()).toContain('shelfview-reader-shell__text--dark')
    expect(text.attributes('style')).toContain('--sv-text-font-size: 22px;')
    expect(text.attributes('style')).toContain('--sv-text-content-width: 680px;')
    expect(text.attributes('style')).toContain('--sv-text-align: justify;')
    expect(text.attributes('style')).toContain('--sv-text-brightness: 1.08;')

    await wrapper.find('[data-testid="shelfview-reader-text-settings-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-text-settings"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-text-settings-primary"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-text-theme-chip-DARK"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-text-font-size-decrease"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-text-font-size-increase"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-text-line-height"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-text-settings-advanced"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-text-settings-more"]').exists()).toBe(true)

    await wrapper.find('[data-testid="shelfview-reader-text-theme-chip-OLED"]').trigger('click')
    await wrapper.find('[data-testid="shelfview-reader-text-font-size-increase"]').trigger('click')
    await Vue.nextTick()

    await wrapper.setData({
      textLetterSpacing: 1.2,
      textContentWidth: 640,
      textBrightness: 0.92,
    })
    const vm = wrapper.vm as any
    vm.onTextDisplaySettingChanged()
    await Vue.nextTick()
    await vm.flushPreferenceSave()

    expect(savePreference).toHaveBeenCalledWith({
      scopeType: 'BOOK',
        scopeId: 'book-1',
        format: 'TEXT_REFLOW',
        settings: expect.objectContaining({
          layout: 'CONTINUOUS_SCROLL',
          textTheme: 'OLED',
          textFontFamily: 'READABLE_KR',
          textFontSize: 23,
          textLetterSpacing: 1.2,
          textContentWidth: 640,
          textAlign: 'JUSTIFY',
          textBrightness: 0.92,
        }),
    })
  })

  it('loads a mixed ZIP manifest and moves from text to image content', async () => {
    const getManifest = jest.fn().mockResolvedValue(mixedManifest)
    const getTextContent = jest.fn().mockResolvedValue(mixedTextContent)
    const savePosition = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest,
      shelfViewText: {getTextContent},
      shelfViewState: {savePosition},
    })

    await flushPromises()
    await Vue.nextTick()

    expect(getTextContent).toHaveBeenCalledWith(expect.objectContaining({
      kind: 'TEXT_REFLOW',
      contentFingerprint: 'tales-file-state:mixed-book:text-item',
    }))
    expect(wrapper.find('[data-testid="shelfview-reader-text"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').text()).toBe('ZIP text opening')
    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1 / 2')

    await wrapper.find('[data-testid="shelfview-reader-next"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').text()).toBe('ZIP image follows')

    await wrapper.find('[data-testid="shelfview-reader-next"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-text"]').exists()).toBe(false)
    const page = wrapper.find('[data-testid="shelfview-reader-page"]')
    expect(page.exists()).toBe(true)
    expect(page.attributes('src')).toBe('http://api.test/api/shelfview/v1/books/book-1/mixed/image-item/pages/0?zero_based=true')

    await (wrapper.vm as any).flushPositionSave()
    await Vue.nextTick()

    expect(savePosition).toHaveBeenCalledWith('book-1', {
      locator: expect.objectContaining({
        kind: 'MIXED',
        itemId: 'image-item',
        itemIndex: 1,
        contentFingerprint: mixedManifest.contentFingerprint,
        itemLocator: expect.objectContaining({
          kind: 'PAGE',
          page: 0,
          contentFingerprint: 'tales-file-state:mixed-book:image-item',
        }),
      }),
      progression: 1,
    })
  })

  it('opens mixed content table of contents and jumps to an item', async () => {
    const getManifest = jest.fn().mockResolvedValue(mixedManifest)
    const getTextContent = jest.fn().mockResolvedValue(mixedTextContent)
    const savePosition = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest,
      shelfViewText: {getTextContent},
      shelfViewState: {savePosition},
    })

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-contents-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-contents-drawer"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="shelfview-contents-item"]').at(0).text()).toContain('001.txt')
    expect(wrapper.findAll('[data-testid="shelfview-contents-item"]').at(1).text()).toContain('002.jpg')

    await wrapper.findAll('[data-testid="shelfview-contents-item"]').at(1).trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-contents-drawer"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-text"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('src')).toBe('http://api.test/api/shelfview/v1/books/book-1/mixed/image-item/pages/0?zero_based=true')

    await (wrapper.vm as any).flushPositionSave()
    await Vue.nextTick()

    expect(savePosition).toHaveBeenCalledWith('book-1', {
      locator: expect.objectContaining({
        kind: 'MIXED',
        itemId: 'image-item',
        itemIndex: 1,
      }),
      progression: 1,
    })
  })

  it('opens manifest-provided contents and jumps to a fixed page', async () => {
    const getManifest = jest.fn().mockResolvedValue({
      ...manifest,
      pageCount: 3,
      contents: [
        {label: 'Opening', locator: pageLocator(0)},
        {label: 'Second section', locator: pageLocator(1)},
      ],
    })
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-contents-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-contents-drawer"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="shelfview-contents-item"]')).toHaveLength(2)
    expect(wrapper.findAll('[data-testid="shelfview-contents-item"]').at(0).text()).toContain('Opening')
    expect(wrapper.findAll('[data-testid="shelfview-contents-item"]').at(1).text()).toContain('Second section')

    await wrapper.findAll('[data-testid="shelfview-contents-item"]').at(1).trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('2 / 3')
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('src')).toBe('http://api.test/api/v1/books/book-1/pages/1?zero_based=true')
    expect(wrapper.find('[data-testid="shelfview-contents-drawer"]').exists()).toBe(false)
  })

  it('opens TXT paragraph contents and jumps to a paragraph', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-contents-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-contents-drawer"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-testid="shelfview-contents-item"]').at(0).text()).toContain('첫 번째 문단입니다.')
    expect(wrapper.findAll('[data-testid="shelfview-contents-item"]').at(2).text()).toContain('마지막 문단입니다.')

    await wrapper.findAll('[data-testid="shelfview-contents-item"]').at(2).trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('3 / 3')
    expect(wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').text()).toBe('마지막 문단입니다.')
    expect(wrapper.find('[data-testid="shelfview-contents-drawer"]').exists()).toBe(false)
  })

  it('prefers heading-like TXT paragraphs in the contents panel', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const getTextContent = jest.fn().mockResolvedValue({
      ...textContent,
      paragraphs: [
        '제1장 시작',
        '첫 장의 일반 문단입니다.',
        'Chapter 2 Next',
        '두 번째 장의 일반 문단입니다.',
      ],
    })
    const wrapper = mountShell({
      getManifest,
      shelfViewText: {getTextContent},
    })

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-contents-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.findAll('[data-testid="shelfview-contents-item"]')).toHaveLength(2)
    expect(wrapper.findAll('[data-testid="shelfview-contents-item"]').at(0).text()).toContain('제1장 시작')
    expect(wrapper.findAll('[data-testid="shelfview-contents-item"]').at(1).text()).toContain('Chapter 2 Next')

    await wrapper.findAll('[data-testid="shelfview-contents-item"]').at(1).trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('3 / 4')
    expect(wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').text()).toBe('Chapter 2 Next')
  })

  it('moves between text paragraphs and saves text position', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const getTextContent = jest.fn().mockResolvedValue(textContent)
    const savePosition = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest,
      shelfViewText: {getTextContent},
      shelfViewState: {savePosition},
    })

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-next"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('2 / 3')
    expect(wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').text()).toBe('<script>alert("x")</script>')

    await (wrapper.vm as any).flushPositionSave()
    await Vue.nextTick()

    expect(savePosition).toHaveBeenCalledWith('book-1', {
      locator: textLocator(1),
      progression: 1 / 3,
    })
  })

  it('opens the end-of-book prompt and routes to the next book', async () => {
    process.env.VUE_APP_SHELFVIEW_READER = 'true'
    const getBookSiblingNext = jest.fn().mockResolvedValue(nextBook)
    const updateReadProgress = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest: jest.fn().mockResolvedValue(manifest),
      talesBooks: {getBookSiblingNext, updateReadProgress},
    })

    await flushPromises()
    await Vue.nextTick()

    await (wrapper.vm as any).goToPageIndex(1)
    await Vue.nextTick()
    await (wrapper.vm as any).runReaderInputAction('NEXT')
    await flushPromises()
    await Vue.nextTick()

    expect(getBookSiblingNext).toHaveBeenCalledWith('book-1')
    expect(wrapper.find('[data-testid="shelfview-reader-end-prompt"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-end-next-title"]').text()).toContain('Book Two')

    await wrapper.find('[data-testid="shelfview-reader-end-next"]').trigger('click')
    await flushPromises()

    expect(updateReadProgress).toHaveBeenCalledWith('book-1', {completed: true})
    expect((wrapper.vm as any).$router.push).toHaveBeenCalledWith({
      name: 'read-book-v2',
      params: {bookId: 'book-2'},
      query: {context: undefined, contextId: undefined, incognito: undefined},
    })
  })

  it('shows the last-volume prompt and exits the reader when there is no next book', async () => {
    const updateReadProgress = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest: jest.fn().mockResolvedValue(manifest),
      talesBooks: {
        getBookSiblingNext: jest.fn().mockRejectedValue(new Error('No next book')),
        updateReadProgress,
      },
    })

    await flushPromises()
    await Vue.nextTick()

    await (wrapper.vm as any).goToPageIndex(1)
    await Vue.nextTick()
    await (wrapper.vm as any).nextPage()
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-end-no-next"]').text()).toBe('shelfview_reader.no_next_book')
    expect(wrapper.find('[data-testid="shelfview-reader-end-next"]').exists()).toBe(false)

    await wrapper.find('[data-testid="shelfview-reader-end-exit"]').trigger('click')
    await flushPromises()

    expect(updateReadProgress).toHaveBeenCalledWith('book-1', {completed: true})
    expect((wrapper.vm as any).$router.push).toHaveBeenCalledWith({
      name: 'browse-book',
      params: {bookId: 'book-1', seriesId: 'series-1'},
      query: {context: undefined, contextId: undefined},
    })
  })

  it('keeps incognito end-of-book actions from marking the book completed', async () => {
    const updateReadProgress = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest: jest.fn().mockResolvedValue(manifest),
      talesBooks: {
        getBookSiblingNext: jest.fn().mockRejectedValue(new Error('No next book')),
        updateReadProgress,
      },
      routeQuery: {incognito: 'true'},
    })

    await flushPromises()
    await Vue.nextTick()

    await (wrapper.vm as any).goToPageIndex(1)
    await Vue.nextTick()
    await (wrapper.vm as any).nextPage()
    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-end-exit"]').trigger('click')
    await flushPromises()

    expect(updateReadProgress).not.toHaveBeenCalled()
  })

  it('continues to the next book when ArrowRight is pressed on the end-of-book prompt', async () => {
    process.env.VUE_APP_SHELFVIEW_READER = 'true'
    const getBookSiblingNext = jest.fn().mockResolvedValue(nextBook)
    const updateReadProgress = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest: jest.fn().mockResolvedValue(manifest),
      talesBooks: {getBookSiblingNext, updateReadProgress},
    })

    await flushPromises()
    await Vue.nextTick()

    await (wrapper.vm as any).goToPageIndex(1)
    await Vue.nextTick()
    await (wrapper.vm as any).runReaderInputAction('NEXT')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-end-prompt"]').exists()).toBe(true)

    const preventDefault = jest.fn()
    await (wrapper.vm as any).onReaderKeydown({
      key: 'ArrowRight',
      ctrlKey: false,
      altKey: false,
      metaKey: false,
      target: wrapper.find('[data-testid="shelfview-reader-end-prompt"]').element,
      preventDefault,
    })
    await flushPromises()

    expect(preventDefault).toHaveBeenCalled()
    expect(updateReadProgress).toHaveBeenCalledWith('book-1', {completed: true})
    expect((wrapper.vm as any).$router.push).toHaveBeenCalledWith({
      name: 'read-book-v2',
      params: {bookId: 'book-2'},
      query: {context: undefined, contextId: undefined, incognito: undefined},
    })
  })

  it('closes the end-of-book prompt when Escape is pressed without leaving the book', async () => {
    const getBookSiblingNext = jest.fn().mockResolvedValue(nextBook)
    const updateReadProgress = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest: jest.fn().mockResolvedValue(manifest),
      talesBooks: {getBookSiblingNext, updateReadProgress},
    })

    await flushPromises()
    await Vue.nextTick()

    await (wrapper.vm as any).goToPageIndex(1)
    await Vue.nextTick()
    await (wrapper.vm as any).runReaderInputAction('NEXT')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-end-prompt"]').exists()).toBe(true)

    const preventDefault = jest.fn()
    await (wrapper.vm as any).onReaderKeydown({
      key: 'Escape',
      ctrlKey: false,
      altKey: false,
      metaKey: false,
      target: wrapper.find('[data-testid="shelfview-reader-end-prompt"]').element,
      preventDefault,
    })
    await Vue.nextTick()

    expect(preventDefault).toHaveBeenCalled()
    expect(wrapper.find('[data-testid="shelfview-reader-end-prompt"]').exists()).toBe(false)
    expect(updateReadProgress).not.toHaveBeenCalled()
    expect((wrapper.vm as any).$router.push).not.toHaveBeenCalled()
  })

  it('restores saved text paragraph and character offset', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const savedLocator = textLocator(2, 4)
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {
        getPosition: jest.fn().mockResolvedValue({
          bookId: 'book-1',
          locator: savedLocator,
          progression: savedLocator.progression,
          contentFingerprint: textManifest.contentFingerprint,
          updatedAt: '2026-06-19T00:00:00Z',
        }),
      },
    })

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('3 / 3')
    expect(wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').text()).toBe(textContent.paragraphs[2])
    expect((wrapper.vm as any).currentLocator).toMatchObject({
      kind: 'TEXT',
      paragraph: 2,
      characterOffset: 4,
      normalizationVersion: 1,
      contentFingerprint: textManifest.contentFingerprint,
    })
  })

  it('updates text character offset while scrolling', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await chooseControlOption(wrapper, 'shelfview-reader-layout-toggle', 'shelfview-reader-layout-option-CONTINUOUS_SCROLL')
    await flushPromises()
    await Vue.nextTick()

    const stage = wrapper.find('[data-testid="shelfview-reader-stage"]').element as HTMLElement
    stage.getBoundingClientRect = jest.fn(() => ({top: 0, bottom: 480, left: 0, right: 320, width: 320, height: 480} as DOMRect))

    const paragraphs = wrapper.findAll('[data-testid="shelfview-reader-text-paragraph"]').wrappers
    paragraphs[0].element.getBoundingClientRect = jest.fn(() => ({top: -220, bottom: -120, left: 0, right: 320, width: 320, height: 100} as DOMRect))
    paragraphs[1].element.getBoundingClientRect = jest.fn(() => ({top: -50, bottom: 50, left: 0, right: 320, width: 320, height: 100} as DOMRect))
    paragraphs[2].element.getBoundingClientRect = jest.fn(() => ({top: 120, bottom: 220, left: 0, right: 320, width: 320, height: 100} as DOMRect))

    await (wrapper.vm as any).onReaderStageScroll()
    await flushPromises()
    await Vue.nextTick()

    expect((wrapper.vm as any).currentLocator).toMatchObject({
      kind: 'TEXT',
      paragraph: 1,
      characterOffset: Math.floor(textContent.paragraphs[1].length / 2),
    })
    expect(wrapper.find('[data-testid="shelfview-reader-sync-status"]').text()).toBe('shelfview_reader.status_saving')
  })

  it('uses the saved character offset when restoring text scroll position after display changes', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const savedLocator = textLocator(1, 6)
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {
        getPosition: jest.fn().mockResolvedValue({
          bookId: 'book-1',
          locator: savedLocator,
          progression: savedLocator.progression,
          contentFingerprint: textManifest.contentFingerprint,
          updatedAt: '2026-06-19T00:00:00Z',
        }),
      },
    })

    await flushPromises()
    await Vue.nextTick()

    const stage = wrapper.find('[data-testid="shelfview-reader-stage"]').element as HTMLElement
    Object.defineProperty(stage, 'scrollTop', {value: 0, writable: true, configurable: true})

    const paragraph = wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').element as HTMLElement
    Object.defineProperty(paragraph, 'offsetTop', {value: 120, configurable: true})
    Object.defineProperty(paragraph, 'offsetHeight', {value: 80, configurable: true})

    void (wrapper.vm as any).restoreCurrentTextScrollPosition()

    expect(stage.scrollTop).toBeCloseTo(120 + (80 * (6 / textContent.paragraphs[1].length)))
  })

  it('restores text scroll position after browser viewport resize', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const savedLocator = textLocator(1, 6)
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {
        getPosition: jest.fn().mockResolvedValue({
          bookId: 'book-1',
          locator: savedLocator,
          progression: savedLocator.progression,
          contentFingerprint: textManifest.contentFingerprint,
          updatedAt: '2026-06-19T00:00:00Z',
        }),
      },
    })

    await flushPromises()
    await Vue.nextTick()

    const stage = wrapper.find('[data-testid="shelfview-reader-stage"]').element as HTMLElement
    Object.defineProperty(stage, 'scrollTop', {value: 0, writable: true, configurable: true})

    const paragraph = wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').element as HTMLElement
    Object.defineProperty(paragraph, 'offsetTop', {value: 180, configurable: true})
    Object.defineProperty(paragraph, 'offsetHeight', {value: 100, configurable: true})

    window.dispatchEvent(new Event('resize'))
    await Vue.nextTick()

    expect(stage.scrollTop).toBeCloseTo(180 + (100 * (6 / textContent.paragraphs[1].length)))
  })

  it('cycles text themes and can show every text paragraph in continuous mode', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const getTextContent = jest.fn().mockResolvedValue(textContent)
    const wrapper = mountShell({
      getManifest,
      shelfViewText: {getTextContent},
    })

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-text"]').classes()).toContain('shelfview-reader-shell__text--paper')

    await chooseControlOption(wrapper, 'shelfview-reader-text-theme-toggle', 'shelfview-reader-text-theme-option-DARK')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-text"]').classes()).toContain('shelfview-reader-shell__text--dark')

    await chooseControlOption(wrapper, 'shelfview-reader-layout-toggle', 'shelfview-reader-layout-option-CONTINUOUS_SCROLL')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.findAll('[data-testid="shelfview-reader-text-paragraph"]').wrappers.map(paragraph => paragraph.attributes('data-text-paragraph-index'))).toEqual(['0', '1', '2'])
  })

  it('searches TXT paragraphs and jumps to the matching paragraph', async () => {
    const getManifest = jest.fn().mockResolvedValue({
      ...textManifest,
      capabilities: {
        ...textManifest.capabilities,
        search: true,
      },
    })
    const savePosition = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {savePosition},
    })

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-search-toggle"]').trigger('click')
    await Vue.nextTick()
    await wrapper.find('[data-testid="shelfview-reader-search-input"]').setValue('마지막')
    await flushPromises()
    await Vue.nextTick()
    await (wrapper.vm as any).flushPositionSave()

    expect(wrapper.find('[data-testid="shelfview-reader-search-status"]').text()).toBe('shelfview_reader.search_match_status')
    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('3 / 3')
    expect(wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').classes()).toContain('shelfview-reader-shell__text-paragraph--search-current')
    expect(savePosition).toHaveBeenCalledWith('book-1', expect.objectContaining({
      locator: expect.objectContaining({
        kind: 'TEXT',
        paragraph: 2,
      }),
    }))
  })

  it('highlights selected TXT text through the dedicated highlight API', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const createBookmark = jest.fn()
    const createHighlight = jest.fn().mockImplementation(async (_bookId, highlight) => ({
      id: 'highlight-1',
      bookId: 'book-1',
      contentFingerprint: textManifest.contentFingerprint,
      createdAt: 'now',
      updatedAt: 'now',
      ...highlight,
    }))
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {
        createBookmark,
        createHighlight,
      },
    })

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').classes()).not.toContain('shelfview-reader-shell__text-paragraph--highlighted')

    const firstParagraph = wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').element
    const firstTextNode = firstParagraph.querySelector('span')?.firstChild
    expect(firstTextNode).toBeTruthy()
    const range = document.createRange()
    range.setStart(firstTextNode as Node, 0)
    range.setEnd(firstTextNode as Node, 4)
    const originalGetSelection = window.getSelection
    const removeAllRanges = jest.fn()
    Object.defineProperty(window, 'getSelection', {
      configurable: true,
      value: jest.fn(() => ({
        rangeCount: 1,
        isCollapsed: false,
        getRangeAt: () => range,
        toString: () => '첫 번째',
        removeAllRanges,
      })),
    })

    try {
      await wrapper.find('[data-testid="shelfview-reader-highlight-add"]').trigger('click')
      await flushPromises()
      await Vue.nextTick()
    } finally {
      Object.defineProperty(window, 'getSelection', {
        configurable: true,
        value: originalGetSelection,
      })
    }

    expect(createHighlight).toHaveBeenCalledWith('book-1', expect.objectContaining({
      color: 'YELLOW',
      textQuote: '첫 번째',
      locator: expect.objectContaining({
        kind: 'TEXT',
        paragraph: 0,
        characterOffset: 0,
      }),
      selector: expect.objectContaining({
        kind: 'TEXT_RANGE',
        startParagraph: 0,
        startCharacterOffset: 0,
        endParagraph: 0,
        endCharacterOffset: 4,
        normalizationVersion: 1,
      }),
    }))
    expect(createBookmark).not.toHaveBeenCalled()
    expect(wrapper.find('[data-testid="shelfview-reader-text-highlight"]').text()).toBe('첫 번째')
    expect(wrapper.find('[data-testid="shelfview-reader-highlight-add"]').attributes('disabled')).toBeUndefined()
    expect(removeAllRanges).toHaveBeenCalled()
  })

  it('opens dictionary lookup for selected TXT text without searching automatically', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const hadGetSelection = 'getSelection' in window
    const originalGetSelection = window.getSelection
    Object.defineProperty(window, 'getSelection', {
      configurable: true,
      value: jest.fn(() => ({
        toString: () => '  첫 번째  ',
      })),
    })

    try {
      const wrapper = mountShell({getManifest})

      await flushPromises()
      await Vue.nextTick()

      await wrapper.find('[data-testid="shelfview-reader-dictionary-toggle"]').trigger('click')
      await Vue.nextTick()

      expect(wrapper.find('[data-testid="shelfview-dictionary-drawer"]').exists()).toBe(true)
      expect((wrapper.find('[data-testid="shelfview-dictionary-query"]').element as HTMLInputElement).value).toBe('첫 번째')
      expect(wrapper.find('[data-testid="shelfview-dictionary-results"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="shelfview-dictionary-result"]').attributes('data-provider-kind')).toBe('WEB_LOOKUP')
      expect(wrapper.find('[data-testid="shelfview-dictionary-result"]').text()).toContain('shelfview_reader.dictionary_provider_web')
      expect(decodeURIComponent(wrapper.find('[data-testid="shelfview-dictionary-search"]').attributes('href') ?? '')).toContain('첫 번째 dictionary')
    } finally {
      if (hadGetSelection) {
        Object.defineProperty(window, 'getSelection', {
          configurable: true,
          value: originalGetSelection,
        })
      } else {
        delete (window as any).getSelection
      }
    }
  })

  it('renders local dictionary provider results as text before the web fallback', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const hadGetSelection = 'getSelection' in window
    const originalGetSelection = window.getSelection
    Object.defineProperty(window, 'getSelection', {
      configurable: true,
      value: jest.fn(() => ({
        toString: () => '첫 번째',
      })),
    })

    try {
      const wrapper = mountShell({getManifest})

      await flushPromises()
      await Vue.nextTick()
      await wrapper.setData({
        dictionaryLocalEntries: [{
          term: '첫 번째',
          definition: '첫 번째 뜻 <b>강조 아님</b>',
          sourceName: 'Test local pack',
        }],
      })

      await wrapper.find('[data-testid="shelfview-reader-dictionary-toggle"]').trigger('click')
      await Vue.nextTick()

      const results = wrapper.findAll('[data-testid="shelfview-dictionary-result"]')
      expect(results).toHaveLength(2)
      expect(results.at(0).attributes('data-provider-kind')).toBe('LOCAL_PACK')
      expect(results.at(0).text()).toContain('shelfview_reader.dictionary_provider_local')
      expect(wrapper.find('[data-testid="shelfview-dictionary-term"]').text()).toBe('첫 번째')
      expect(wrapper.find('[data-testid="shelfview-dictionary-definition"]').text()).toBe('첫 번째 뜻 <b>강조 아님</b>')
      expect(wrapper.find('[data-testid="shelfview-dictionary-definition"] b').exists()).toBe(false)
      expect(wrapper.find('[data-testid="shelfview-dictionary-source"]').text()).toBe('Test local pack')
      expect(results.at(1).attributes('data-provider-kind')).toBe('WEB_LOOKUP')
    } finally {
      if (hadGetSelection) {
        Object.defineProperty(window, 'getSelection', {
          configurable: true,
          value: originalGetSelection,
        })
      } else {
        delete (window as any).getSelection
      }
    }
  })

  it('renders account-synced dictionary results before local and web fallback', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const getDictionaryProviders = jest.fn().mockResolvedValue([
      {
        id: 'provider-1',
        kind: 'LOCAL_PACK',
        label: 'Server pack',
        enabled: true,
        sourceName: 'Server pack',
        licenseName: 'First-party test data',
        entryCount: 1,
        createdAt: 'now',
        updatedAt: 'now',
      },
    ])
    const lookupDictionary = jest.fn().mockResolvedValue([
      {
        key: 'server-entry-1',
        kind: 'LOCAL_PACK',
        providerLabel: 'Server pack',
        term: '첫 번째',
        definition: '서버에 저장된 뜻',
        sourceName: 'Server pack',
      },
    ])
    const deleteDictionaryProvider = jest.fn().mockResolvedValue(undefined)
    const hadGetSelection = 'getSelection' in window
    const originalGetSelection = window.getSelection
    Object.defineProperty(window, 'getSelection', {
      configurable: true,
      value: jest.fn(() => ({
        toString: () => '첫 번째',
      })),
    })

    try {
      const wrapper = mountShell({
        getManifest,
        shelfViewState: {
          getDictionaryProviders,
          lookupDictionary,
          deleteDictionaryProvider,
        },
      })

      await flushPromises()
      await Vue.nextTick()
      await wrapper.setData({
        dictionaryLocalEntries: [{
          term: '첫 번째',
          definition: '브라우저-local 뜻',
          sourceName: 'Browser pack',
        }],
      })

      await wrapper.find('[data-testid="shelfview-reader-dictionary-toggle"]').trigger('click')
      await flushPromises()
      await Vue.nextTick()

      const results = wrapper.findAll('[data-testid="shelfview-dictionary-result"]')
      expect(results).toHaveLength(3)
      expect(lookupDictionary).toHaveBeenCalledWith('첫 번째')
      expect(results.at(0).text()).toContain('Server pack')
      expect(results.at(0).text()).toContain('서버에 저장된 뜻')
      expect(results.at(1).text()).toContain('브라우저-local 뜻')

      await wrapper.find('[data-testid="shelfview-dictionary-clear"]').trigger('click')
      await flushPromises()

      expect(deleteDictionaryProvider).toHaveBeenCalledWith('provider-1')
    } finally {
      if (hadGetSelection) {
        Object.defineProperty(window, 'getSelection', {
          configurable: true,
          value: originalGetSelection,
        })
      } else {
        delete (window as any).getSelection
      }
    }
  })

  it('imports a local dictionary pack JSON into the dictionary drawer session', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const hadGetSelection = 'getSelection' in window
    const originalGetSelection = window.getSelection
    Object.defineProperty(window, 'getSelection', {
      configurable: true,
      value: jest.fn(() => ({
        toString: () => '첫 번째',
      })),
    })

    try {
      const wrapper = mountShell({getManifest})

      await flushPromises()
      await Vue.nextTick()

      await (wrapper.vm as any).importDictionaryPackJson(JSON.stringify({
        schemaVersion: 1,
        metadata: {
          sourceName: 'Session pack',
          sourceUrl: 'https://example.test/session-pack',
          licenseName: 'First-party test data',
        },
        entries: [
          {
            term: '첫 번째',
            definition: '세션에서 가져온 뜻',
          },
        ],
      }))

      await wrapper.find('[data-testid="shelfview-reader-dictionary-toggle"]').trigger('click')
      await Vue.nextTick()

      expect((wrapper.vm as any).readerStateMessage).toBe('shelfview_reader.status_dictionary_pack_loaded')
      expect(wrapper.find('[data-testid="shelfview-dictionary-import"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="shelfview-dictionary-definition"]').text()).toBe('세션에서 가져온 뜻')
      expect(wrapper.find('[data-testid="shelfview-dictionary-source"]').text()).toBe('Session pack')
    } finally {
      if (hadGetSelection) {
        Object.defineProperty(window, 'getSelection', {
          configurable: true,
          value: originalGetSelection,
        })
      } else {
        delete (window as any).getSelection
      }
    }
  })

  it('persists an imported dictionary pack in browser storage and can clear it', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const hadGetSelection = 'getSelection' in window
    const originalGetSelection = window.getSelection
    Object.defineProperty(window, 'getSelection', {
      configurable: true,
      value: jest.fn(() => ({
        toString: () => '첫 번째',
      })),
    })
    window.localStorage.clear()

    try {
      const wrapper = mountShell({getManifest})

      await flushPromises()
      await Vue.nextTick()

      await (wrapper.vm as any).importDictionaryPackJson(JSON.stringify({
        schemaVersion: 1,
        metadata: {
          sourceName: 'Stored pack',
          licenseName: 'First-party test data',
        },
        entries: [
          {
            term: '첫 번째',
            definition: '브라우저에 기억된 뜻',
          },
        ],
      }))

      expect(window.localStorage.getItem('shelfview.dictionaryPack.v1')).toContain('Stored pack')
      wrapper.destroy()

      const restoredWrapper = mountShell({getManifest})
      await flushPromises()
      await Vue.nextTick()

      await restoredWrapper.find('[data-testid="shelfview-reader-dictionary-toggle"]').trigger('click')
      await Vue.nextTick()

      expect(restoredWrapper.find('[data-testid="shelfview-dictionary-definition"]').text()).toBe('브라우저에 기억된 뜻')
      await restoredWrapper.find('[data-testid="shelfview-dictionary-clear"]').trigger('click')
      await Vue.nextTick()

      expect(window.localStorage.getItem('shelfview.dictionaryPack.v1')).toBeNull()
      expect((restoredWrapper.vm as any).dictionaryLocalEntries).toEqual([])
      expect((restoredWrapper.vm as any).readerStateMessage).toBe('shelfview_reader.status_dictionary_pack_cleared')
    } finally {
      window.localStorage.clear()
      if (hadGetSelection) {
        Object.defineProperty(window, 'getSelection', {
          configurable: true,
          value: originalGetSelection,
        })
      } else {
        delete (window as any).getSelection
      }
    }
  })

  it('reads selected TXT text aloud when the selection is inside the reader', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const speak = jest.fn()
    const cancel = jest.fn()
    const hadGetSelection = 'getSelection' in window
    const originalGetSelection = window.getSelection
    const hadSpeechSynthesis = 'speechSynthesis' in window
    const originalSpeechSynthesis = window.speechSynthesis
    const hadSpeechSynthesisUtterance = 'SpeechSynthesisUtterance' in window
    const originalSpeechSynthesisUtterance = window.SpeechSynthesisUtterance

    class TestSpeechSynthesisUtterance {
      text: string
      onend: (() => void) | null = null
      onerror: (() => void) | null = null

      constructor(text: string) {
        this.text = text
      }
    }

    Object.defineProperty(window, 'speechSynthesis', {
      configurable: true,
      value: {
        cancel,
        speak,
      },
    })
    Object.defineProperty(window, 'SpeechSynthesisUtterance', {
      configurable: true,
      value: TestSpeechSynthesisUtterance,
    })

    try {
      const wrapper = mountShell({getManifest})

      await flushPromises()
      await Vue.nextTick()

      const firstParagraph = wrapper.find('[data-testid="shelfview-reader-text-paragraph"]').element
      const firstTextNode = firstParagraph.querySelector('span')?.firstChild
      expect(firstTextNode).toBeTruthy()
      const range = document.createRange()
      range.setStart(firstTextNode as Node, 0)
      range.setEnd(firstTextNode as Node, 4)

      Object.defineProperty(window, 'getSelection', {
        configurable: true,
        value: jest.fn(() => ({
          rangeCount: 1,
          isCollapsed: false,
          getRangeAt: () => range,
          toString: () => '첫 번째',
        })),
      })

      cancel.mockClear()
      speak.mockClear()

      await wrapper.find('[data-testid="shelfview-reader-tts-toggle"]').trigger('click')
      await Vue.nextTick()

      expect(speak).toHaveBeenCalledWith(expect.objectContaining({
        text: '첫 번째',
      }))
    } finally {
      if (hadGetSelection) {
        Object.defineProperty(window, 'getSelection', {
          configurable: true,
          value: originalGetSelection,
        })
      } else {
        delete (window as any).getSelection
      }

      if (hadSpeechSynthesis) {
        Object.defineProperty(window, 'speechSynthesis', {
          configurable: true,
          value: originalSpeechSynthesis,
        })
      } else {
        delete (window as any).speechSynthesis
      }

      if (hadSpeechSynthesisUtterance) {
        Object.defineProperty(window, 'SpeechSynthesisUtterance', {
          configurable: true,
          value: originalSpeechSynthesisUtterance,
        })
      } else {
        delete (window as any).SpeechSynthesisUtterance
      }
    }
  })

  it('reads the current TXT paragraph aloud when browser speech is available', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const speak = jest.fn()
    const cancel = jest.fn()
    const hadSpeechSynthesis = 'speechSynthesis' in window
    const originalSpeechSynthesis = window.speechSynthesis
    const hadSpeechSynthesisUtterance = 'SpeechSynthesisUtterance' in window
    const originalSpeechSynthesisUtterance = window.SpeechSynthesisUtterance

    class TestSpeechSynthesisUtterance {
      text: string
      onend: (() => void) | null = null
      onerror: (() => void) | null = null

      constructor(text: string) {
        this.text = text
      }
    }

    Object.defineProperty(window, 'speechSynthesis', {
      configurable: true,
      value: {
        cancel,
        speak,
      },
    })
    Object.defineProperty(window, 'SpeechSynthesisUtterance', {
      configurable: true,
      value: TestSpeechSynthesisUtterance,
    })

    try {
      const wrapper = mountShell({getManifest})

      await flushPromises()
      await Vue.nextTick()

      await wrapper.find('[data-testid="shelfview-reader-text-settings-toggle"]').trigger('click')
      await Vue.nextTick()
      expect(wrapper.find('[data-testid="shelfview-reader-tts-rate"]').exists()).toBe(true)
      await wrapper.setData({textToSpeechRate: 1.3})
      await Vue.nextTick()

      cancel.mockClear()
      speak.mockClear()

      await wrapper.find('[data-testid="shelfview-reader-tts-toggle"]').trigger('click')
      await Vue.nextTick()

      expect(cancel).toHaveBeenCalledTimes(1)
      expect(speak).toHaveBeenCalledWith(expect.objectContaining({
        text: textContent.paragraphs[0],
        rate: 1.3,
      }))

      await wrapper.find('[data-testid="shelfview-reader-tts-toggle"]').trigger('click')
      await Vue.nextTick()

      expect(cancel).toHaveBeenCalledTimes(2)
    } finally {
      if (hadSpeechSynthesis) {
        Object.defineProperty(window, 'speechSynthesis', {
          configurable: true,
          value: originalSpeechSynthesis,
        })
      } else {
        delete (window as any).speechSynthesis
      }

      if (hadSpeechSynthesisUtterance) {
        Object.defineProperty(window, 'SpeechSynthesisUtterance', {
          configurable: true,
          value: originalSpeechSynthesisUtterance,
        })
      } else {
        delete (window as any).SpeechSynthesisUtterance
      }
    }
  })

  it('uses the selected browser voice for TXT read-aloud', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const voice = {
      voiceURI: 'ko-voice',
      name: 'Korean Voice',
      lang: 'ko-KR',
    } as SpeechSynthesisVoice
    const speak = jest.fn()
    const cancel = jest.fn()
    const getVoices = jest.fn(() => [voice])
    const addEventListener = jest.fn()
    const removeEventListener = jest.fn()
    const hadSpeechSynthesis = 'speechSynthesis' in window
    const originalSpeechSynthesis = window.speechSynthesis
    const hadSpeechSynthesisUtterance = 'SpeechSynthesisUtterance' in window
    const originalSpeechSynthesisUtterance = window.SpeechSynthesisUtterance

    class TestSpeechSynthesisUtterance {
      text: string
      voice?: SpeechSynthesisVoice
      rate = 1
      onend: (() => void) | null = null
      onerror: (() => void) | null = null

      constructor(text: string) {
        this.text = text
      }
    }

    Object.defineProperty(window, 'speechSynthesis', {
      configurable: true,
      value: {
        addEventListener,
        cancel,
        getVoices,
        removeEventListener,
        speak,
      },
    })
    Object.defineProperty(window, 'SpeechSynthesisUtterance', {
      configurable: true,
      value: TestSpeechSynthesisUtterance,
    })

    try {
      const wrapper = mountShell({getManifest})

      await flushPromises()
      await Vue.nextTick()

      await wrapper.find('[data-testid="shelfview-reader-text-settings-toggle"]').trigger('click')
      await Vue.nextTick()

      expect(addEventListener).toHaveBeenCalledWith('voiceschanged', expect.any(Function))
      expect(wrapper.find('[data-testid="shelfview-reader-tts-voice"]').exists()).toBe(true)

      await wrapper.setData({textToSpeechVoiceURI: 'ko-voice'})
      await wrapper.find('[data-testid="shelfview-reader-tts-toggle"]').trigger('click')
      await Vue.nextTick()

      expect(speak).toHaveBeenCalledWith(expect.objectContaining({
        text: textContent.paragraphs[0],
        voice,
      }))
    } finally {
      if (hadSpeechSynthesis) {
        Object.defineProperty(window, 'speechSynthesis', {
          configurable: true,
          value: originalSpeechSynthesis,
        })
      } else {
        delete (window as any).speechSynthesis
      }

      if (hadSpeechSynthesisUtterance) {
        Object.defineProperty(window, 'SpeechSynthesisUtterance', {
          configurable: true,
          value: originalSpeechSynthesisUtterance,
        })
      } else {
        delete (window as any).SpeechSynthesisUtterance
      }
    }
  })

  it('restores saved position, bookmarks and effective settings', async () => {
    const getManifest = jest.fn().mockResolvedValue({...manifest, pageCount: 4})
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {
        getPosition: jest.fn().mockResolvedValue({
          bookId: 'book-1',
          locator: pageLocator(1),
          progression: 0.5,
          contentFingerprint: manifest.contentFingerprint,
          updatedAt: '2026-06-18T00:00:00Z',
        }),
        getBookmarks: jest.fn().mockResolvedValue([
          {id: 'bookmark-1', bookId: 'book-1', locator: pageLocator(1), label: 'Saved page', createdAt: 'now', updatedAt: 'now'},
        ]),
        getPreference: jest.fn().mockResolvedValue({
          scopeType: 'BOOK',
          scopeId: 'book-1',
          format: 'FIXED_PAGE_SEQUENCE',
          effectiveSettings: {
            layout: 'DOUBLE_PAGE',
            readingDirection: 'RTL',
            pageFit: 'WIDTH',
            imageFilterPreset: 'MONO_COMIC',
            fixedPageRotation: 90,
            fixedPageScale: 125,
            fixedPageGap: 24,
            fixedPageMargin: 32,
            fixedPageBackground: 'PAPER',
            fixedPageBrightness: 1.08,
            fixedPageContrast: 1.12,
          },
          sources: [{
            scopeType: 'BOOK',
            scopeId: 'book-1',
            format: 'FIXED_PAGE_SEQUENCE',
            settings: {pageFit: 'WIDTH'},
            updatedAt: '2026-06-18T00:00:00Z',
          }],
        }),
      },
    })

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.findAll('[data-testid="shelfview-reader-page"]').wrappers.map(page => page.attributes('data-page-index'))).toEqual(['1', '0'])
    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1-2 / 4')
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').classes()).toContain('shelfview-reader-shell__page--fit-width')
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('style')).toContain('--sv-grayscale: 1;')
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('style')).toContain('--sv-brightness: 1.08;')
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('style')).toContain('--sv-fixed-page-scale: 1.25;')
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('style')).toContain('--sv-fixed-page-rotation: 90deg;')
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('style')).toContain('--sv-fixed-background: #f4ecd9;')

    await wrapper.find('[data-testid="shelfview-reader-bookmarks-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-bookmarks-drawer"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-bookmark-go"]').text()).toBe('Saved page')
  })

  it('uses default screen fit only when no saved page fit exists', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})

    await flushPromises()
    await Vue.nextTick()

    const vm = wrapper.vm as any
    vm.pageFit = 'WIDTH'
    vm.applyPreferenceSettings(vm.settingsWithFormatDefaults({
      scopeType: 'BOOK',
      scopeId: 'book-1',
      format: 'FIXED_PAGE_SEQUENCE',
      effectiveSettings: {},
      sources: [],
    }), false, 'FIXED_PAGE_SEQUENCE')

    expect(vm.pageFit).toBe('SCREEN')

    vm.applyPreferenceSettings(vm.settingsWithFormatDefaults({
      scopeType: 'BOOK',
      scopeId: 'book-1',
      format: 'FIXED_PAGE_SEQUENCE',
      effectiveSettings: {pageFit: 'WIDTH'},
      sources: [{
        scopeType: 'BOOK',
        scopeId: 'book-1',
        format: 'FIXED_PAGE_SEQUENCE',
        settings: {pageFit: 'WIDTH'},
        updatedAt: '2026-06-18T00:00:00Z',
      }],
    }), false, 'FIXED_PAGE_SEQUENCE')

    expect(vm.pageFit).toBe('WIDTH')
  })

  it('applies and saves fixed page display settings', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const savePreference = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {
        getPreference: jest.fn().mockResolvedValue({
          scopeType: 'BOOK',
          scopeId: 'book-1',
          format: 'FIXED_PAGE_SEQUENCE',
          effectiveSettings: {
            layout: 'SINGLE_PAGE',
            readingDirection: 'LTR',
            pageFit: 'SCREEN',
            imageFilterPreset: 'ORIGINAL',
            fixedPageRotation: 0,
            fixedPageScale: 130,
            fixedPageGap: 18,
            fixedPageMargin: 28,
            fixedPageBackground: 'GRAY',
            fixedPageBrightness: 1.06,
            fixedPageContrast: 1.14,
          },
          sources: [],
        }),
        savePreference,
      },
    })

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-fixed-settings-toggle"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('style')).toContain('--sv-fixed-page-scale: 1.3;')
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('style')).toContain('--sv-fixed-page-gap: 18px;')
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('style')).toContain('--sv-brightness: 1.06;')
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('style')).toContain('--sv-contrast: 1.14;')

    await wrapper.find('[data-testid="shelfview-reader-fixed-settings-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-fixed-settings"]').exists()).toBe(true)

    await wrapper.setData({
      fixedPageScale: 145,
      fixedPageGap: 28,
      fixedPageMargin: 44,
      fixedPageBackground: 'WHITE',
      fixedPageRotation: 180,
      fixedPageBrightness: 1.12,
      fixedPageContrast: 1.2,
    })
    const vm = wrapper.vm as any
    vm.onFixedPageDisplaySettingChanged()
    await Vue.nextTick()
    await vm.flushPreferenceSave()

    expect(savePreference).toHaveBeenCalledWith({
      scopeType: 'BOOK',
      scopeId: 'book-1',
      format: 'FIXED_PAGE_SEQUENCE',
      settings: expect.objectContaining({
        layout: 'SINGLE_PAGE',
        readingDirection: 'LTR',
        pageFit: 'SCREEN',
        imageFilterPreset: 'ORIGINAL',
        fixedPageRotation: 180,
        fixedPageScale: 145,
        fixedPageGap: 28,
        fixedPageMargin: 44,
        fixedPageBackground: 'WHITE',
        fixedPageBrightness: 1.12,
        fixedPageContrast: 1.2,
      }),
    })
  })

  it('rotates fixed pages from the toolbar and keyboard shortcut', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const savePreference = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({getManifest, shelfViewState: {savePreference}})

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-rotate-toggle"]').exists()).toBe(true)

    await chooseControlOption(wrapper, 'shelfview-reader-rotate-toggle', 'shelfview-reader-rotate-option-90')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('style')).toContain('--sv-fixed-page-rotation: 90deg;')

    const preventDefault = jest.fn()
    await (wrapper.vm as any).onReaderKeydown({
      key: 'R',
      ctrlKey: false,
      altKey: false,
      metaKey: false,
      target: wrapper.find('[data-testid="shelfview-reader-stage"]').element,
      preventDefault,
    })
    await Vue.nextTick()
    await (wrapper.vm as any).flushPreferenceSave()

    expect(preventDefault).toHaveBeenCalled()
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('style')).toContain('--sv-fixed-page-rotation: 180deg;')
    expect(savePreference).toHaveBeenCalledWith(expect.objectContaining({
      settings: expect.objectContaining({fixedPageRotation: 180}),
    }))
  })

  it('jumps with thumbnails, direct page input and slideshow controls', async () => {
    const getManifest = jest.fn().mockResolvedValue({
      ...manifest,
      pageCount: 3,
      pages: {
        ...manifest.pages!,
        thumbnailEndpointTemplate: '/api/v1/books/book-1/pages/{pageOneBased}/thumbnail',
      },
    })
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-thumbnail-strip"]').exists()).toBe(false)
    await wrapper.setData({chromeVisible: true, overflowMenuOpen: true})
    await wrapper.find('[data-testid="shelfview-reader-thumbnails-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.findAll('[data-testid="shelfview-reader-thumbnail"]').length).toBe(3)
    expect(wrapper.find('[data-testid="shelfview-reader-thumbnail"] img').attributes('src')).toBe('http://api.test/api/v1/books/book-1/pages/1/thumbnail')

    await wrapper.findAll('[data-testid="shelfview-reader-thumbnail"]').at(2).trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('3 / 3')

    await wrapper.find('[data-testid="shelfview-reader-page-jump"]').setValue('2')
    await wrapper.find('[data-testid="shelfview-reader-page-jump"]').trigger('change')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('2 / 3')

    await wrapper.find('[data-testid="shelfview-reader-slideshow-toggle"]').trigger('click')
    await (wrapper.vm as any).advanceSlideshow()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('3 / 3')
  })

  it('defers off-screen page images in continuous scroll and keeps thumbnails lazy', async () => {
    const getManifest = jest.fn().mockResolvedValue({
      ...manifest,
      pageCount: 3,
      pages: {
        ...manifest.pages!,
        thumbnailEndpointTemplate: '/api/v1/books/book-1/pages/{pageOneBased}/thumbnail',
      },
    })
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    const singlePage = wrapper.find('[data-testid="shelfview-reader-page"]')
    expect(singlePage.attributes('loading')).toBe('eager')
    expect(singlePage.attributes('decoding')).toBe('async')

    await wrapper.setData({chromeVisible: true, overflowMenuOpen: true})
    await chooseControlOption(wrapper, 'shelfview-reader-layout-toggle', 'shelfview-reader-layout-option-CONTINUOUS_SCROLL')
    await Vue.nextTick()

    const continuousPages = wrapper.findAll('[data-testid="shelfview-reader-page"]')
    expect(continuousPages.length).toBe(3)
    expect(continuousPages.at(0).attributes('loading')).toBe('lazy')
    expect(continuousPages.at(2).attributes('loading')).toBe('lazy')

    await wrapper.setData({chromeVisible: true, overflowMenuOpen: true})
    await wrapper.find('[data-testid="shelfview-reader-thumbnails-toggle"]').trigger('click')
    await Vue.nextTick()

    const thumbnail = wrapper.find('[data-testid="shelfview-reader-thumbnail-image"]')
    expect(thumbnail.attributes('loading')).toBe('lazy')
    expect(thumbnail.attributes('decoding')).toBe('async')
  })

  it('zooms fixed pages with the mouse wheel and keeps the pointer area anchored', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const savePreference = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({getManifest, shelfViewState: {savePreference}})

    await flushPromises()
    await Vue.nextTick()

    const stage = wrapper.find('[data-testid="shelfview-reader-stage"]').element as HTMLElement
    stage.getBoundingClientRect = jest.fn(() => ({top: 10, bottom: 410, left: 20, right: 620, width: 600, height: 400} as DOMRect))
    Object.defineProperty(stage, 'scrollLeft', {value: 40, writable: true, configurable: true})
    Object.defineProperty(stage, 'scrollTop', {value: 60, writable: true, configurable: true})
    const preventDefault = jest.fn()
    const vm = wrapper.vm as any

    vm.onReaderWheel({
      deltaY: -120,
      clientX: 120,
      clientY: 90,
      ctrlKey: false,
      metaKey: false,
      cancelable: true,
      target: stage,
      preventDefault,
    } as unknown as WheelEvent)
    await Vue.nextTick()
    await vm.flushPreferenceSave()

    expect(preventDefault).toHaveBeenCalled()
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('style')).toContain('--sv-fixed-page-scale: 1.1;')
    expect(stage.scrollLeft).toBeCloseTo(54)
    expect(stage.scrollTop).toBeCloseTo(74)
    expect(savePreference).toHaveBeenCalledWith(expect.objectContaining({
      settings: expect.objectContaining({fixedPageScale: 110}),
    }))
  })

  it('pans fixed pages by dragging with the mouse', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    const stage = wrapper.find('[data-testid="shelfview-reader-stage"]').element as HTMLElement
    Object.defineProperty(stage, 'scrollLeft', {value: 70, writable: true, configurable: true})
    Object.defineProperty(stage, 'scrollTop', {value: 25, writable: true, configurable: true})
    // Panning only starts on a page that overflows the stage.
    Object.defineProperty(stage, 'clientWidth', {value: 1000, configurable: true})
    Object.defineProperty(stage, 'scrollWidth', {value: 2400, configurable: true})
    stage.setPointerCapture = jest.fn()
    stage.releasePointerCapture = jest.fn()
    const downPreventDefault = jest.fn()
    const movePreventDefault = jest.fn()
    const vm = wrapper.vm as any

    vm.onReaderPointerDown({
      pointerType: 'mouse',
      button: 0,
      pointerId: 7,
      clientX: 120,
      clientY: 90,
      cancelable: true,
      target: stage,
      preventDefault: downPreventDefault,
    } as unknown as PointerEvent)
    vm.onReaderPointerMove({
      pointerType: 'mouse',
      buttons: 1,
      pointerId: 7,
      clientX: 90,
      clientY: 70,
      cancelable: true,
      preventDefault: movePreventDefault,
    } as unknown as PointerEvent)

    expect(downPreventDefault).toHaveBeenCalled()
    expect(movePreventDefault).toHaveBeenCalled()
    expect(stage.setPointerCapture).toHaveBeenCalledWith(7)
    expect(stage.scrollLeft).toBe(100)
    expect(stage.scrollTop).toBe(45)
    expect(vm.fixedPagePanning).toBe(true)

    vm.onReaderPointerUp({pointerId: 7} as PointerEvent)

    expect(stage.releasePointerCapture).toHaveBeenCalledWith(7)
    expect(vm.fixedPagePanning).toBe(false)
  })

  it('moves between fixed pages with adapter controls', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-next"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('src')).toBe('http://api.test/api/v1/books/book-1/pages/1?zero_based=true')
    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('2 / 2')
    expect(wrapper.find('[data-testid="shelfview-reader-next"]').attributes('disabled')).toBe('disabled')

    await wrapper.find('[data-testid="shelfview-reader-previous"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('src')).toBe('http://api.test/api/v1/books/book-1/pages/0?zero_based=true')
    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1 / 2')
  })

  it('keeps the current continuous page visible after mobile orientation changes', async () => {
    const getManifest = jest.fn().mockResolvedValue({...manifest, pageCount: 3})
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await chooseControlOption(wrapper, 'shelfview-reader-layout-toggle', 'shelfview-reader-layout-option-CONTINUOUS_SCROLL')
    await Vue.nextTick()
    await wrapper.find('[data-testid="shelfview-reader-next"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    const currentPage = wrapper
      .findAll('[data-testid="shelfview-reader-page"]')
      .wrappers
      .find(page => page.attributes('data-page-index') === '1')
    expect(currentPage).toBeTruthy()

    const scrollIntoView = jest.fn()
    Object.defineProperty(currentPage!.element, 'scrollIntoView', {value: scrollIntoView, configurable: true})

    window.dispatchEvent(new Event('orientationchange'))
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('2 / 3')
    expect(scrollIntoView).toHaveBeenCalledWith({block: 'start', inline: 'nearest'})
  })

  it('moves pages with keyboard shortcuts and preserves editable target input', async () => {
    const getManifest = jest.fn().mockResolvedValue({...manifest, pageCount: 3})
    const wrapper = mountShell({getManifest})
    const preventDefault = jest.fn()

    await flushPromises()
    await Vue.nextTick()

    await (wrapper.vm as any).onReaderKeydown({
      key: 'ArrowRight',
      ctrlKey: false,
      altKey: false,
      metaKey: false,
      target: wrapper.find('[data-testid="shelfview-reader-stage"]').element,
      preventDefault,
    })
    await flushPromises()
    await Vue.nextTick()

    expect(preventDefault).toHaveBeenCalled()
    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('2 / 3')

    window.dispatchEvent(new KeyboardEvent('keydown', {
      key: 'ArrowRight',
      bubbles: true,
      cancelable: true,
    }))
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('3 / 3')

    await (wrapper.vm as any).onReaderKeydown({
      key: 'Home',
      ctrlKey: false,
      altKey: false,
      metaKey: false,
      target: wrapper.find('[data-testid="shelfview-reader-stage"]').element,
      preventDefault,
    })
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1 / 3')

    await (wrapper.vm as any).onReaderKeydown({
      key: 'End',
      ctrlKey: false,
      altKey: false,
      metaKey: false,
      target: document.createElement('input'),
      preventDefault,
    })
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1 / 3')
  })

  it('toggles reader chrome with Escape only when no chrome panel is open', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})

    await flushPromises()
    await Vue.nextTick()

    const preventDefault = jest.fn()
    await (wrapper.vm as any).onReaderKeydown({
      key: 'Escape',
      ctrlKey: false,
      altKey: false,
      metaKey: false,
      target: wrapper.find('[data-testid="shelfview-reader-stage"]').element,
      preventDefault,
    })
    await Vue.nextTick()

    expect(preventDefault).toHaveBeenCalled()
    expect((wrapper.vm as any).chromeVisible).toBe(false)

    const vm = wrapper.vm as any
    vm.chromeVisible = true
    vm.openControlMenu = 'layout'
    await Vue.nextTick()

    const panelPreventDefault = jest.fn()
    await (wrapper.vm as any).onReaderKeydown({
      key: 'Escape',
      ctrlKey: false,
      altKey: false,
      metaKey: false,
      target: wrapper.find('[data-testid="shelfview-reader-stage"]').element,
      preventDefault: panelPreventDefault,
    })
    await Vue.nextTick()

    expect(panelPreventDefault).not.toHaveBeenCalled()
    expect((wrapper.vm as any).chromeVisible).toBe(true)
  })

  it('shows the fullscreen toggle and requests fullscreen from the toolbar when supported', async () => {
    mockScreenfull.isEnabled = true
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})

    await flushPromises()
    await Vue.nextTick()

    expect(mockScreenfull.on).toHaveBeenCalledWith('change', expect.any(Function))
    const toggle = wrapper.find('[data-testid="shelfview-reader-fullscreen-toggle"]')
    expect(toggle.exists()).toBe(true)

    await toggle.trigger('click')
    expect(mockScreenfull.request).toHaveBeenCalled()

    const changeHandler = mockScreenfull.on.mock.calls.find((call: any[]) => call[0] === 'change')![1]
    mockScreenfull.isFullscreen = true
    changeHandler()
    await Vue.nextTick()

    expect((wrapper.vm as any).isFullscreen).toBe(true)
    // Going fullscreen must not take the toolbar away with it.
    expect((wrapper.vm as any).chromeVisible).toBe(true)

    await wrapper.find('[data-testid="shelfview-reader-fullscreen-toggle"]').trigger('click')
    expect(mockScreenfull.exit).toHaveBeenCalled()
  })

  it('toggles fullscreen with the F keyboard shortcut', async () => {
    mockScreenfull.isEnabled = true
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})

    await flushPromises()
    await Vue.nextTick()

    const preventDefault = jest.fn()
    await (wrapper.vm as any).onReaderKeydown({
      key: 'f',
      ctrlKey: false,
      altKey: false,
      metaKey: false,
      target: wrapper.find('[data-testid="shelfview-reader-stage"]').element,
      preventDefault,
    })
    await flushPromises()

    expect(preventDefault).toHaveBeenCalled()
    expect(mockScreenfull.request).toHaveBeenCalled()
  })

  it('hides the fullscreen toggle when the browser does not support fullscreen', async () => {
    mockScreenfull.isEnabled = false
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-fullscreen-toggle"]').exists()).toBe(false)
    expect(mockScreenfull.on).not.toHaveBeenCalled()
  })

  it('opens and closes shortcut help from keyboard and button', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await (wrapper.vm as any).onReaderKeydown({
      key: '?',
      ctrlKey: false,
      altKey: false,
      metaKey: false,
      target: wrapper.find('[data-testid="shelfview-reader-stage"]').element,
      preventDefault: jest.fn(),
    })
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-shortcuts"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-shortcuts"]').attributes('role')).toBe('dialog')
    expect(wrapper.find('[data-testid="shelfview-reader-shortcuts"]').text()).toContain('shelfview_reader.shortcut_keys_escape')
    expect(wrapper.find('[data-testid="shelfview-reader-shortcuts"]').text()).toContain('shelfview_reader.shortcut_toggle_chrome')
    expect(wrapper.find('[data-testid="shelfview-reader-shortcuts"]').text()).toContain('shelfview_reader.shortcut_keys_side_tap')
    expect(wrapper.find('[data-testid="shelfview-reader-shortcuts"]').text()).toContain('shelfview_reader.shortcut_side_tap_pages')
    expect(wrapper.find('[data-testid="shelfview-reader-shortcuts"]').text()).toContain('shelfview_reader.shortcut_keys_center_tap')
    expect(wrapper.find('[data-testid="shelfview-reader-shortcuts"]').text()).toContain('shelfview_reader.shortcut_center_tap_chrome')

    await wrapper.find('[data-testid="shelfview-reader-shortcuts-close"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-shortcuts"]').exists()).toBe(false)

    await wrapper.find('[data-testid="shelfview-reader-shortcuts-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-shortcuts"]').exists()).toBe(true)
  })

  it('moves pages with horizontal touch swipes', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await (wrapper.vm as any).onReaderTouchStart({
      changedTouches: [{clientX: 220, clientY: 100}],
    })
    await (wrapper.vm as any).onReaderTouchEnd({
      changedTouches: [{clientX: 40, clientY: 104}],
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('2 / 2')
  })

  it('toggles reader chrome with fixed-page center mouse taps', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})

    await flushPromises()
    await Vue.nextTick()

    const stage = wrapper.find('[data-testid="shelfview-reader-stage"]')
    Object.defineProperty(stage.element, 'getBoundingClientRect', {
      value: () => ({left: 0, top: 0, right: 1000, bottom: 700, width: 1000, height: 700}),
      configurable: true,
    })

    await (wrapper.vm as any).onReaderPointerDown({
      pointerType: 'mouse',
      button: 0,
      pointerId: 7,
      clientX: 500,
      clientY: 120,
      target: stage.element,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await (wrapper.vm as any).onReaderPointerUp({
      pointerId: 7,
      clientX: 504,
      clientY: 126,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await Vue.nextTick()

    expect((wrapper.vm as any).chromeVisible).toBe(false)

    await (wrapper.vm as any).onReaderPointerDown({
      pointerType: 'mouse',
      button: 0,
      pointerId: 8,
      clientX: 500,
      clientY: 120,
      target: stage.element,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await (wrapper.vm as any).onReaderPointerUp({
      pointerId: 8,
      clientX: 504,
      clientY: 126,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await Vue.nextTick()

    expect((wrapper.vm as any).chromeVisible).toBe(true)
  })

  it('still treats a drifting center click as a chrome toggle', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})
    const vm = wrapper.vm as any

    await flushPromises()
    await Vue.nextTick()

    const stage = setReaderStageRect(wrapper)

    await vm.onReaderPointerDown({
      pointerType: 'mouse',
      button: 0,
      pointerId: 21,
      clientX: 500,
      clientY: 120,
      target: stage.element,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    vm.onReaderPointerMove({
      pointerId: 21,
      buttons: 1,
      clientX: 505,
      clientY: 123,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await vm.onReaderPointerUp({
      pointerId: 21,
      clientX: 505,
      clientY: 123,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await Vue.nextTick()

    expect(vm.fixedPagePanning).toBe(false)
    expect(vm.chromeVisible).toBe(false)
  })

  it('pans instead of tapping when the page is larger than the stage', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})
    const vm = wrapper.vm as any

    await flushPromises()
    await Vue.nextTick()

    const stage = setReaderStageRect(wrapper)
    Object.defineProperty(stage.element, 'clientWidth', {value: 1000, configurable: true})
    Object.defineProperty(stage.element, 'scrollWidth', {value: 2400, configurable: true})

    await vm.onReaderPointerDown({
      pointerType: 'mouse',
      button: 0,
      pointerId: 22,
      clientX: 500,
      clientY: 120,
      target: stage.element,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    vm.onReaderPointerMove({
      pointerId: 22,
      buttons: 1,
      clientX: 460,
      clientY: 120,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await vm.onReaderPointerUp({
      pointerId: 22,
      clientX: 460,
      clientY: 120,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await Vue.nextTick()

    expect(stage.element.scrollLeft).toBe(40)
    // Panning must leave the toolbar exactly as it was rather than count as a tap.
    expect(vm.chromeVisible).toBe(true)
  })

  it('toggles reader chrome with a touch center tap without moving pages', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})

    await flushPromises()
    await Vue.nextTick()

    const stage = wrapper.find('[data-testid="shelfview-reader-stage"]')
    Object.defineProperty(stage.element, 'getBoundingClientRect', {
      value: () => ({left: 0, top: 0, right: 1000, bottom: 700, width: 1000, height: 700}),
      configurable: true,
    })

    await (wrapper.vm as any).onReaderTouchStart({
      changedTouches: [{clientX: 500, clientY: 120}],
    })
    await (wrapper.vm as any).onReaderTouchEnd({
      changedTouches: [{clientX: 504, clientY: 126}],
      cancelable: true,
      preventDefault: jest.fn(),
      target: stage.element,
    })
    await Vue.nextTick()

    expect((wrapper.vm as any).chromeVisible).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1 / 2')
  })

  it('moves fixed pages with left and right mouse tap zones', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue({...manifest, pageCount: 3})})

    await flushPromises()
    await Vue.nextTick()

    const stage = setReaderStageRect(wrapper)

    await (wrapper.vm as any).onReaderPointerDown({
      pointerType: 'mouse',
      button: 0,
      pointerId: 10,
      clientX: 900,
      clientY: 120,
      target: stage.element,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await (wrapper.vm as any).onReaderPointerUp({
      pointerId: 10,
      clientX: 904,
      clientY: 126,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('2 / 3')

    await (wrapper.vm as any).onReaderPointerDown({
      pointerType: 'mouse',
      button: 0,
      pointerId: 11,
      clientX: 100,
      clientY: 120,
      target: stage.element,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await (wrapper.vm as any).onReaderPointerUp({
      pointerId: 11,
      clientX: 104,
      clientY: 126,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1 / 3')
  })

  it('does not move fixed pages from side tap zones while zoomed', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue({...manifest, pageCount: 3})})

    await flushPromises()
    await Vue.nextTick()

    const stage = setReaderStageRect(wrapper)
    await wrapper.setData({fixedPageScale: 125})

    await (wrapper.vm as any).onReaderPointerDown({
      pointerType: 'mouse',
      button: 0,
      pointerId: 12,
      clientX: 900,
      clientY: 120,
      target: stage.element,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await (wrapper.vm as any).onReaderPointerUp({
      pointerId: 12,
      clientX: 904,
      clientY: 126,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1 / 3')
  })

  it('toggles reader chrome from text mouse center taps without side paging', async () => {
    const wrapper = mountShell({
      getManifest: jest.fn().mockResolvedValue(textManifest),
      shelfViewState: {
        getPreference: jest.fn().mockResolvedValue({
          scopeType: 'BOOK',
          scopeId: 'book-1',
          format: 'TEXT_REFLOW',
          effectiveSettings: {},
          sources: [],
        }),
      },
    })

    await flushPromises()
    await Vue.nextTick()

    const stage = setReaderStageRect(wrapper)

    await (wrapper.vm as any).onReaderPointerDown({
      pointerType: 'mouse',
      button: 0,
      pointerId: 13,
      clientX: 900,
      clientY: 120,
      target: stage.element,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await (wrapper.vm as any).onReaderPointerUp({
      pointerId: 13,
      clientX: 904,
      clientY: 126,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1 / 3')
    // A side tap in text is not a paging zone, so it leaves the toolbar alone.
    expect((wrapper.vm as any).chromeVisible).toBe(true)

    await (wrapper.vm as any).onReaderPointerDown({
      pointerType: 'mouse',
      button: 0,
      pointerId: 14,
      clientX: 500,
      clientY: 120,
      target: stage.element,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await (wrapper.vm as any).onReaderPointerUp({
      pointerId: 14,
      clientX: 504,
      clientY: 126,
      cancelable: true,
      preventDefault: jest.fn(),
    })
    await Vue.nextTick()

    expect((wrapper.vm as any).chromeVisible).toBe(false)
  })

  it('debounces position saving and flushes pending saves', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const savePosition = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {savePosition},
    })

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-next"]').trigger('click')
    await Vue.nextTick()

    expect(savePosition).not.toHaveBeenCalled()
    expect(wrapper.find('[data-testid="shelfview-reader-sync-status"]').text()).toBe('shelfview_reader.status_saving')

    await (wrapper.vm as any).flushPositionSave()
    await Vue.nextTick()

    expect(savePosition).toHaveBeenCalledTimes(1)
    expect(savePosition).toHaveBeenCalledWith('book-1', {
      locator: pageLocator(1),
      progression: 1,
    })
    expect(wrapper.find('[data-testid="shelfview-reader-sync-status"]').text()).toBe('shelfview_reader.status_saved')
  })

  it('renders double page spreads and keeps the current location in the spread', async () => {
    const getManifest = jest.fn().mockResolvedValue({...manifest, pageCount: 4})
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await chooseControlOption(wrapper, 'shelfview-reader-layout-toggle', 'shelfview-reader-layout-option-DOUBLE_PAGE')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.findAll('[data-testid="shelfview-reader-page"]').wrappers.map(page => page.attributes('data-page-index'))).toEqual(['0', '1'])
    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1-2 / 4')

    await wrapper.find('[data-testid="shelfview-reader-next"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.findAll('[data-testid="shelfview-reader-page"]').wrappers.map(page => page.attributes('data-page-index'))).toEqual(['2', '3'])
    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('3-4 / 4')
    expect(wrapper.find('[data-testid="shelfview-reader-next"]').attributes('disabled')).toBe('disabled')
  })

  it('reverses double page image order in RTL mode', async () => {
    const getManifest = jest.fn().mockResolvedValue({...manifest, pageCount: 4})
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await chooseControlOption(wrapper, 'shelfview-reader-layout-toggle', 'shelfview-reader-layout-option-DOUBLE_PAGE')
    await chooseControlOption(wrapper, 'shelfview-reader-direction-toggle', 'shelfview-reader-direction-option-RTL')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.findAll('[data-testid="shelfview-reader-page"]').wrappers.map(page => page.attributes('data-page-index'))).toEqual(['1', '0'])
  })

  it('renders all pages in continuous scroll mode', async () => {
    const getManifest = jest.fn().mockResolvedValue({...manifest, pageCount: 3})
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await chooseControlOption(wrapper, 'shelfview-reader-layout-toggle', 'shelfview-reader-layout-option-CONTINUOUS_SCROLL')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-pages"]').classes()).toContain('shelfview-reader-shell__pages--continuous')
    expect(wrapper.findAll('[data-testid="shelfview-reader-page"]').wrappers.map(page => page.attributes('data-page-index'))).toEqual(['0', '1', '2'])
  })

  it('opens the end prompt when scrolling down again at the bottom of continuous scroll', async () => {
    const getManifest = jest.fn().mockResolvedValue({...manifest, pageCount: 3})
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await chooseControlOption(wrapper, 'shelfview-reader-layout-toggle', 'shelfview-reader-layout-option-CONTINUOUS_SCROLL')
    await (wrapper.vm as any).goToPageIndex(2)
    await flushPromises()
    await Vue.nextTick()

    const stage = wrapper.find('[data-testid="shelfview-reader-stage"]').element as HTMLElement
    Object.defineProperty(stage, 'clientHeight', {value: 500, configurable: true})
    Object.defineProperty(stage, 'scrollHeight', {value: 1500, configurable: true})
    Object.defineProperty(stage, 'scrollTop', {value: 800, writable: true, configurable: true})
    const preventDefault = jest.fn()
    const vm = wrapper.vm as any

    vm.onReaderWheel({
      deltaY: 90,
      ctrlKey: false,
      metaKey: false,
      cancelable: true,
      target: stage,
      preventDefault,
    } as unknown as WheelEvent)
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-end-prompt"]').exists()).toBe(false)

    stage.scrollTop = 1000
    vm.onReaderWheel({
      deltaY: 90,
      ctrlKey: false,
      metaKey: false,
      cancelable: true,
      target: stage,
      preventDefault,
    } as unknown as WheelEvent)
    await Vue.nextTick()

    expect(preventDefault).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-testid="shelfview-reader-end-prompt"]').exists()).toBe(true)
  })

  it('cycles page fit classes', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page"]').classes()).toContain('shelfview-reader-shell__page--fit-screen')

    await chooseControlOption(wrapper, 'shelfview-reader-fit-toggle', 'shelfview-reader-fit-option-WIDTH')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page"]').classes()).toContain('shelfview-reader-shell__page--fit-width')
  })

  it('saves selected page fit as the global fixed-page default', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const savePreference = jest.fn().mockResolvedValue({})
    const wrapper = mountShell({getManifest, shelfViewState: {savePreference}})

    await flushPromises()
    await Vue.nextTick()

    await chooseControlOption(wrapper, 'shelfview-reader-fit-toggle', 'shelfview-reader-fit-option-HEIGHT')
    await Vue.nextTick()
    await (wrapper.vm as any).flushPreferenceSave()

    expect(savePreference).toHaveBeenCalledWith(expect.objectContaining({
      scopeType: 'BOOK',
      scopeId: 'book-1',
      format: 'FIXED_PAGE_SEQUENCE',
      settings: expect.objectContaining({pageFit: 'HEIGHT'}),
    }))
    expect(savePreference).toHaveBeenCalledWith({
      scopeType: 'GLOBAL',
      scopeId: 'GLOBAL',
      format: 'FIXED_PAGE_SEQUENCE',
      settings: {pageFit: 'HEIGHT'},
    })
  })

  it('shows only primary image filter options in the simplified menu', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-more-toggle"]').trigger('click')
    await Vue.nextTick()
    await wrapper.find('[data-testid="shelfview-reader-filter-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-filter-option-ORIGINAL"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-filter-option-IMAGE_ENHANCE"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-filter-option-MONO_COMIC"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-filter-option-NIGHT_INVERT"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-filter-option-CANVAS_SHARPEN"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-filter-option-FADED_SCAN"]').exists()).toBe(false)
  })

  it('applies image filter presets only to page content', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    const originalPage = wrapper.find('[data-testid="shelfview-reader-page"]')
    expect(originalPage.classes()).toContain('shelfview-page-content')
    expect(originalPage.attributes('style')).toContain('--sv-brightness: 1;')
    expect(wrapper.find('[data-testid="shelfview-reader-toolbar"]').classes()).not.toContain('shelfview-page-content')

    await chooseControlOption(wrapper, 'shelfview-reader-filter-toggle', 'shelfview-reader-filter-option-IMAGE_ENHANCE')
    await flushPromises()
    await Vue.nextTick()

    const filteredPage = wrapper.find('[data-testid="shelfview-reader-page"]')
    expect(filteredPage.attributes('src')).toBe('http://api.test/api/v1/books/book-1/pages/0?zero_based=true')
    expect(filteredPage.attributes('style')).toContain('--sv-brightness: 1.04;')
    expect(filteredPage.attributes('style')).toContain('--sv-contrast: 1.28;')
    expect(filteredPage.attributes('style')).toContain('--sv-saturate: 1.08;')
  })

  it('uses the canvas page prototype for canvas sharpen outside continuous scroll', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await wrapper.setData({imageFilterPreset: 'CANVAS_SHARPEN'})
    await Vue.nextTick()

    const page = wrapper.find('[data-testid="shelfview-reader-page"]')
    expect(page.element.tagName.toLowerCase()).toBe('canvas')
    expect(page.attributes('data-canvas-src')).toBe('http://api.test/api/v1/books/book-1/pages/0?zero_based=true')
    expect(page.attributes('data-canvas-mode')).toBe('SHARPEN')
  })

  it('passes advanced image processing modes to the canvas page', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await wrapper.setData({imageFilterPreset: 'WEBGL_SHARPEN'})
    await Vue.nextTick()
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('data-canvas-mode')).toBe('WEBGL_SHARPEN')

    await wrapper.setData({imageFilterPreset: 'BACKGROUND_CLEANUP'})
    await Vue.nextTick()
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('data-canvas-mode')).toBe('BACKGROUND_CLEANUP')

    await wrapper.setData({imageFilterPreset: 'ADAPTIVE_MONO'})
    await Vue.nextTick()
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('data-canvas-mode')).toBe('ADAPTIVE_MONO')
  })

  it('keeps continuous scroll on the normal image path for canvas sharpen', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await chooseControlOption(wrapper, 'shelfview-reader-layout-toggle', 'shelfview-reader-layout-option-CONTINUOUS_SCROLL')
    await wrapper.setData({imageFilterPreset: 'CANVAS_SHARPEN'})
    await flushPromises()
    await Vue.nextTick()

    const pages = wrapper.findAll('[data-testid="shelfview-reader-page"]')
    expect(pages.length).toBe(2)
    expect(pages.at(0).element.tagName.toLowerCase()).toBe('img')
  })

  it('creates, opens and deletes bookmarks from the drawer', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const savedBookmark = {id: 'bookmark-2', bookId: 'book-1', locator: pageLocator(1), label: 'Second page', note: 'Remember this page', createdAt: 'now', updatedAt: 'now'}
    const createBookmark = jest.fn().mockResolvedValue({
      id: 'bookmark-new',
      bookId: 'book-1',
      locator: pageLocator(0),
      label: 'Page 1',
      createdAt: 'now',
      updatedAt: 'now',
    })
    const updateBookmark = jest.fn().mockResolvedValue({
      ...savedBookmark,
      note: 'Updated memo',
    })
    const deleteBookmark = jest.fn().mockResolvedValue(undefined)
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {
        getBookmarks: jest.fn().mockResolvedValue([
          savedBookmark,
        ]),
        createBookmark,
        updateBookmark,
        deleteBookmark,
      },
    })

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-bookmarks-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-bookmark-note"]').text()).toBe('Remember this page')

    await wrapper.find('[data-testid="shelfview-bookmark-note-edit"]').trigger('click')
    await Vue.nextTick()
    await wrapper.find('[data-testid="shelfview-bookmark-note-input"]').setValue('Updated memo')
    await wrapper.find('[data-testid="shelfview-bookmark-note-save"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(updateBookmark).toHaveBeenCalledWith('bookmark-2', {
      locator: pageLocator(1),
      label: 'Second page',
      note: 'Updated memo',
    })
    expect(wrapper.find('[data-testid="shelfview-bookmark-note"]').text()).toBe('Updated memo')

    await wrapper.find('[data-testid="shelfview-bookmark-go"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('2 / 2')

    await wrapper.find('[data-testid="shelfview-reader-bookmarks-toggle"]').trigger('click')
    await Vue.nextTick()
    await wrapper.find('[data-testid="shelfview-bookmarks-add"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(createBookmark).toHaveBeenCalledWith('book-1', {
      locator: pageLocator(1),
      label: 'shelfview_reader.bookmark_page',
    })

    await wrapper.find('[data-testid="shelfview-bookmark-delete"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(deleteBookmark).toHaveBeenCalledWith('bookmark-2')
  })

  it('disables image filters when the manifest does not advertise support', async () => {
    const getManifest = jest.fn().mockResolvedValue({
      ...manifest,
      capabilities: {
        ...manifest.capabilities,
        imageFilters: false,
      },
    })
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-filter-toggle"]').attributes('disabled')).toBe('disabled')
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('style')).toContain('--sv-brightness: 1;')
  })

  it('shows an error state when the manifest cannot be opened', async () => {
    const getManifest = jest.fn().mockRejectedValue(new ShelfViewApiError('Unsupported book', 'BAD_REQUEST', 400))
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-error"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').exists()).toBe(false)
  })

  it('does not show raw English error text for an unmapped failure', async () => {
    const getManifest = jest.fn().mockRejectedValue(new Error('Network Error: ECONNREFUSED 10.0.0.5:25600'))
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    const error = wrapper.find('[data-testid="shelfview-reader-error"]')
    expect(error.exists()).toBe(true)
    expect(error.text()).toContain('shelfview_reader.error_generic_message')
    expect(error.text()).not.toContain('ECONNREFUSED')
    expect(error.text()).not.toContain('Network Error')
  })

  it('retries opening the reader after a temporary manifest failure', async () => {
    const getManifest = jest
      .fn()
      .mockRejectedValueOnce(new ShelfViewApiError('Temporary outage', 'UNAVAILABLE', 503))
      .mockResolvedValueOnce(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-error"]').text()).toContain('shelfview_reader.error_unavailable_title')

    await wrapper.find('[data-testid="shelfview-reader-retry"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(getManifest).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-testid="shelfview-reader-error"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').exists()).toBe(true)
  })

  it('retries a failed page image without reopening the whole book', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-page"]').trigger('error')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-error"]').text()).toContain('shelfview_reader.error_page_failed_title')

    await wrapper.find('[data-testid="shelfview-reader-retry"]').trigger('click')
    await Vue.nextTick()

    const page = wrapper.find('[data-testid="shelfview-reader-page"]')
    expect(getManifest).toHaveBeenCalledTimes(1)
    expect(page.exists()).toBe(true)
    expect(page.attributes('data-page-reload')).toBe('1')
  })

  it('clears a failed page image state when moving between mixed content items', async () => {
    const getManifest = jest.fn().mockResolvedValue(mixedManifest)
    const getTextContent = jest.fn().mockResolvedValue(mixedTextContent)
    const wrapper = mountShell({getManifest, shelfViewText: {getTextContent}})

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-contents-toggle"]').trigger('click')
    await Vue.nextTick()
    await wrapper.findAll('[data-testid="shelfview-contents-item"]').at(1).trigger('click')
    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-page"]').trigger('error')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-error"]').text()).toContain('shelfview_reader.error_page_failed_title')

    await (wrapper.vm as any).previousPage()
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-error"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-text"]').exists()).toBe(true)
  })

  it('retries text content with a manual encoding when automatic decoding fails', async () => {
    const getManifest = jest.fn().mockResolvedValue(textManifest)
    const getTextContent = jest
      .fn()
      .mockRejectedValueOnce(new ShelfViewApiError('SHELFVIEW_TEXT_ENCODING_UNREADABLE', 'BAD_REQUEST', 400))
      .mockResolvedValueOnce({...textContent, encoding: 'euc-kr'})
    const wrapper = mountShell({
      getManifest,
      shelfViewText: {getTextContent},
    })

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-error"]').text()).toContain('shelfview_reader.error_encoding_title')
    expect(wrapper.find('[data-testid="shelfview-reader-retry-euckr"]').exists()).toBe(true)

    await wrapper.find('[data-testid="shelfview-reader-retry-euckr"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(getTextContent).toHaveBeenNthCalledWith(2, textManifest, 'euc-kr')
    expect(wrapper.find('[data-testid="shelfview-reader-error"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="shelfview-reader-text"]').exists()).toBe(true)
  })

  it('shows an empty state when the manifest has no readable pages', async () => {
    const getManifest = jest.fn().mockResolvedValue({...manifest, pageCount: 0})
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-empty"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').exists()).toBe(false)
  })

  it('returns to the originating screen when the reader was opened from inside the app', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})
    const vm = wrapper.vm as any

    await flushPromises()
    await Vue.nextTick()

    vm.canReturnToPreviousScreen = true
    await vm.closeBook()

    expect(vm.$router.back).toHaveBeenCalledTimes(1)
    expect(vm.$router.push).not.toHaveBeenCalled()
  })

  it('falls back to the book page when the reader was opened directly', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})
    const vm = wrapper.vm as any

    await flushPromises()
    await Vue.nextTick()

    vm.canReturnToPreviousScreen = false
    await vm.closeBook()

    expect(vm.$router.back).not.toHaveBeenCalled()
    expect(vm.$router.push).toHaveBeenCalledWith(expect.objectContaining({
      name: 'browse-book',
      params: expect.objectContaining({bookId: 'book-1'}),
    }))
  })

  it('mirrors paging in a bottom dock on phone-sized viewports', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest), smAndDown: true})

    await flushPromises()
    await Vue.nextTick()
    await wrapper.setData({chromeVisible: true})

    expect(wrapper.find('[data-testid="shelfview-reader-page-dock"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-dock-counter"]').text()).toBe('1 / 2')

    await wrapper.find('[data-testid="shelfview-reader-dock-next"]').trigger('click')
    await Vue.nextTick()

    expect((wrapper.vm as any).currentPageIndex).toBe(1)
  })

  it('hides the bottom dock with the rest of the chrome and on wide viewports', async () => {
    const phone = mountShell({getManifest: jest.fn().mockResolvedValue(manifest), smAndDown: true})

    await flushPromises()
    await Vue.nextTick()
    await phone.setData({chromeVisible: false})

    expect(phone.find('[data-testid="shelfview-reader-page-dock"]').exists()).toBe(false)

    const desktop = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})

    await flushPromises()
    await Vue.nextTick()
    await desktop.setData({chromeVisible: true})

    expect(desktop.find('[data-testid="shelfview-reader-page-dock"]').exists()).toBe(false)
  })

  it('keeps the bottom chrome space reserved while the toolbar is hidden', async () => {
    const phone = mountShell({getManifest: jest.fn().mockResolvedValue(manifest), smAndDown: true})

    await flushPromises()
    await Vue.nextTick()

    expect(phone.find('[data-testid="shelfview-reader-shell"]').classes())
      .toContain('shelfview-reader-shell--page-dock')

    await phone.setData({chromeVisible: false})
    await Vue.nextTick()

    // The dock itself goes away with the toolbar, but the reservation that keeps
    // the page clear of it must not, or hiding the toolbar would resize the page.
    expect(phone.find('[data-testid="shelfview-reader-page-dock"]').exists()).toBe(false)
    expect(phone.find('[data-testid="shelfview-reader-shell"]').classes())
      .toContain('shelfview-reader-shell--page-dock')
  })

  it('stops offering the back exit after moving to another book inside the reader', async () => {
    const wrapper = mountShell({getManifest: jest.fn().mockResolvedValue(manifest)})
    const vm = wrapper.vm as any

    await flushPromises()
    await Vue.nextTick()

    vm.canReturnToPreviousScreen = true
    const next = jest.fn()
    await (wrapper.vm.$options as any).beforeRouteUpdate.call(
      vm,
      {params: {bookId: 'book-2'}},
      {params: {bookId: 'book-1'}},
      next,
    )

    expect(vm.canReturnToPreviousScreen).toBe(false)
    expect(next).toHaveBeenCalled()
  })
})

function mountShell({
  getManifest,
  shelfViewState = {},
  shelfViewText = {},
  talesBooks = {},
  talesReadLists = {},
  routeQuery = {},
  smAndDown = false,
}: {
  getManifest: jest.Mock,
  shelfViewState?: Record<string, jest.Mock>,
  shelfViewText?: Record<string, jest.Mock>,
  talesBooks?: Record<string, jest.Mock>,
  talesReadLists?: Record<string, jest.Mock>,
  routeQuery?: Record<string, unknown>,
  smAndDown?: boolean,
}) {
  const stateService = {
    getPosition: jest.fn().mockResolvedValue(undefined),
    savePosition: jest.fn().mockResolvedValue({}),
    getBookmarks: jest.fn().mockResolvedValue([]),
    createBookmark: jest.fn(),
    updateBookmark: jest.fn(),
    deleteBookmark: jest.fn().mockResolvedValue(undefined),
    getHighlights: jest.fn().mockResolvedValue([]),
    createHighlight: jest.fn(),
    updateHighlight: jest.fn(),
    deleteHighlight: jest.fn().mockResolvedValue(undefined),
    getPreference: jest.fn().mockResolvedValue({
      scopeType: 'BOOK',
      scopeId: 'book-1',
      format: 'FIXED_PAGE_SEQUENCE',
      effectiveSettings: {
        layout: 'SINGLE_PAGE',
        readingDirection: 'LTR',
        pageFit: 'SCREEN',
        imageFilterPreset: 'ORIGINAL',
        fixedPageRotation: 0,
        fixedPageCoverSingle: false,
        fixedPageScale: 100,
        fixedPageGap: 12,
        fixedPageMargin: 16,
        fixedPageBackground: 'DARK',
        fixedPageBrightness: 1,
        fixedPageContrast: 1,
        textBrightness: 1,
      },
      sources: [],
    }),
    savePreference: jest.fn().mockResolvedValue({}),
    getDictionaryProviders: jest.fn().mockResolvedValue([]),
    importDictionaryLocalPack: jest.fn().mockResolvedValue({}),
    deleteDictionaryProvider: jest.fn().mockResolvedValue(undefined),
    lookupDictionary: jest.fn().mockResolvedValue([]),
    ...shelfViewState,
  }
  const textService = {
    getTextContent: jest.fn().mockResolvedValue(textContent),
    ...shelfViewText,
  }

  const wrapper = shallowMount(ShelfViewReaderShell, {
    propsData: {
      bookId: 'book-1',
    },
    mocks: {
      $talesBooks: {
        getBook: jest.fn().mockResolvedValue({...book}),
        getBookSiblingNext: jest.fn().mockRejectedValue(new Error('No next book')),
        updateReadProgress: jest.fn().mockResolvedValue({}),
        ...talesBooks,
      },
      $talesReadLists: {
        getBookSiblingNext: jest.fn().mockRejectedValue(new Error('No next book')),
        ...talesReadLists,
      },
      $talesSeries: {
        getOneSeries: jest.fn().mockResolvedValue(series),
      },
      $shelfViewManifest: {
        getManifest,
      },
      $shelfViewState: stateService,
      $shelfViewText: textService,
      $route: {
        query: routeQuery,
      },
      $router: {
        push: jest.fn(),
        back: jest.fn(),
      },
      $vuetify: {
        breakpoint: {smAndDown},
      },
      $t: (key: string) => key,
    },
    stubs: {
      'v-container': {template: '<div><slot /></div>'},
      'v-toolbar': {template: '<div v-bind="$attrs"><slot /></div>'},
      'v-toolbar-title': {template: '<div><slot /></div>'},
      'v-spacer': {template: '<span />'},
      'v-btn': {template: '<button v-bind="$attrs" v-on="$listeners"><slot /></button>'},
      'v-icon': {template: '<i><slot /></i>'},
      'v-tooltip': {template: '<div><slot name="activator" :on="{}" /><slot /></div>'},
      'v-select': {template: '<select v-bind="$attrs" v-on="$listeners"><slot /></select>'},
      'v-slider': {template: '<input type="range" v-bind="$attrs" v-on="$listeners" />'},
      'v-checkbox': {template: '<input type="checkbox" v-bind="$attrs" v-on="$listeners" />'},
      'v-expansion-panels': {template: '<div v-bind="$attrs"><slot /></div>'},
      'v-expansion-panel': {template: '<section><slot /></section>'},
      'v-expansion-panel-header': {template: '<button v-bind="$attrs" v-on="$listeners"><slot /></button>'},
      'v-expansion-panel-content': {template: '<div><slot /></div>'},
      'v-text-field': {
        props: ['value'],
        template: '<input v-bind="$attrs" :value="value" @input="$emit(\'input\', $event.target.value)" @keyup="$emit(\'keyup\', $event)" />',
      },
      'v-snackbar': {
        props: ['value'],
        template: '<div v-if="value" v-bind="$attrs"><slot /></div>',
      },
      'v-progress-circular': {template: '<span />'},
      ShelfViewControlMenu: {
        props: ['menu', 'title', 'left', 'options'],
        template: `
          <div>
            <button
              data-testid="shelfview-reader-control-scrim"
              @click="$emit('close')"
            />
            <ul
              data-testid="shelfview-reader-control-menu"
              :aria-label="title"
              :style="{ left: left + 'px' }"
            >
              <li>{{ title }}</li>
              <li
                v-for="option in options"
                :key="String(option.value)"
              >
                <button
                  :data-testid="'shelfview-reader-' + menu + '-option-' + option.value"
                  :aria-checked="option.active ? 'true' : 'false'"
                  @click="$emit('select', option.value)"
                >{{ option.label }}</button>
              </li>
            </ul>
          </div>
        `,
      },
      ShelfViewCanvasImagePage: {
        props: ['src', 'alt', 'pageIndex', 'reloadNonce', 'pageClass', 'pageStyle', 'mode'],
        template: `
          <canvas
            class="shelfview-reader-shell__page shelfview-page-content"
            :class="pageClass"
            :style="pageStyle"
            :data-canvas-src="src"
            :data-canvas-mode="mode"
            :aria-label="alt"
            :data-page-index="pageIndex"
            :data-page-reload="reloadNonce"
            data-testid="shelfview-reader-page"
          />
        `,
      },
    },
  })
  mountedWrappers.push(wrapper)
  return wrapper
}
