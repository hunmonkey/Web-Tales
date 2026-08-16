<template>
  <div>
    <library-edit-dialog
      v-model="editLibraryDialog"
      :library="editLibrary"
    />

    <confirmation-dialog
      v-model="deleteLibraryDialog"
      :title="$t('dialog.delete_library.title')"
      :body-html="$t('dialog.delete_library.warning_html', {name: libraryToDelete.name})"
      :confirm-text="$t('dialog.delete_library.confirm_delete', {name: libraryToDelete.name})"
      :button-confirm="$t('dialog.delete_library.button_confirm')"
      button-confirm-color="error"
      @confirm="deleteLibrary"
    />

    <edit-books-dialog
      v-model="updateBooksDialog"
      :books="updateBooks"
    />

    <bulk-edit-books-dialog
      v-model="updateBulkBooksDialog"
      :books="updateBulkBooks"
    />

    <edit-oneshot-dialog
      v-model="updateOneshotsDialog"
      :oneshots="updateOneshots"
    />

    <edit-series-dialog
      v-model="updateSeriesDialog"
      :series="updateSeries"
    />

    <confirmation-dialog
      v-model="deleteSeriesDialog"
      :title="$t('dialog.delete_series.dialog_title')"
      :body-html="seriesToDeleteSingle ? $t('dialog.delete_series.warning_html', {name: seriesToDelete.name}) : $t('dialog.delete_series.warning_multiple_html', {count: seriesToDelete.length})"
      :confirm-text="seriesToDeleteSingle ? $t('dialog.delete_series.confirm_delete', {name: seriesToDelete.name}) : $t('dialog.delete_series.confirm_delete_multiple', {count: seriesToDelete.length})"
      :button-confirm="$t('dialog.delete_series.button_confirm')"
      button-confirm-color="error"
      @confirm="deleteSeries"
    />

    <confirmation-dialog
      v-model="deleteBookDialog"
      :title="booksToDeleteSingle ? $t('dialog.delete_book.dialog_title') : $t('dialog.delete_book.dialog_title_multiple')"
      :body-html="booksToDeleteSingle ? $t('dialog.delete_book.warning_html', {name: booksToDelete.name}) : $t('dialog.delete_book.warning_multiple_html', {count: booksToDelete.length})"
      :confirm-text="booksToDeleteSingle ? $t('dialog.delete_book.confirm_delete', {name: booksToDelete.name}) : $t('dialog.delete_book.confirm_delete_multiple', {count: booksToDelete.length})"
      :button-confirm="$t('dialog.delete_book.button_confirm')"
      button-confirm-color="error"
      @confirm="deleteBooks"
    />

  </div>
</template>

<script lang="ts">
import EditBooksDialog from '@/components/dialogs/EditBooksDialog.vue'
import EditSeriesDialog from '@/components/dialogs/EditSeriesDialog.vue'
import LibraryEditDialog from '@/components/dialogs/LibraryEditDialog.vue'
import Vue from 'vue'
import {BookDto} from '@/types/tales-books'
import {Oneshot, SeriesDto} from '@/types/tales-series'
import {ERROR} from '@/types/events'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import {LibraryDto} from '@/types/tales-libraries'
import BulkEditBooksDialog from '@/components/dialogs/BulkEditBooksDialog.vue'
import EditOneshotDialog from '@/components/dialogs/EditOneshotDialog.vue'

export default Vue.extend({
  name: 'ReusableDialogs',
  components: {
    EditOneshotDialog,
    BulkEditBooksDialog,
    ConfirmationDialog,
    LibraryEditDialog,
    EditBooksDialog,
    EditSeriesDialog,
  },
  computed: {
    // libraries
    editLibraryDialog: {
      get(): boolean {
        return this.$store.state.editLibraryDialog
      },
      set(val) {
        this.$store.dispatch('dialogEditLibraryDisplay', val)
      },
    },
    editLibrary(): LibraryDto | undefined {
      return this.$store.state.editLibrary
    },
    deleteLibraryDialog: {
      get(): boolean {
        return this.$store.state.deleteLibraryDialog
      },
      set(val) {
        this.$store.dispatch('dialogDeleteLibraryDisplay', val)
      },
    },
    libraryToDelete(): LibraryDto {
      return this.$store.state.deleteLibrary
    },
    // books
    updateBooksDialog: {
      get(): boolean {
        return this.$store.state.updateBooksDialog
      },
      set(val) {
        this.$store.dispatch('dialogUpdateBooksDisplay', val)
      },
    },
    updateBooks(): BookDto | BookDto[] {
      return this.$store.state.updateBooks
    },
    // books bulk
    updateBulkBooksDialog: {
      get(): boolean {
        return this.$store.state.updateBulkBooksDialog
      },
      set(val) {
        this.$store.dispatch('dialogUpdateBulkBooksDisplay', val)
      },
    },
    updateBulkBooks(): BookDto[] {
      return this.$store.state.updateBulkBooks
    },
    deleteBookDialog: {
      get(): boolean {
        return this.$store.state.deleteBookDialog
      },
      set(val) {
        this.$store.dispatch('dialogDeleteBookDisplay', val)
      },
    },
    booksToDelete(): BookDto | BookDto[] {
      return this.$store.state.deleteBooks
    },
    booksToDeleteSingle(): boolean {
      return !Array.isArray(this.booksToDelete)
    },
    // oneshots
    updateOneshotsDialog: {
      get(): boolean {
        return this.$store.state.updateOneshotsDialog
      },
      set(val) {
        this.$store.dispatch('dialogUpdateOneshotsDisplay', val)
      },
    },
    updateOneshots(): Oneshot | Oneshot[] {
      return this.$store.state.updateOneshots
    },
    // series
    updateSeriesDialog: {
      get(): boolean {
        return this.$store.state.updateSeriesDialog
      },
      set(val) {
        this.$store.dispatch('dialogUpdateSeriesDisplay', val)
      },
    },
    updateSeries(): SeriesDto | SeriesDto[] {
      return this.$store.state.updateSeries
    },
    deleteSeriesDialog: {
      get(): boolean {
        return this.$store.state.deleteSeriesDialog
      },
      set(val) {
        this.$store.dispatch('dialogDeleteSeriesDisplay', val)
      },
    },
    seriesToDelete(): SeriesDto | SeriesDto[] {
      return this.$store.state.deleteSeries
    },
    seriesToDeleteSingle(): boolean {
      return !Array.isArray(this.seriesToDelete)
    },
  },
  methods: {
    async deleteLibrary() {
      try {
        await this.$store.dispatch('deleteLibrary', this.libraryToDelete)
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
    },
    async deleteSeries() {
      const toUpdate = (this.seriesToDeleteSingle ? [this.seriesToDelete] : this.seriesToDelete) as SeriesDto[]
      for (const b of toUpdate) {
        try {
          await this.$talesSeries.deleteSeries(b.id)
        } catch (e) {
          this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
        }
      }
    },
    async deleteBooks() {
      const toUpdate = (this.booksToDeleteSingle ? [this.booksToDelete] : this.booksToDelete) as BookDto[]
      for (const b of toUpdate) {
        try {
          await this.$talesBooks.deleteBook(b.id)
        } catch (e) {
          this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
        }
      }
    },
  },
})
</script>

<style scoped>

</style>
