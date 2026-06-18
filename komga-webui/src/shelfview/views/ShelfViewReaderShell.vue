<template>
  <v-container
    class="ma-0 pa-0 shelfview-reader-shell"
    fluid
    data-testid="shelfview-reader-shell"
  >
    <v-toolbar
      dense
      elevation="1"
      class="shelfview-reader-shell__toolbar full-width"
    >
      <v-btn
        icon
        @click="closeBook"
      >
        <v-icon>mdi-arrow-left</v-icon>
      </v-btn>

      <v-toolbar-title>{{ bookTitle }}</v-toolbar-title>
      <v-spacer/>

      <v-tooltip bottom>
        <template v-slot:activator="{ on }">
          <v-btn
            icon
            :to="legacyReaderLocation"
            v-on="on"
          >
            <v-icon>mdi-book-open-page-variant</v-icon>
          </v-btn>
        </template>
        <span>{{ $t('browse_book.read_book') }}</span>
      </v-tooltip>
    </v-toolbar>

    <main class="shelfview-reader-shell__stage">
      <v-progress-circular
        v-if="loading"
        indeterminate
        color="primary"
        data-testid="shelfview-reader-loading"
      />
    </main>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import {RawLocation} from 'vue-router/types/router'
import {BookDto} from '@/types/komga-books'
import {SeriesDto} from '@/types/komga-series'
import {Context, ContextOrigin} from '@/types/context'
import {getBookTitleCompact} from '@/functions/book-title'
import {getLegacyBookReadRouteFromMedia} from '@/functions/book-format'

export default Vue.extend({
  name: 'ShelfViewReaderShell',
  props: {
    bookId: {
      type: String,
      required: true,
    },
  },
  data: () => ({
    book: undefined as unknown as BookDto,
    series: undefined as unknown as SeriesDto,
    context: {} as Context,
    loading: true,
  }),
  async mounted() {
    await this.setup(this.bookId)
  },
  async beforeRouteUpdate(to, from, next) {
    if (to.params.bookId !== from.params.bookId) {
      await this.setup(to.params.bookId)
    }
    next()
  },
  computed: {
    bookTitle(): string {
      if (this.book && this.series) {
        return getBookTitleCompact(this.book.metadata.title, this.series.metadata.title, this.book.oneshot ? undefined : this.book.metadata.number)
      }
      return 'ShelfView'
    },
    legacyReaderLocation(): RawLocation {
      return {
        name: this.book?.media ? getLegacyBookReadRouteFromMedia(this.book.media) : 'read-book',
        params: {bookId: this.bookId},
        query: this.$route.query,
      }
    },
  },
  methods: {
    async setup(bookId: string) {
      this.loading = true

      this.context = {} as Context
      if (this.$route.query.contextId && this.$route.query.context &&
        Object.values(ContextOrigin).includes(this.$route.query.context as ContextOrigin)) {
        this.context = {
          origin: this.$route.query.context as ContextOrigin,
          id: this.$route.query.contextId as string,
        }
      }

      this.book = await this.$komgaBooks.getBook(bookId)
      this.series = await this.$komgaSeries.getOneSeries(this.book.seriesId)
      this.book.context = this.context
      document.title = `Komga - ${this.bookTitle}`
      this.loading = false
    },
    closeBook() {
      if (this.book) {
        this.$router.push({
          name: this.book.oneshot ? 'browse-oneshot' : 'browse-book',
          params: {bookId: this.bookId, seriesId: this.book.seriesId},
          query: {context: this.context.origin, contextId: this.context.id},
        })
      } else {
        this.$router.push({name: 'browse-book', params: {bookId: this.bookId}})
      }
    },
  },
})
</script>

<style scoped>
.shelfview-reader-shell {
  background: #111111;
  height: 100vh;
  width: 100%;
}

.shelfview-reader-shell__toolbar {
  position: fixed;
  top: 0;
  z-index: 2;
}

.shelfview-reader-shell__stage {
  align-items: center;
  display: flex;
  height: 100%;
  justify-content: center;
  width: 100%;
}

.full-width {
  width: 100%;
}
</style>
