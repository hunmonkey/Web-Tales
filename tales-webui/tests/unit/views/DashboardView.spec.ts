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

  it('refreshes the card poster when the book gets a new thumbnail', async () => {
    const wrapper = mountDashboard({
      getBooksList: jest.fn()
        .mockResolvedValueOnce(pageOf([resumeBook]))
        .mockResolvedValue(pageOf([])),
    })
    await flushPromises()
    await wrapper.vm.$nextTick()

    const before = (wrapper.vm as any).resumeThumbnailUrl(resumeBook)
    expect(before).not.toContain('?')

    ;(wrapper.vm as any).thumbnailBookChanged({selected: true, bookId: 'book1'})
    await wrapper.vm.$nextTick()

    const after = (wrapper.vm as any).resumeThumbnailUrl(resumeBook)
    expect(after).toContain('?')
    expect(after.split('?')[0]).toBe(before)
  })

  it('ignores thumbnail events for a book that is not in the row', async () => {
    const wrapper = mountDashboard({
      getBooksList: jest.fn()
        .mockResolvedValueOnce(pageOf([resumeBook]))
        .mockResolvedValue(pageOf([])),
    })
    await flushPromises()
    await wrapper.vm.$nextTick()

    const before = (wrapper.vm as any).resumeThumbnailUrl(resumeBook)
    ;(wrapper.vm as any).thumbnailBookChanged({selected: true, bookId: otherBook.id})
    await wrapper.vm.$nextTick()

    expect((wrapper.vm as any).resumeThumbnailUrl(resumeBook)).toBe(before)
  })

  it('fetches every Keep Reading page up front, since the grid has no scroll paging', async () => {
    const firstPage = {...pageOf([resumeBook]), last: false, totalElements: 2, totalPages: 2}
    const secondPage = {...pageOf([otherBook]), first: false, number: 1, totalPages: 2}
    const getBooksList = jest.fn()
      .mockResolvedValueOnce(firstPage) // Keep Reading, page 0
      .mockResolvedValueOnce(pageOf([])) // recently released
      .mockResolvedValueOnce(pageOf([])) // recently added
      .mockResolvedValueOnce(pageOf([])) // recently read
      .mockResolvedValueOnce(secondPage) // Keep Reading, page 1, fetched by the drain
      .mockResolvedValue(pageOf([]))

    const wrapper = mountDashboard({getBooksList})
    await flushPromises()
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect((wrapper.vm as any).resumeBooks.map((b: any) => b.id)).toEqual(['book1', 'book2'])
    expect(wrapper.findAll('[data-testid="dashboard-resume-card"]').length).toBe(2)
  })

  it('keeps every book that is still in progress, not just the most recent one', async () => {
    const wrapper = mountDashboard({
      getBooksList: jest.fn()
        .mockResolvedValueOnce(pageOf([resumeBook, otherBook]))
        .mockResolvedValue(pageOf([])),
    })
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect((wrapper.vm as any).resumeBooks.map((b: any) => b.id)).toEqual(['book1', 'book2'])
  })

  it('deletes the read progress of the book removed from Keep Reading, and reloads the row', async () => {
    const getBooksList = jest.fn()
      .mockResolvedValueOnce(pageOf([resumeBook, otherBook]))
      .mockResolvedValue(pageOf([]))
    const deleteReadProgress = jest.fn().mockResolvedValue(undefined)

    const wrapper = mountDashboard({getBooksList, deleteReadProgress})
    await flushPromises()
    await wrapper.vm.$nextTick()

    // The cross asks first: nothing is deleted until the dialog is confirmed.
    ;(wrapper.vm as any).confirmRemoveResume([otherBook])
    expect((wrapper.vm as any).removeResumeDialog).toBe(true)
    expect(deleteReadProgress).not.toHaveBeenCalled()

    const callsBefore = getBooksList.mock.calls.length
    await (wrapper.vm as any).removeResume()

    expect(deleteReadProgress).toHaveBeenCalledWith('book2')
    // The row is reloaded rather than left to the throttled SSE reload.
    expect(getBooksList.mock.calls.length).toBeGreaterThan(callsBefore)
  })

  it('selects and unselects every book from the header checkbox', async () => {
    const wrapper = await mountWithResume()
    const vm = wrapper.vm as any

    expect(vm.allResumeSelected).toBe(false)
    expect(vm.someResumeSelected).toBe(false)

    vm.setResumeSelection('book1', true)
    expect(vm.someResumeSelected).toBe(true)
    expect(vm.allResumeSelected).toBe(false)

    vm.toggleAllResume(true)
    expect(vm.selectedResume.sort()).toEqual(['book1', 'book2'])
    expect(vm.allResumeSelected).toBe(true)
    expect(vm.someResumeSelected).toBe(false)

    // Selecting the same book twice must not queue it twice.
    vm.setResumeSelection('book1', true)
    expect(vm.selectedResume.filter((id: string) => id === 'book1').length).toBe(1)

    vm.setResumeSelection('book1', false)
    expect(vm.selectedResume).toEqual(['book2'])

    vm.toggleAllResume(false)
    expect(vm.selectedResume).toEqual([])
  })

  it('ignores a selected id that the grid no longer holds', async () => {
    const wrapper = await mountWithResume()
    const vm = wrapper.vm as any

    vm.setResumeSelection('book1', true)
    vm.setResumeSelection('gone', true)

    // Two ids but only one of the two books: counting ids alone would read as
    // "all selected", and the phantom would reach the delete call.
    expect(vm.allResumeSelected).toBe(false)
    expect(vm.someResumeSelected).toBe(true)
    expect(vm.selectedResumeBooks.map((b: any) => b.id)).toEqual(['book1'])
  })

  it('deletes the read progress of every selected book on one confirmation', async () => {
    const deleteReadProgress = jest.fn().mockResolvedValue(undefined)
    const wrapper = await mountWithResume({deleteReadProgress})
    const vm = wrapper.vm as any

    vm.setResumeSelection('book2', true)
    vm.confirmRemoveResume(vm.selectedResumeBooks)
    expect(vm.removeResumeDialog).toBe(true)
    expect(deleteReadProgress).not.toHaveBeenCalled()

    await vm.removeResume()

    expect(deleteReadProgress.mock.calls.map((c: any[]) => c[0])).toEqual(['book2'])
    expect(vm.selectedResume).toEqual([])
  })

  it('deletes the whole grid when Remove all is confirmed', async () => {
    const deleteReadProgress = jest.fn().mockResolvedValue(undefined)
    const wrapper = await mountWithResume({deleteReadProgress})
    const vm = wrapper.vm as any

    vm.confirmRemoveResume(vm.resumeBooks)
    await vm.removeResume()

    expect(deleteReadProgress.mock.calls.map((c: any[]) => c[0]).sort()).toEqual(['book1', 'book2'])
  })

  it('does not open the dialog when nothing is selected', async () => {
    const wrapper = await mountWithResume()
    const vm = wrapper.vm as any

    vm.confirmRemoveResume(vm.selectedResumeBooks)

    expect(vm.removeResumeDialog).toBe(false)
  })

  it('names the book when one is removed and counts them when several are', async () => {
    const wrapper = await mountWithResume()
    const vm = wrapper.vm as any

    expect(vm.removeResumeBody).toBe('')
    vm.confirmRemoveResume([resumeBook])
    expect(vm.removeResumeBody).toBe('dialog.delete_read_progress.warning')
    vm.confirmRemoveResume([resumeBook, otherBook])
    expect(vm.removeResumeBody).toBe('dialog.delete_read_progress.warning_multiple')
  })

  it('draws a checkbox on every card', async () => {
    const wrapper = await mountWithResume()
    expect(wrapper.findAll('[data-testid="dashboard-resume-select"]').length).toBe(2)
    expect(wrapper.find('[data-testid="dashboard-resume-select-all"]').exists()).toBe(true)
  })
})

async function mountWithResume(services: {deleteReadProgress?: jest.Mock} = {}) {
  const wrapper = mountDashboard({
    getBooksList: jest.fn()
      .mockResolvedValueOnce(pageOf([resumeBook, otherBook]))
      .mockResolvedValue(pageOf([])),
    deleteReadProgress: services.deleteReadProgress,
  })
  await flushPromises()
  await wrapper.vm.$nextTick()
  return wrapper
}

function mountDashboard(services: {
  getBooksList: jest.Mock,
  getBooksOnDeck?: jest.Mock,
  deleteReadProgress?: jest.Mock,
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
        deleteReadProgress: services.deleteReadProgress ?? jest.fn().mockResolvedValue(undefined),
      },
      $talesSeries: {
        getNewSeries: jest.fn().mockResolvedValue(pageOf([])),
        getUpdatedSeries: jest.fn().mockResolvedValue(pageOf([])),
      },
      $vuetify: {breakpoint: {xs: false, sm: false, md: false}},
    },
    stubs: [
      'confirmation-dialog',
      'edit-recommended-dialog',
      'empty-state',
      'horizontal-scroller',
      'item-browser',
      'item-card-skeleton',
      'multi-select-bar',
      'router-link',
      'v-btn',
      'v-checkbox',
      'v-container',
      'v-fab-transition',
      'v-icon',
      'v-progress-linear',
    ],
  })
}
