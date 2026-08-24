import TalesLibrariesService from '@/services/tales-libraries.service'
import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import {Module} from 'vuex/types'
import {LibraryDto} from '@/types/tales-libraries'
import {sortLibraries} from '@/functions/library-sort'

let service: TalesLibrariesService

// Exported so the store getters can be exercised directly in tests, the same
// way tales-settings.plugin.ts already is.
export const vuexModule: Module<any, any> = {
  state: {
    libraries: [] as LibraryDto[],
  },
  getters: {
    getLibraries(state, getters) {
      const settings = getters.getClientSettingsLibraries
      return sortLibraries(
        state.libraries.map((it: LibraryDto) => Object.assign({}, it, settings[it.id])),
        getters.getClientSettingsLibrariesSort,
      )
    },
    getLibraryById: (state, getters) => (id: number) => {
      return getters.getLibraries.find((l: any) => l.id === id)
    },
    getLibrariesPinned(state, getters) {
      return getters.getLibraries.filter((it: LibraryDto) => !it.unpinned)
    },
    getLibrariesUnpinned(state, getters) {
      return getters.getLibraries.filter((it: LibraryDto) => it.unpinned)
    },
  },
  mutations: {
    setLibraries(state, libraries) {
      state.libraries = libraries
    },
  },
  actions: {
    async getLibraries({commit}) {
      commit('setLibraries', await service.getLibraries())
    },
    async postLibrary({dispatch}, library) {
      await service.postLibrary(library)
    },
    async updateLibrary({dispatch}, {libraryId, library}) {
      await service.updateLibrary(libraryId, library)
    },
    async deleteLibrary({dispatch}, library) {
      await service.deleteLibrary(library)
    },
  },
}

export default {
  install(
    Vue: typeof _Vue,
    {store, http}: { store: any, http: AxiosInstance }) {
    service = new TalesLibrariesService(http)
    Vue.prototype.$talesLibraries = service

    store.registerModule('talesLibraries', vuexModule)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesLibraries: TalesLibrariesService;
  }
}
