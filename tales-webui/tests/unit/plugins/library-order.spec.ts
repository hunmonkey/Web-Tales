import Vue from 'vue'
import Vuex from 'vuex'
import {vuexModule as talesLibrariesModule} from '@/plugins/tales-libraries.plugin'
import {vuexModule as talesSettingsModule} from '@/plugins/tales-settings.plugin'
import {persistedModule} from '@/plugins/persisted-state'
import {CLIENT_SETTING} from '@/types/tales-clientsettings'
import {LibraryDto} from '@/types/tales-libraries'

jest.mock('@/i18n', () => ({
  __esModule: true,
  KNOWN_LOCALES: ['en', 'ko'],
  default: {
    locale: 'en',
    availableLocales: ['en', 'ko'],
    t: (key: string) => key,
  },
}))

Vue.use(Vuex)

const libraries = [
  {id: 'c', name: 'Comics', createdDate: '2026-01-03T10:00:00'},
  {id: 'a', name: 'Art', createdDate: '2026-01-01T10:00:00'},
  {id: 'b', name: 'Books', createdDate: '2026-01-02T10:00:00'},
] as LibraryDto[]

// The manual order deliberately disagrees with both the names and the dates, so
// each order is told apart by its result rather than by luck.
const manualOrder = {
  c: {order: 0},
  b: {order: 1},
  a: {order: 2},
}

function storeWithSort(sort?: string) {
  const store = new Vuex.Store({
    modules: {
      talesLibraries: talesLibrariesModule,
      talesSettings: talesSettingsModule,
      persistedState: persistedModule,
    },
  })
  store.commit('setLibraries', libraries)
  store.commit('setClientSettingsUser', {
    [CLIENT_SETTING.WEBUI_LIBRARIES]: {value: JSON.stringify(manualOrder)},
    ...(sort ? {[CLIENT_SETTING.WEBUI_LIBRARIES_SORT]: {value: sort}} : {}),
  })
  return store
}

describe('getLibraries', () => {
  it('uses the hand-made order when no order is saved', () => {
    const store = storeWithSort()

    expect(store.getters.getClientSettingsLibrariesSort).toBe('MANUAL')
    expect(store.getters.getLibraries.map((it: LibraryDto) => it.name)).toEqual(['Comics', 'Books', 'Art'])
  })

  it('orders by name when the account asks for it', () => {
    const store = storeWithSort('NAME')

    expect(store.getters.getLibraries.map((it: LibraryDto) => it.name)).toEqual(['Art', 'Books', 'Comics'])
  })

  it('orders by creation date when the account asks for it', () => {
    const store = storeWithSort('CREATED')

    expect(store.getters.getLibraries.map((it: LibraryDto) => it.name)).toEqual(['Art', 'Books', 'Comics'])
  })

  it('keeps the hand-made order intact behind a derived order', () => {
    // Switching away and back must not lose the manual order, which is why the
    // chosen order lives in its own setting instead of rewriting `order`.
    const store = storeWithSort('NAME')
    store.commit('setClientSettingsUser', {
      [CLIENT_SETTING.WEBUI_LIBRARIES]: {value: JSON.stringify(manualOrder)},
      [CLIENT_SETTING.WEBUI_LIBRARIES_SORT]: {value: 'MANUAL'},
    })

    expect(store.getters.getLibraries.map((it: LibraryDto) => it.name)).toEqual(['Comics', 'Books', 'Art'])
  })

  it('falls back to the hand-made order for an unknown saved value', () => {
    const store = storeWithSort('BY_SIZE')

    expect(store.getters.getClientSettingsLibrariesSort).toBe('MANUAL')
    expect(store.getters.getLibraries.map((it: LibraryDto) => it.name)).toEqual(['Comics', 'Books', 'Art'])
  })

  it('still splits pinned from unpinned under a derived order', () => {
    const store = new Vuex.Store({
      modules: {
        talesLibraries: talesLibrariesModule,
        talesSettings: talesSettingsModule,
        persistedState: persistedModule,
      },
    })
    store.commit('setLibraries', libraries)
    store.commit('setClientSettingsUser', {
      [CLIENT_SETTING.WEBUI_LIBRARIES]: {value: JSON.stringify({b: {unpinned: true}})},
      [CLIENT_SETTING.WEBUI_LIBRARIES_SORT]: {value: 'NAME'},
    })

    expect(store.getters.getLibrariesPinned.map((it: LibraryDto) => it.name)).toEqual(['Art', 'Comics'])
    expect(store.getters.getLibrariesUnpinned.map((it: LibraryDto) => it.name)).toEqual(['Books'])
  })
})
