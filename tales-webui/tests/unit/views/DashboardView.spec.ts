import {shallowMount} from '@vue/test-utils'
import DashboardView from '@/views/DashboardView.vue'

jest.mock('@/i18n', () => ({
  __esModule: true,
  default: {
    locale: 'en',
    t: (key: string) => key,
    tc: (key: string, count: number) => `${key}:${count}`,
  },
}))

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0))

const resumeBook = {
  id: 'book1',
  seriesId: 'series1',
  seriesTitle: 'Series One',
  oneshot: false,
  metadata: {title: 'Chapter One'},
  media: {pagesCount: 100, mediaProfile: 'divina'},
  readProgress: {page: 42, completed: false},
}

const otherBook = {
  id: 'book2',
  seriesId: 'series2',
  seriesTitle: 'Series Two',
  oneshot: false,
  metadata: {title: 'Chapter Two'},
  media: {pagesCount: 50, mediaProfile: 'divina'},
  readProgress: {page: 10, completed: false},
}

function pageOf(content: any[]) {
  return {
    content,
    empty: content.length === 0,
    first: true,
    last: true,
    number: 0,
    numberOfElements: content.length,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
  }
}

describe('DashboardView', () => {
  it('leaves the loading state when one section fails, and keeps showing the others', async () => {
    // Keep Reading is the first getBooksList call; the rest resolve empty.
    const getBooksList = jest.fn()
      .mockResolvedValueOnce(pageOf([resumeBook]))
      .mockResolvedValue(pageOf([]))
    const getBooksOnDeck = jest.fn().mockRejectedValue(new Error('On Deck is down'))

    const wrapper = mountDashboard({getBooksList, getBooksOnDeck})
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(getBooksOnDeck).toHaveBeenCalled()
    expect((wrapper.vm as any).loading).toBe(false)
    expect(wrapper.find('[data-testid="dashboard-section-skeleton"]').exists()).toBe(false)
    // The section that did load is still on screen.
    expect(wrapper.find('[data-testid="dashboard-resume"]').exists()).toBe(true)
  })

  it('keeps the resume banner on screen while the dashboard reloads', async () => {
    const wrapper = mountDashboard({
      getBooksList: jest.fn()
        .mockResolvedValueOnce(pageOf([resumeBook]))
        .mockResolvedValue(pageOf([])),
    })
    await flushPromises()
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="dashboard-resume"]').exists()).toBe(true)

    // A read-progress event reloads the dashboard. The banner must not unmount
    // and remount, which would shift the page up and back down.
    ;(wrapper.vm as any).loadAll(true)
    await wrapper.vm.$nextTick()

    expect((wrapper.vm as any).loading).toBe(true)
    expect(wrapper.find('[data-testid="dashboard-resume"]').exists()).toBe(true)
  })

  it('refreshes the banner poster when the book gets a new thumbnail', async () => {
    const wrapper = mountDashboard({
      getBooksList: jest.fn()
        .mockResolvedValueOnce(pageOf([resumeBook]))
        .mockResolvedValue(pageOf([])),
    })
    await flushPromises()
    await wrapper.vm.$nextTick()

    const before = (wrapper.vm as any).resumeBookThumbnailUrl
    expect(before).not.toContain('?')

    ;(wrapper.vm as any).thumbnailBookChanged({selected: true, bookId: 'book1'})
    await wrapper.vm.$nextTick()

    const after = (wrapper.vm as any).resumeBookThumbnailUrl
    expect(after).toContain('?')
    expect(after.split('?')[0]).toBe(before)
  })

  it('ignores thumbnail events for a book that is not in the banner', async () => {
    const wrapper = mountDashboard({
      getBooksList: jest.fn()
        .mockResolvedValueOnce(pageOf([resumeBook]))
        .mockResolvedValue(pageOf([])),
    })
    await flushPromises()
    await wrapper.vm.$nextTick()

    const before = (wrapper.vm as any).resumeBookThumbnailUrl
    ;(wrapper.vm as any).thumbnailBookChanged({selected: true, bookId: otherBook.id})
    await wrapper.vm.$nextTick()

    expect((wrapper.vm as any).resumeBookThumbnailUrl).toBe(before)
  })
})

function mountDashboard(services: {
  getBooksList: jest.Mock,
  getBooksOnDeck?: jest.Mock,
}) {
  return shallowMount(DashboardView, {
    mocks: {
      $store: {
        state: {talesLibraries: {libraries: [{id: 'library1'}]}},
        getters: {
          getClientSettings: {},
          getLibrariesPinned: [{id: 'library1'}],
          getLibraryById: () => ({id: 'library1', name: 'Main library'}),
          meAdmin: false,
        },
        commit: jest.fn(),
        dispatch: jest.fn(),
      },
      $t: (key: string) => key,
      $tc: (key: string) => key,
      $eventHub: {$on: jest.fn(), $off: jest.fn()},
      $talesBooks: {
        getBooksList: services.getBooksList,
        getBooksOnDeck: services.getBooksOnDeck ?? jest.fn().mockResolvedValue(pageOf([])),
      },
      $talesSeries: {
        getNewSeries: jest.fn().mockResolvedValue(pageOf([])),
        getUpdatedSeries: jest.fn().mockResolvedValue(pageOf([])),
      },
      $vuetify: {breakpoint: {xs: false, sm: false, md: false}},
    },
    stubs: [
      'edit-recommended-dialog',
      'empty-state',
      'horizontal-scroller',
      'item-browser',
      'item-card-skeleton',
      'multi-select-bar',
      'router-link',
      'v-btn',
      'v-container',
      'v-fab-transition',
      'v-icon',
      'v-progress-linear',
    ],
  })
}
