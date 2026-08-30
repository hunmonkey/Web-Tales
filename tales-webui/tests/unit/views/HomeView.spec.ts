import {shallowMount} from '@vue/test-utils'
import HomeView from '@/views/HomeView.vue'

jest.mock('@/i18n', () => ({
  __esModule: true,
  default: {
    locale: 'en',
    t: (key: string) => key,
    tc: (key: string, count: number) => `${key}:${count}`,
  },
}))

// ReusableDialogs pulls in the whole edit-dialog tree, which includes an
// ESM-only dependency Jest cannot parse; the shell tests never open it.
jest.mock('@/components/ReusableDialogs.vue', () => ({
  __esModule: true,
  default: {name: 'ReusableDialogs', render: (h: any) => h('div')},
}))

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0))

const locks = [
  {libraryId: 'locked1', name: 'Locked Library', unlocked: false},
  {libraryId: 'unlocked1', name: 'Unlocked Library', unlocked: true},
]

describe('HomeView library locks', () => {
  it('shows a sidebar entry for each still-locked library, none for unlocked ones', async () => {
    const wrapper = mountHome({getLockStates: jest.fn().mockResolvedValue(locks)})
    await flushPromises()
    await wrapper.vm.$nextTick()

    const entries = wrapper.findAll('[data-testid="locked-library"]')
    expect(entries.length).toBe(1)
    expect(entries.at(0).text()).toContain('Locked Library')
  })

  it('refreshes the library list and navigates into the library after a successful unlock', async () => {
    const unlock = jest.fn().mockResolvedValue(undefined)
    const dispatch = jest.fn().mockResolvedValue(undefined)
    const push = jest.fn()
    const wrapper = mountHome({getLockStates: jest.fn().mockResolvedValue(locks), unlock, dispatch, push})
    await flushPromises()

    ;(wrapper.vm as any).openUnlockDialog(locks[0])
    await (wrapper.vm as any).submitUnlock('1234')

    expect(unlock).toHaveBeenCalledWith('locked1', '1234')
    expect(dispatch).toHaveBeenCalledWith('getLibraries')
    expect(push).toHaveBeenCalledWith({name: 'libraries', params: {libraryId: 'locked1'}})
    expect((wrapper.vm as any).unlockDialogVisible).toBe(false)
  })

  it('keeps the dialog open with an error when the PIN is wrong', async () => {
    const unlock = jest.fn().mockRejectedValue({response: {status: 403}})
    const push = jest.fn()
    const wrapper = mountHome({getLockStates: jest.fn().mockResolvedValue(locks), unlock, push})
    await flushPromises()

    ;(wrapper.vm as any).openUnlockDialog(locks[0])
    await (wrapper.vm as any).submitUnlock('9999')

    expect((wrapper.vm as any).unlockDialogVisible).toBe(true)
    expect((wrapper.vm as any).unlockError).toBe('dialog.unlock_library.wrong_pin')
    expect(push).not.toHaveBeenCalled()
  })

  it('keeps the sidebar working when the lock list cannot be fetched', async () => {
    const wrapper = mountHome({getLockStates: jest.fn().mockRejectedValue(new Error('down'))})
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(wrapper.findAll('[data-testid="locked-library"]').length).toBe(0)
  })
})

function mountHome(services: {
  getLockStates: jest.Mock,
  unlock?: jest.Mock,
  dispatch?: jest.Mock,
  push?: jest.Mock,
}) {
  return shallowMount(HomeView, {
    mocks: {
      $store: {
        getters: {
          getLibraries: [],
          getLibrariesPinned: [],
          getLibrariesUnpinned: [],
          meAdmin: true,
        },
        dispatch: services.dispatch ?? jest.fn().mockResolvedValue(undefined),
      },
      $route: {params: {}},
      $router: {push: services.push ?? jest.fn()},
      $t: (key: string) => key,
      $eventHub: {$on: jest.fn(), $off: jest.fn(), $emit: jest.fn()},
      $shelfViewLibraryLocks: {
        getLockStates: services.getLockStates,
        unlock: services.unlock ?? jest.fn().mockResolvedValue(undefined),
      },
      $vuetify: {breakpoint: {lgAndUp: true}, rtl: false},
    },
    stubs: [
      'confirmation-dialog',
      'library-pin-dialog',
      'libraries-actions-menu',
      'library-actions-menu',
      'reorder-libraries',
      'reusable-dialogs',
      'router-view',
      'search-box',
      'toaster-notification',
      'v-btn',
      'v-icon',
      'v-list',
      'v-list-group',
      'v-list-item',
      'v-list-item-action',
      'v-list-item-content',
      'v-list-item-subtitle',
      'v-list-item-title',
      'v-main',
      'v-navigation-drawer',
      'v-slide-x-transition',
      'v-spacer',
    ],
  })
}
