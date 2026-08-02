import Vue from 'vue'
import Vuex from 'vuex'
import {vuexModule as talesSettingsModule} from '@/plugins/tales-settings.plugin'
import {persistedModule} from '@/plugins/persisted-state'
import {CLIENT_SETTING} from '@/types/tales-clientsettings'
import {Theme} from '@/types/themes'

jest.mock('@/i18n', () => ({
  __esModule: true,
  default: {
    locale: 'ko',
    availableLocales: ['en', 'ko', 'ja'],
    t: (key: string) => key,
  },
}))

Vue.use(Vuex)

function storeWithUserSettings(settings: Record<string, { value: string }>) {
  const store = new Vuex.Store({
    modules: {
      talesSettings: talesSettingsModule,
      persistedState: persistedModule,
    },
  })
  store.commit('setClientSettingsUser', settings)
  return store
}

describe('applyUserPreferences', () => {
  it('restores the theme and locale saved on the account', async () => {
    const store = storeWithUserSettings({
      [CLIENT_SETTING.WEBUI_THEME]: {value: Theme.LIGHT},
      [CLIENT_SETTING.WEBUI_LOCALE]: {value: 'en'},
    })

    await store.dispatch('applyUserPreferences')

    expect(store.state.persistedState.theme).toBe(Theme.LIGHT)
    expect(store.state.persistedState.locale).toBe('en')
  })

  it('keeps the current values when the account has no saved preferences', async () => {
    const store = storeWithUserSettings({})
    store.commit('setTheme', Theme.DARK)
    store.commit('setLocale', 'ko')

    await store.dispatch('applyUserPreferences')

    expect(store.state.persistedState.theme).toBe(Theme.DARK)
    expect(store.state.persistedState.locale).toBe('ko')
  })

  it('ignores stored values that are not a known theme or locale', async () => {
    const store = storeWithUserSettings({
      [CLIENT_SETTING.WEBUI_THEME]: {value: 'neon'},
      [CLIENT_SETTING.WEBUI_LOCALE]: {value: 'xx'},
    })
    store.commit('setTheme', Theme.DARK)
    store.commit('setLocale', 'ko')

    await store.dispatch('applyUserPreferences')

    expect(store.state.persistedState.theme).toBe(Theme.DARK)
    expect(store.state.persistedState.locale).toBe('ko')
  })
})
