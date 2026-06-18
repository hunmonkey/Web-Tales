import {shallowMount, Wrapper} from '@vue/test-utils'
import Vue from 'vue'
import ShelfViewReaderShell from '@/shelfview/views/ShelfViewReaderShell.vue'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {ShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {BookDto} from '@/types/komga-books'
import {SeriesDto} from '@/types/komga-series'
import {SHELFVIEW_LOCATOR_SCHEMA_VERSION, ShelfViewPageLocator} from '@/shelfview/models/shelfview-locator'

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

const pageLocator = (page: number): ShelfViewPageLocator => ({
  schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  kind: 'PAGE',
  page,
  offset: 0,
  contentFingerprint: manifest.contentFingerprint,
})

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
    expect(wrapper.find('[data-testid="shelfview-reader-sync-status"]').text()).toBe('Saving')

    await (wrapper.vm as any).flushPositionSave()
    await Vue.nextTick()

    expect(savePosition).toHaveBeenCalledTimes(1)
    expect(savePosition).toHaveBeenCalledWith('book-1', {
      locator: pageLocator(1),
      progression: 1,
    })
    expect(wrapper.find('[data-testid="shelfview-reader-sync-status"]').text()).toBe('Saved')
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
      label: 'Page 2',
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
}: {
  getManifest: jest.Mock,
  shelfViewState?: Record<string, jest.Mock>,
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
      'v-progress-circular': {template: '<span />'},
    },
  })
  mountedWrappers.push(wrapper)
  return wrapper
}
