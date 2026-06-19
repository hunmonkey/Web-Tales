import {shallowMount, Wrapper} from '@vue/test-utils'
import Vue from 'vue'
import ShelfViewReaderShell from '@/shelfview/views/ShelfViewReaderShell.vue'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {ShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {BookDto} from '@/types/komga-books'
import {SeriesDto} from '@/types/komga-series'
import {SHELFVIEW_LOCATOR_SCHEMA_VERSION, ShelfViewPageLocator, ShelfViewTextLocator} from '@/shelfview/models/shelfview-locator'
import {ShelfViewTextContent} from '@/shelfview/models/shelfview-text'

jest.mock('@/functions/urls', () => ({
  __esModule: true,
  default: {
    originNoSlash: 'http://api.test',
  },
}))

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
    epubIsKepub: false,
  },
  metadata: {
    title: 'Book One',
    number: '1',
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

const textManifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'Book One',
  kind: 'TEXT_REFLOW',
  sourceFormat: 'TXT',
  contentFingerprint: 'komga-file-state:book-1-text',
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
  contentFingerprint: 'komga-file-state:mixed-book',
  pageCount: 3,
  items: [
    {
      itemId: 'text-item',
      index: 0,
      kind: 'TEXT_REFLOW',
      label: '001.txt',
      contentFingerprint: 'komga-file-state:mixed-book:text-item',
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
      contentFingerprint: 'komga-file-state:mixed-book:image-item',
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
  contentFingerprint: 'komga-file-state:mixed-book:text-item',
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

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0))
const mountedWrappers: Wrapper<Vue>[] = []

describe('ShelfViewReaderShell', () => {
  afterEach(() => {
    mountedWrappers.splice(0).forEach(wrapper => wrapper.destroy())
    jest.useRealTimers()
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
    expect(wrapper.find('[data-testid="shelfview-reader-text-theme-toggle"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-text-settings-toggle"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-reader-stage"]').attributes('aria-label')).toContain('paragraph 1 of 3')
    expect(wrapper.find('[data-testid="shelfview-reader-live-status"]').text()).toBe('Paragraph 1 of 3')

    await wrapper.find('[data-testid="shelfview-reader-layout-toggle"]').trigger('click')
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

    await wrapper.find('[data-testid="shelfview-reader-text-settings-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-text-settings"]').exists()).toBe(true)

    await wrapper.setData({
      textFontSize: 26,
      textLetterSpacing: 1.2,
      textContentWidth: 640,
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
        textTheme: 'DARK',
        textFontFamily: 'READABLE_KR',
        textFontSize: 26,
        textLetterSpacing: 1.2,
        textContentWidth: 640,
        textAlign: 'JUSTIFY',
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
      contentFingerprint: 'komga-file-state:mixed-book:text-item',
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
          contentFingerprint: 'komga-file-state:mixed-book:image-item',
        }),
      }),
      progression: 1,
    })
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

    await wrapper.find('[data-testid="shelfview-reader-layout-toggle"]').trigger('click')
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

    await wrapper.find('[data-testid="shelfview-reader-text-theme-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-text"]').classes()).toContain('shelfview-reader-shell__text--dark')

    await wrapper.find('[data-testid="shelfview-reader-layout-toggle"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.findAll('[data-testid="shelfview-reader-text-paragraph"]').wrappers.map(paragraph => paragraph.attributes('data-text-paragraph-index'))).toEqual(['0', '1', '2'])
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
          },
          sources: [],
        }),
      },
    })

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.findAll('[data-testid="shelfview-reader-page"]').wrappers.map(page => page.attributes('data-page-index'))).toEqual(['1', '0'])
    expect(wrapper.find('[data-testid="shelfview-reader-page-counter"]').text()).toBe('1-2 / 4')
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').classes()).toContain('shelfview-reader-shell__page--fit-width')
    expect(wrapper.find('[data-testid="shelfview-reader-page"]').attributes('style')).toContain('--sv-grayscale: 1;')

    await wrapper.find('[data-testid="shelfview-reader-bookmarks-toggle"]').trigger('click')
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-bookmarks-drawer"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="shelfview-bookmark-go"]').text()).toBe('Saved page')
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

    await wrapper.find('[data-testid="shelfview-reader-layout-toggle"]').trigger('click')
    await Vue.nextTick()
    await wrapper.find('[data-testid="shelfview-reader-layout-toggle"]').trigger('click')
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

    await wrapper.find('[data-testid="shelfview-reader-layout-toggle"]').trigger('click')
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

    await wrapper.find('[data-testid="shelfview-reader-layout-toggle"]').trigger('click')
    await wrapper.find('[data-testid="shelfview-reader-direction-toggle"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.findAll('[data-testid="shelfview-reader-page"]').wrappers.map(page => page.attributes('data-page-index'))).toEqual(['1', '0'])
  })

  it('renders all pages in continuous scroll mode', async () => {
    const getManifest = jest.fn().mockResolvedValue({...manifest, pageCount: 3})
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-layout-toggle"]').trigger('click')
    await wrapper.find('[data-testid="shelfview-reader-layout-toggle"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-pages"]').classes()).toContain('shelfview-reader-shell__pages--continuous')
    expect(wrapper.findAll('[data-testid="shelfview-reader-page"]').wrappers.map(page => page.attributes('data-page-index'))).toEqual(['0', '1', '2'])
  })

  it('cycles page fit classes', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const wrapper = mountShell({getManifest})

    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page"]').classes()).toContain('shelfview-reader-shell__page--fit-screen')

    await wrapper.find('[data-testid="shelfview-reader-fit-toggle"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    expect(wrapper.find('[data-testid="shelfview-reader-page"]').classes()).toContain('shelfview-reader-shell__page--fit-width')
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

    await wrapper.find('[data-testid="shelfview-reader-filter-toggle"]').trigger('click')
    await flushPromises()
    await Vue.nextTick()

    const filteredPage = wrapper.find('[data-testid="shelfview-reader-page"]')
    expect(filteredPage.attributes('src')).toBe('http://api.test/api/v1/books/book-1/pages/0?zero_based=true')
    expect(filteredPage.attributes('style')).toContain('--sv-brightness: 0.92;')
    expect(filteredPage.attributes('style')).toContain('--sv-sepia: 0.16;')
  })

  it('creates, opens and deletes bookmarks from the drawer', async () => {
    const getManifest = jest.fn().mockResolvedValue(manifest)
    const createBookmark = jest.fn().mockResolvedValue({
      id: 'bookmark-new',
      bookId: 'book-1',
      locator: pageLocator(0),
      label: 'Page 1',
      createdAt: 'now',
      updatedAt: 'now',
    })
    const deleteBookmark = jest.fn().mockResolvedValue(undefined)
    const wrapper = mountShell({
      getManifest,
      shelfViewState: {
        getBookmarks: jest.fn().mockResolvedValue([
          {id: 'bookmark-2', bookId: 'book-1', locator: pageLocator(1), label: 'Second page', createdAt: 'now', updatedAt: 'now'},
        ]),
        createBookmark,
        deleteBookmark,
      },
    })

    await flushPromises()
    await Vue.nextTick()

    await wrapper.find('[data-testid="shelfview-reader-bookmarks-toggle"]').trigger('click')
    await Vue.nextTick()
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
})

function mountShell({
  getManifest,
  shelfViewState = {},
  shelfViewText = {},
}: {
  getManifest: jest.Mock,
  shelfViewState?: Record<string, jest.Mock>,
  shelfViewText?: Record<string, jest.Mock>,
}) {
  const stateService = {
    getPosition: jest.fn().mockResolvedValue(undefined),
    savePosition: jest.fn().mockResolvedValue({}),
    getBookmarks: jest.fn().mockResolvedValue([]),
    createBookmark: jest.fn(),
    updateBookmark: jest.fn(),
    deleteBookmark: jest.fn().mockResolvedValue(undefined),
    getPreference: jest.fn().mockResolvedValue({
      scopeType: 'BOOK',
      scopeId: 'book-1',
      format: 'FIXED_PAGE_SEQUENCE',
      effectiveSettings: {
        layout: 'SINGLE_PAGE',
        readingDirection: 'LTR',
        pageFit: 'SCREEN',
        imageFilterPreset: 'ORIGINAL',
      },
      sources: [],
    }),
    savePreference: jest.fn().mockResolvedValue({}),
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
      $komgaBooks: {
        getBook: jest.fn().mockResolvedValue({...book}),
      },
      $komgaSeries: {
        getOneSeries: jest.fn().mockResolvedValue(series),
      },
      $shelfViewManifest: {
        getManifest,
      },
      $shelfViewState: stateService,
      $shelfViewText: textService,
      $route: {
        query: {},
      },
      $router: {
        push: jest.fn(),
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
      'v-progress-circular': {template: '<span />'},
    },
  })
  mountedWrappers.push(wrapper)
  return wrapper
}
