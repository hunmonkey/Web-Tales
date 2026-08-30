import {shallowMount} from '@vue/test-utils'
import PreferencesView from '@/views/PreferencesView.vue'

jest.mock('@/i18n', () => ({
  __esModule: true,
  default: {
    locale: 'en',
    t: (key: string) => key,
    tc: (key: string, count: number) => `${key}:${count}`,
  },
}))

// The embedded settings views pull in heavy component trees (and, for the
// users screen, ESM-only dependencies Jest cannot parse); the tab-selection
// tests never render their contents.
jest.mock('@/views/AccountView.vue', () => ({__esModule: true, default: {name: 'AccountSettings', render: (h: any) => h('div')}}))
jest.mock('@/views/UISettings.vue', () => ({__esModule: true, default: {name: 'UiSettings', render: (h: any) => h('div')}}))
jest.mock('@/views/ServerSettings.vue', () => ({__esModule: true, default: {name: 'ServerSettings', render: (h: any) => h('div')}}))
jest.mock('@/views/ServerManagement.vue', () => ({__esModule: true, default: {name: 'ServerManagement', render: (h: any) => h('div')}}))
jest.mock('@/views/SettingsUsers.vue', () => ({__esModule: true, default: {name: 'SettingsUsers', render: (h: any) => h('div')}}))
jest.mock('@/components/ReorderLibraries.vue', () => ({__esModule: true, default: {name: 'ReorderLibraries', render: (h: any) => h('div')}}))

describe('PreferencesView users tab', () => {
  it('lands on the users tab when the old /settings/users address redirects here', () => {
    const wrapper = mountPreferences({admin: true, query: {tab: 'users'}})
    expect((wrapper.vm as any).tab).toBe(3)
  })

  it('selects the users tab for the add-user route, so its dialog has a router-view to render in', () => {
    const wrapper = mountPreferences({admin: true, routeName: 'settings-users-add'})
    expect((wrapper.vm as any).tab).toBe(3)
  })

  it('ignores the users tab query for a non-admin, who has no such tab', () => {
    const wrapper = mountPreferences({admin: false, query: {tab: 'users'}})
    expect((wrapper.vm as any).tab).toBe(0)
  })

  it('starts on the first tab without a tab query', () => {
    const wrapper = mountPreferences({admin: true})
    expect((wrapper.vm as any).tab).toBe(0)
  })
})

// The sort and reorder controls moved here from the removed 서재 sidebar row
// (SV-1294); these tests carry over the coverage of its menu's spec.
describe('PreferencesView library ordering', () => {
  it('offers the manual, name and creation-date orders', () => {
    const wrapper = mountPreferences({admin: false})
    const values = ((wrapper.vm as any).librarySorts as any[]).map(it => it.value)
    expect(values).toEqual(expect.arrayContaining(['MANUAL', 'NAME', 'CREATED']))
  })

  it('saves the chosen order on the account', () => {
    const dispatch = jest.fn().mockResolvedValue(undefined)
    const wrapper = mountPreferences({admin: false, dispatch, librarySort: 'MANUAL'})

    ;(wrapper.vm as any).librarySort = 'CREATED'

    expect(dispatch).toHaveBeenCalledWith('saveUserPreference', {
      key: 'webui.libraries_sort',
      value: 'CREATED',
    })
  })

  it('does not write the order that is already in use', () => {
    const dispatch = jest.fn().mockResolvedValue(undefined)
    const wrapper = mountPreferences({admin: false, dispatch, librarySort: 'NAME'})

    ;(wrapper.vm as any).librarySort = 'NAME'

    expect(dispatch).not.toHaveBeenCalled()
  })

  it('shows the reorder panel only on demand, since opening it switches the sort to manual', async () => {
    const wrapper = mountPreferences({admin: false})
    expect(wrapper.findComponent({name: 'ReorderLibraries'}).exists()).toBe(false)

    ;(wrapper.vm as any).showReorder = true
    await wrapper.vm.$nextTick()

    expect(wrapper.findComponent({name: 'ReorderLibraries'}).exists()).toBe(true)
  })
})

function mountPreferences(options: {
  admin: boolean,
  query?: Record<string, string>,
  routeName?: string,
  dispatch?: jest.Mock,
  librarySort?: string,
}) {
  return shallowMount(PreferencesView, {
    mocks: {
      $store: {
        state: {persistedState: {theme: 'system'}},
        getters: {
          meAdmin: options.admin,
          getClientSettingsLibrariesSort: options.librarySort ?? 'MANUAL',
        },
        dispatch: options.dispatch ?? jest.fn().mockResolvedValue(undefined),
      },
      $route: {name: options.routeName ?? 'preferences', query: options.query ?? {}},
      $t: (key: string) => key,
      $i18n: {
        locale: 'en',
        availableLocales: ['en', 'ko'],
        t: (key: string) => key,
      },
    },
    stubs: [
      'v-btn',
      'v-col',
      'v-container',
      'v-row',
      'v-select',
      'v-tab',
      'v-tab-item',
      'v-tabs',
      'v-tabs-items',
    ],
  })
}
