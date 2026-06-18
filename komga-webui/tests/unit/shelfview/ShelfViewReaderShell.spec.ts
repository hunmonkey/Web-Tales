import {shallowMount} from '@vue/test-utils'
import Vue from 'vue'
import ShelfViewReaderShell from '@/shelfview/views/ShelfViewReaderShell.vue'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {ShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {BookDto} from '@/types/komga-books'
import {SeriesDto} from '@/types/komga-series'

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

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0))

describe('ShelfViewReaderShell', () => {
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

function mountShell({getManifest}: { getManifest: jest.Mock }) {
  return shallowMount(ShelfViewReaderShell, {
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
}
