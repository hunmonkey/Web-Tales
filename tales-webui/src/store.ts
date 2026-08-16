import Vue from 'vue'
import Vuex from 'vuex'
import {BookDto} from '@/types/tales-books'
import {Oneshot, SeriesDto} from '@/types/tales-series'
import createPersistedState from 'vuex-persistedstate'
import {persistedModule} from './plugins/persisted-state'
import {LibraryDto} from '@/types/tales-libraries'

Vue.use(Vuex)

const persistedState = createPersistedState({
  paths: ['persistedState'],
})

export default new Vuex.Store({
  state: {
    // libraries
    editLibrary: {} as LibraryDto | undefined,
    editLibraryDialog: false,
    deleteLibrary: {} as LibraryDto,
    deleteLibraryDialog: false,
    // books
    updateBooks: {} as BookDto | BookDto[],
    updateBooksDialog: false,
    deleteBooks: {} as BookDto | BookDto[],
    deleteBookDialog: false,
    // books bulk
    updateBulkBooks: [] as BookDto[],
    updateBulkBooksDialog: false,

    // oneshots
    updateOneshots: {} as Oneshot | Oneshot[],
    updateOneshotsDialog: false,

    // series
    updateSeries: {} as SeriesDto | SeriesDto[],
    updateSeriesDialog: false,
    deleteSeries: {} as SeriesDto | SeriesDto[],
    deleteSeriesDialog: false,

  },
  mutations: {
    // Libraries
    setEditLibrary(state, library) {
      state.editLibrary = library
    },
    setEditLibraryDialog(state, dialog) {
      state.editLibraryDialog = dialog
    },
    setDeleteLibrary(state, library) {
      state.deleteLibrary = library
    },
    setDeleteLibraryDialog(state, dialog) {
      state.deleteLibraryDialog = dialog
    },
    // Books
    setUpdateBooks(state, books) {
      state.updateBooks = books
    },
    setUpdateBooksDialog(state, dialog) {
      state.updateBooksDialog = dialog
    },
    setDeleteBooks(state, books) {
      state.deleteBooks = books
    },
    setDeleteBookDialog(state, dialog) {
      state.deleteBookDialog = dialog
    },
    // Books bulk
    setUpdateBulkBooks(state, books) {
      state.updateBulkBooks = books
    },
    setUpdateBulkBooksDialog(state, dialog) {
      state.updateBulkBooksDialog = dialog
    },
    // One-shots
    setUpdateOneshots(state, oneshots) {
      state.updateOneshots = oneshots
    },
    setUpdateOneshotsDialog(state, dialog) {
      state.updateOneshotsDialog = dialog
    },
    // Series
    setUpdateSeries(state, series) {
      state.updateSeries = series
    },
    setUpdateSeriesDialog(state, dialog) {
      state.updateSeriesDialog = dialog
    },
    setDeleteSeries(state, series) {
      state.deleteSeries = series
    },
    setDeleteSeriesDialog(state, dialog) {
      state.deleteSeriesDialog = dialog
    },
  },
  actions: {
    // libraries
    dialogAddLibrary({commit}) {
      commit('setEditLibrary', undefined)
      commit('setEditLibraryDialog', true)
    },
    dialogEditLibrary({commit}, value) {
      commit('setEditLibrary', value)
      commit('setEditLibraryDialog', true)
    },
    dialogEditLibraryDisplay({commit}, value) {
      commit('setEditLibraryDialog', value)
    },
    dialogDeleteLibrary({commit}, library) {
      commit('setDeleteLibrary', library)
      commit('setDeleteLibraryDialog', true)
    },
    dialogDeleteLibraryDisplay({commit}, value) {
      commit('setDeleteLibraryDialog', value)
    },
    // books
    dialogUpdateBooks({commit}, books) {
      commit('setUpdateBooks', books)
      commit('setUpdateBooksDialog', true)
    },
    dialogUpdateBooksDisplay({commit}, value) {
      commit('setUpdateBooksDialog', value)
    },
    dialogDeleteBook({commit}, books) {
      commit('setDeleteBooks', books)
      commit('setDeleteBookDialog', true)
    },
    dialogDeleteBookDisplay({commit}, value) {
      commit('setDeleteBookDialog', value)
    },
    // books bulk
    dialogUpdateBulkBooks({commit}, books) {
      commit('setUpdateBulkBooks', books)
      commit('setUpdateBulkBooksDialog', true)
    },
    dialogUpdateBulkBooksDisplay({commit}, value) {
      commit('setUpdateBulkBooksDialog', value)
    },
    // oneshots
    dialogUpdateOneshots({commit}, oneshots) {
      commit('setUpdateOneshots', oneshots)
      commit('setUpdateOneshotsDialog', true)
    },
    dialogUpdateOneshotsDisplay({commit}, value) {
      commit('setUpdateOneshotsDialog', value)
    },

    // series
    dialogUpdateSeries({commit}, series) {
      commit('setUpdateSeries', series)
      commit('setUpdateSeriesDialog', true)
    },
    dialogUpdateSeriesDisplay({commit}, value) {
      commit('setUpdateSeriesDialog', value)
    },
    dialogDeleteSeries({commit}, series) {
      commit('setDeleteSeries', series)
      commit('setDeleteSeriesDialog', true)
    },
    dialogDeleteSeriesDisplay({commit}, value) {
      commit('setDeleteSeriesDialog', value)
    },
  },
  modules: {
    persistedState: persistedModule,
  },
  plugins: [persistedState],
})
