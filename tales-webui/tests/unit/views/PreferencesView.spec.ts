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

function mountPreferences(options: {
  admin: boolean,
  query?: Record<string, string>,
  routeName?: string,
}) {
  return shallowMount(PreferencesView, {
    mocks: {
      $store: {
        state: {persistedState: {theme: 'system'}},
        getters: {meAdmin: options.admin},
        dispatch: jest.fn().mockResolvedValue(undefined),
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
