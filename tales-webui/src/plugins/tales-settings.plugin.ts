import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import TalesSettingsService from '@/services/tales-settings.service'
import {Module} from 'vuex'
import {
  CLIENT_SETTING,
  ClientSettingDto,
  ClientSettingLibrary,
  ClientSettingLibraryUpdate,
  ClientSettingUserUpdateDto,
} from '@/types/tales-clientsettings'
import {LibrarySort} from '@/types/tales-clientsettings'
import {normalizeLibrarySort} from '@/functions/library-sort'
import {Theme} from '@/types/themes'
import {KNOWN_LOCALES} from '@/i18n'

let service: TalesSettingsService

export const vuexModule: Module<any, any> = {
  state: {
    clientSettingsGlobal: {} as Record<string, ClientSettingDto>,
    clientSettingsUser: {} as Record<string, ClientSettingDto>,
  },
  getters: {
    getClientSettings(state): Record<string, ClientSettingDto> {
      return {...state.clientSettingsGlobal, ...state.clientSettingsUser}
    },
    getClientSettingsLibraries(state): Record<string, ClientSettingLibrary> {
      let settings: Record<string, ClientSettingLibrary> = {}
      try {
        settings = JSON.parse(state.clientSettingsUser[CLIENT_SETTING.WEBUI_LIBRARIES]?.value)
      } catch (e) {
      }
      return settings
    },
    getClientSettingsLibrariesSort(state): LibrarySort {
      return normalizeLibrarySort(state.clientSettingsUser[CLIENT_SETTING.WEBUI_LIBRARIES_SORT]?.value)
    },
  },
  mutations: {
    setClientSettingsGlobal(state, settings) {
      state.clientSettingsGlobal = settings
    },
    setClientSettingsUser(state, settings) {
      state.clientSettingsUser = settings
    },
  },
  actions: {
    async getClientSettingsGlobal({commit}) {
      commit('setClientSettingsGlobal', await service.getClientSettingsGlobal())
    },
    async getClientSettingsUser({commit}) {
      commit('setClientSettingsUser', await service.getClientSettingsUser())
    },
    // Theme and locale are stored per account on the server, so signing in restores
    // the preferences of that account instead of whatever the browser last used.
    applyUserPreferences({state, commit}) {
      const theme = state.clientSettingsUser[CLIENT_SETTING.WEBUI_THEME]?.value
      if (Object.values(Theme).includes(theme)) commit('setTheme', theme)

      const locale = state.clientSettingsUser[CLIENT_SETTING.WEBUI_LOCALE]?.value
      // Checked against every locale that has a file, not against the loaded
      // ones: a stored locale outside the four eager ones is legitimate and
      // arrives when App's watcher fetches its chunk.
      if (KNOWN_LOCALES.includes(locale)) commit('setLocale', locale)
    },
    async saveUserPreference({dispatch}, {key, value}: { key: CLIENT_SETTING, value: string }) {
      const newSettings = {} as Record<string, ClientSettingUserUpdateDto>
      newSettings[key] = {value: value}
      await service.updateClientSettingUser(newSettings)
      await dispatch('getClientSettingsUser')
    },
    async updateLibrarySetting({dispatch, getters}, update: ClientSettingLibraryUpdate) {
      const all = getters.getClientSettingsLibraries
      all[update.libraryId] = Object.assign({}, all[update.libraryId], update.patch)
      const newSettings = {} as Record<string, ClientSettingUserUpdateDto>
      newSettings[CLIENT_SETTING.WEBUI_LIBRARIES] = {
        value: JSON.stringify(all),
      }
      await service.updateClientSettingUser(newSettings)
      dispatch('getClientSettingsUser')
    },
    async updateLibrariesSettings({dispatch, getters}, updates: ClientSettingLibraryUpdate[]) {
      const all = getters.getClientSettingsLibraries
      updates.forEach(u => all[u.libraryId] = Object.assign({}, all[u.libraryId], u.patch))

      const newSettings = {} as Record<string, ClientSettingUserUpdateDto>
      newSettings[CLIENT_SETTING.WEBUI_LIBRARIES] = {
        value: JSON.stringify(all),
      }
      await service.updateClientSettingUser(newSettings)
      dispatch('getClientSettingsUser')
    },
  },
}

export default {
  install(
    Vue: typeof _Vue,
    {store, http}: { store: any, http: AxiosInstance }) {
    service = new TalesSettingsService(http)
    Vue.prototype.$talesSettings = new TalesSettingsService(http)

    store.registerModule('talesSettings', vuexModule)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesSettings: TalesSettingsService;
  }
}
