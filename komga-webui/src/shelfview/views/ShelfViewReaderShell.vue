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

      <section
        v-else-if="hasDisplayError"
        class="shelfview-reader-shell__status"
        data-testid="shelfview-reader-error"
      >
        <v-icon
          size="36"
          color="error"
        >
          mdi-alert-circle-outline
        </v-icon>
        <h2>{{ displayErrorTitle }}</h2>
        <p>{{ displayErrorMessage }}</p>
        <v-btn
          color="primary"
          :to="legacyReaderLocation"
        >
          {{ $t('browse_book.read_book') }}
        </v-btn>
      </section>

      <section
        v-else-if="empty"
        class="shelfview-reader-shell__status"
        data-testid="shelfview-reader-empty"
      >
        <v-icon
          size="36"
          color="grey lighten-1"
        >
          mdi-book-open-blank-variant
        </v-icon>
        <h2>ShelfView</h2>
        <p>{{ emptyMessage }}</p>
      </section>

      <img
        v-else-if="firstPageUrl"
        class="shelfview-reader-shell__page"
        :src="firstPageUrl"
        :alt="`${bookTitle} - page 1`"
        data-testid="shelfview-reader-page"
        @error="pageImageFailed = true"
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
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {ShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {shelfViewPageUrl} from '@/shelfview/functions/shelfview-urls'

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
    manifest: undefined as unknown as ShelfViewManifest,
    context: {} as Context,
    error: undefined as Error | ShelfViewApiError | undefined,
    loading: true,
    pageImageFailed: false,
    setupRequestId: 0,
  }),
  async mounted() {
    await this.setup(this.bookId)
  },
  beforeDestroy() {
    this.setupRequestId += 1
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
    firstPageUrl(): string | undefined {
      if (!this.manifest) return undefined
      if (this.manifest.pageCount !== undefined && this.manifest.pageCount < 1) return undefined
      return shelfViewPageUrl(this.manifest, 0)
    },
    empty(): boolean {
      return !!this.manifest && !this.firstPageUrl
    },
    emptyMessage(): string {
      if (this.manifest?.kind && this.manifest.kind !== 'FIXED_PAGE_SEQUENCE') {
        return 'This ShelfView format is not ready yet.'
      }
      return 'No readable pages are available for this book.'
    },
    hasDisplayError(): boolean {
      return !!this.error || this.pageImageFailed
    },
    displayErrorTitle(): string {
      return this.pageImageFailed ? 'Unable to load this page' : 'Unable to open this book'
    },
    displayErrorMessage(): string {
      if (this.pageImageFailed) {
        return 'The first page image could not be loaded.'
      }
      if (this.error instanceof ShelfViewApiError && this.error.code === 'BAD_REQUEST') {
        return 'This book is not supported by the ShelfView reader yet.'
      }
      return this.error?.message || 'ShelfView could not load this book.'
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
      const requestId = this.setupRequestId + 1
      this.setupRequestId = requestId
      this.loading = true
      this.error = undefined
      this.manifest = undefined as unknown as ShelfViewManifest
      this.pageImageFailed = false

      this.context = {} as Context
      if (this.$route.query.contextId && this.$route.query.context &&
        Object.values(ContextOrigin).includes(this.$route.query.context as ContextOrigin)) {
        this.context = {
          origin: this.$route.query.context as ContextOrigin,
          id: this.$route.query.contextId as string,
        }
      }

      try {
        this.book = await this.$komgaBooks.getBook(bookId)
        if (requestId !== this.setupRequestId) return

        this.series = await this.$komgaSeries.getOneSeries(this.book.seriesId)
        if (requestId !== this.setupRequestId) return

        this.book.context = this.context
        this.manifest = await this.$shelfViewManifest.getManifest(bookId)
        if (requestId !== this.setupRequestId) return

        document.title = `Komga - ${this.bookTitle}`
      } catch (e) {
        if (requestId === this.setupRequestId) this.error = e
      } finally {
        if (requestId === this.setupRequestId) this.loading = false
      }
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
  overflow: hidden;
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
  padding: 56px 16px 16px;
  width: 100%;
}

.shelfview-reader-shell__page {
  max-height: calc(100vh - 72px);
  max-width: 100%;
  object-fit: contain;
}

.shelfview-reader-shell__status {
  align-items: center;
  color: #f5f5f5;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 520px;
  text-align: center;
}

.shelfview-reader-shell__status h2 {
  font-size: 20px;
  font-weight: 500;
  margin: 0;
}

.shelfview-reader-shell__status p {
  color: #d0d0d0;
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}

.full-width {
  width: 100%;
}
</style>
