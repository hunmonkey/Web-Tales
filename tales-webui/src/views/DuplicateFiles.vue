<template>
  <v-container fluid class="pa-6">
    <v-data-table
      v-model="selectedBooks"
      :headers="headers"
      :items="books"
      :options.sync="options"
      :server-items-length="totalBooks"
      :loading="loading"
      item-key="id"
      sort-by="fileHash"
      multi-sort
      show-select
      show-group-by
      group-by="fileHash"
      class="elevation-1"
      :footer-props="{
        itemsPerPageOptions: [20, 50, 100]
      }"
    >
      <template v-slot:item.url="{ item }">
        <router-link
          :to="{name: item.oneshot ? 'browse-oneshot' : 'browse-book', params: {bookId: item.id, seriesId: item.seriesId}}">
          {{ item.url }}
        </router-link>
      </template>

      <template v-slot:item.deleted="{ item }">
        <v-chip
          v-if="item.deleted"
          label small color="error">
          {{ $t('common.unavailable') }}
        </v-chip>
      </template>

      <template v-slot:item.id="{ item }">
        <v-btn
          icon
          color="error"
          :disabled="deleting"
          @click="promptDeleteBook(item)"
          :aria-label="$t('common.delete')"
        >
          <v-icon>mdi-delete-outline</v-icon>
        </v-btn>
      </template>

      <template v-slot:footer.prepend>
        <v-btn icon :disabled="deleting" :aria-label="$t('common.refresh')" @click="loadBooks"><v-icon>mdi-refresh</v-icon></v-btn>
        <v-btn
          text
          color="error"
          :disabled="selectedBooks.length === 0 || deleting"
          :loading="deleting"
          @click="promptDeleteBooks(selectedBooks)"
        >
          <v-icon left>mdi-delete-outline</v-icon>
          {{ $t('duplicates.delete_selected', {count: selectedBooks.length}) }}
        </v-btn>
      </template>
    </v-data-table>

    <confirmation-dialog
      v-model="deleteDialog"
      :title="deleteDialogTitle"
      :body-html="deleteDialogBody"
      :confirm-text="deleteDialogConfirmText"
      :button-confirm="$t('duplicates.delete_button')"
      button-confirm-color="error"
      @confirm="deleteBooks"
    />
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import {BookDto} from '@/types/tales-books'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import {ERROR, ErrorEvent} from '@/types/events'

export default Vue.extend({
  name: 'DuplicateFiles',
  components: {ConfirmationDialog},
  data: function () {
    return {
      books: [] as BookDto[],
      selectedBooks: [] as BookDto[],
      booksToDelete: [] as BookDto[],
      totalBooks: 0,
      loading: true,
      deleting: false,
      deleteDialog: false,
      options: {} as any,
    }
  },
  watch: {
    options: {
      handler() {
        this.loadBooks()
      },
      deep: true,
    },
  },
  computed: {
    headers(): object[] {
      return [
        {text: this.$i18n.t('duplicates.file_hash').toString(), value: 'fileHash'},
        {text: this.$i18n.t('duplicates.url').toString(), value: 'url', groupable: false},
        {text: this.$i18n.t('duplicates.size').toString(), value: 'size', groupable: false},
        {text: '', value: 'deleted', groupable: false, sortable: false},
        {text: this.$i18n.t('menu.delete').toString(), value: 'id', groupable: false},
      ]
    },
    booksToDeleteSingle(): boolean {
      return this.booksToDelete.length === 1
    },
    deleteDialogTitle(): string {
      return this.$i18n
        .t(this.booksToDeleteSingle ? 'duplicates.delete_dialog_title' : 'duplicates.delete_dialog_title_multiple')
        .toString()
    },
    deleteDialogBody(): string {
      if (this.booksToDeleteSingle) {
        return this.$i18n.t('duplicates.delete_warning_html', {name: this.booksToDelete[0].name}).toString()
      }

      return this.$i18n.t('duplicates.delete_warning_multiple_html', {count: this.booksToDelete.length}).toString()
    },
    deleteDialogConfirmText(): string {
      if (this.booksToDeleteSingle) {
        return this.$i18n.t('duplicates.confirm_delete', {name: this.booksToDelete[0].name}).toString()
      }

      return this.$i18n.t('duplicates.confirm_delete_multiple', {count: this.booksToDelete.length}).toString()
    },
  },
  methods: {
    async loadBooks() {
      this.loading = true

      const {sortBy, sortDesc, page, itemsPerPage} = this.options

      const pageRequest = {
        page: page - 1,
        size: itemsPerPage,
        sort: [],
      } as PageRequest

      for (let i = 0; i < sortBy.length; i++) {
        pageRequest.sort!!.push(`${sortBy[i]},${sortDesc[i] ? 'desc' : 'asc'}`)
      }

      const booksPage = await this.$talesBooks.getDuplicateBooks(pageRequest)
      this.totalBooks = booksPage.totalElements
      this.books = booksPage.content

      this.loading = false
    },
    promptDeleteBook(book: BookDto) {
      this.promptDeleteBooks([book])
    },
    promptDeleteBooks(books: BookDto[]) {
      if (books.length === 0) return

      this.booksToDelete = [...books]
      this.deleteDialog = true
    },
    async deleteBooks() {
      if (this.booksToDelete.length === 0) return

      this.deleting = true

      try {
        await this.$talesBooks.deleteDuplicateBooks(this.booksToDelete.map(book => book.id))
        this.selectedBooks = []
        this.booksToDelete = []
        await this.loadBooks()
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      } finally {
        this.deleting = false
      }
    },
  },
})
</script>

<style scoped>

</style>
