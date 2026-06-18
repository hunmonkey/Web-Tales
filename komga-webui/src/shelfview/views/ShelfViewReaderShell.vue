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
      data-testid="shelfview-reader-toolbar"
    >
      <v-btn
        icon
        @click="closeBook"
      >
        <v-icon>mdi-arrow-left</v-icon>
      </v-btn>

      <v-toolbar-title class="shelfview-reader-shell__title">{{ bookTitle }}</v-toolbar-title>
      <v-spacer/>

      <div
        v-if="pageControlsVisible"
        class="shelfview-reader-shell__page-controls"
        data-testid="shelfview-reader-page-controls"
      >
        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              data-testid="shelfview-reader-layout-toggle"
              v-on="on"
              @click="cycleReaderLayout"
            >
              <v-icon>{{ readerLayoutIcon }}</v-icon>
            </v-btn>
          </template>
          <span>{{ readerLayoutLabel }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              :disabled="!rtlAvailable"
              data-testid="shelfview-reader-direction-toggle"
              v-on="on"
              @click="toggleReadingDirection"
            >
              <v-icon>{{ readingDirectionIcon }}</v-icon>
            </v-btn>
          </template>
          <span>{{ readingDirectionLabel }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              data-testid="shelfview-reader-fit-toggle"
              v-on="on"
              @click="cyclePageFit"
            >
              <v-icon>{{ pageFitIcon }}</v-icon>
            </v-btn>
          </template>
          <span>{{ pageFitLabel }}</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              :disabled="!imageFiltersAvailable"
              data-testid="shelfview-reader-filter-toggle"
              v-on="on"
              @click="cycleImageFilterPreset"
            >
              <v-icon>{{ imageFilterPresetIcon }}</v-icon>
            </v-btn>
          </template>
          <span>{{ imageFilterPresetLabel }}</span>
        </v-tooltip>

        <v-btn
          icon
          :disabled="!canGoPrevious"
          aria-label="Previous page"
          data-testid="shelfview-reader-previous"
          @click="previousPage"
        >
          <v-icon>mdi-chevron-left</v-icon>
        </v-btn>

        <span
          class="shelfview-reader-shell__page-counter"
          data-testid="shelfview-reader-page-counter"
        >
          {{ currentPageLabel }} / {{ pagesCount }}
        </span>

        <v-btn
          icon
          :disabled="!canGoNext"
          aria-label="Next page"
          data-testid="shelfview-reader-next"
          @click="nextPage"
        >
          <v-icon>mdi-chevron-right</v-icon>
        </v-btn>
      </div>

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

    <main
      class="shelfview-reader-shell__stage"
      :class="stageClasses"
    >
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

      <div
        v-else-if="readerPagesVisible"
        class="shelfview-reader-shell__pages"
        :class="readerPagesClasses"
        data-testid="shelfview-reader-pages"
      >
        <img
          v-for="page in visiblePageSources"
          :key="`page-${page.pageIndex}`"
          class="shelfview-reader-shell__page shelfview-page-content"
          :class="pageImageClass"
          :style="imageFilterStyle"
          :src="page.url"
          :alt="`${bookTitle} - page ${page.pageIndex + 1}`"
          :data-page-index="page.pageIndex"
          data-testid="shelfview-reader-page"
          @error="pageImageFailed = true"
        />
      </div>
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
import {FixedPageAdapter} from '@/shelfview/adapters/fixed-page-adapter'
import {ShelfViewPageLocator} from '@/shelfview/models/shelfview-locator'
import {
  SHELFVIEW_FIXED_PAGE_FITS,
  SHELFVIEW_FIXED_PAGE_LAYOUTS,
  ShelfViewFixedPageFit,
  ShelfViewFixedPageLayout,
  ShelfViewFixedPageReadingDirection,
  shelfViewCanGoNext,
  shelfViewCanGoPrevious,
  shelfViewNextPageIndex,
  shelfViewPreviousPageIndex,
  shelfViewVisiblePageIndexes,
} from '@/shelfview/functions/fixed-page-view'
import {
  ShelfViewImageFilterPreset,
  shelfViewImageFilterPreset,
  shelfViewImageFilterStyle,
  shelfViewNextImageFilterPreset,
} from '@/shelfview/functions/image-filter-presets'

interface ShelfViewPageSource {
  pageIndex: number,
  url: string,
}

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
    adapter: undefined as FixedPageAdapter | undefined,
    currentLocator: undefined as ShelfViewPageLocator | undefined,
    context: {} as Context,
    error: undefined as Error | ShelfViewApiError | undefined,
    loading: true,
    pageImageFailed: false,
    readerLayout: 'SINGLE_PAGE' as ShelfViewFixedPageLayout,
    readingDirection: 'LTR' as ShelfViewFixedPageReadingDirection,
    pageFit: 'SCREEN' as ShelfViewFixedPageFit,
    imageFilterPreset: 'ORIGINAL' as ShelfViewImageFilterPreset,
    setupRequestId: 0,
  }),
  async mounted() {
    await this.setup(this.bookId)
  },
  beforeDestroy() {
    this.setupRequestId += 1
    this.destroyAdapter()
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
    pageControlsVisible(): boolean {
      return !!this.adapter && !this.hasDisplayError && !this.empty && this.pagesCount > 0
    },
    availableReaderLayouts(): ShelfViewFixedPageLayout[] {
      const layouts: ShelfViewFixedPageLayout[] = ['SINGLE_PAGE']
      if (this.manifest?.capabilities.doublePage) layouts.push('DOUBLE_PAGE')
      if (this.manifest?.capabilities.continuousScroll) layouts.push('CONTINUOUS_SCROLL')
      return layouts
    },
    pagesCount(): number {
      return this.adapter?.getPagesCount() || this.manifest?.pageCount || 0
    },
    currentPageIndex(): number {
      return this.currentLocator?.page || 0
    },
    currentPageNumber(): number {
      return this.currentPageIndex + 1
    },
    currentPageLabel(): string {
      const visiblePages = this.visiblePageSources.map(page => page.pageIndex + 1)
      if (visiblePages.length > 1 && this.readerLayout === 'DOUBLE_PAGE') {
        return `${Math.min(...visiblePages)}-${Math.max(...visiblePages)}`
      }
      return this.currentPageNumber.toString()
    },
    canGoPrevious(): boolean {
      return !!this.adapter && shelfViewCanGoPrevious(this.currentPageIndex, this.pagesCount, this.readerLayout)
    },
    canGoNext(): boolean {
      return !!this.adapter && shelfViewCanGoNext(this.currentPageIndex, this.pagesCount, this.readerLayout)
    },
    rtlAvailable(): boolean {
      return !!this.manifest?.capabilities.rtl
    },
    visiblePageSources(): ShelfViewPageSource[] {
      if (!this.adapter) return []

      return shelfViewVisiblePageIndexes(
        this.currentPageIndex,
        this.pagesCount,
        this.readerLayout,
        this.readingDirection,
      )
        .map(pageIndex => ({
          pageIndex,
          url: this.adapter?.getPageUrl(pageIndex),
        }))
        .filter((page): page is ShelfViewPageSource => !!page.url)
    },
    readerPagesVisible(): boolean {
      return this.visiblePageSources.length > 0
    },
    stageClasses(): object {
      return {
        'shelfview-reader-shell__stage--continuous': this.readerLayout === 'CONTINUOUS_SCROLL',
      }
    },
    readerPagesClasses(): object {
      return {
        'shelfview-reader-shell__pages--single': this.readerLayout === 'SINGLE_PAGE',
        'shelfview-reader-shell__pages--double': this.readerLayout === 'DOUBLE_PAGE',
        'shelfview-reader-shell__pages--continuous': this.readerLayout === 'CONTINUOUS_SCROLL',
      }
    },
    pageImageClass(): object {
      return {
        'shelfview-reader-shell__page--fit-screen': this.pageFit === 'SCREEN',
        'shelfview-reader-shell__page--fit-width': this.pageFit === 'WIDTH',
        'shelfview-reader-shell__page--fit-height': this.pageFit === 'HEIGHT',
        'shelfview-reader-shell__page--fit-original': this.pageFit === 'ORIGINAL',
      }
    },
    imageFiltersAvailable(): boolean {
      return !!this.manifest?.capabilities.imageFilters
    },
    imageFilterStyle(): Record<string, string> {
      return this.imageFiltersAvailable ? shelfViewImageFilterStyle(this.imageFilterPreset) : shelfViewImageFilterStyle('ORIGINAL')
    },
    imageFilterPresetIcon(): string {
      return shelfViewImageFilterPreset(this.imageFilterPreset).icon
    },
    imageFilterPresetLabel(): string {
      return shelfViewImageFilterPreset(this.imageFilterPreset).label
    },
    readerLayoutIcon(): string {
      switch (this.readerLayout) {
        case 'DOUBLE_PAGE':
          return 'mdi-book-open-variant'
        case 'CONTINUOUS_SCROLL':
          return 'mdi-view-sequential'
        default:
          return 'mdi-page-layout-body'
      }
    },
    readerLayoutLabel(): string {
      switch (this.readerLayout) {
        case 'DOUBLE_PAGE':
          return 'Double page'
        case 'CONTINUOUS_SCROLL':
          return 'Continuous scroll'
        default:
          return 'Single page'
      }
    },
    readingDirectionIcon(): string {
      return this.readingDirection === 'RTL' ? 'mdi-format-textdirection-r-to-l' : 'mdi-format-textdirection-l-to-r'
    },
    readingDirectionLabel(): string {
      return this.readingDirection === 'RTL' ? 'Right to left' : 'Left to right'
    },
    pageFitIcon(): string {
      switch (this.pageFit) {
        case 'WIDTH':
          return 'mdi-arrow-expand-horizontal'
        case 'HEIGHT':
          return 'mdi-arrow-expand-vertical'
        case 'ORIGINAL':
          return 'mdi-image-size-select-large'
        default:
          return 'mdi-fit-to-screen-outline'
      }
    },
    pageFitLabel(): string {
      switch (this.pageFit) {
        case 'WIDTH':
          return 'Fit width'
        case 'HEIGHT':
          return 'Fit height'
        case 'ORIGINAL':
          return 'Original size'
        default:
          return 'Fit screen'
      }
    },
    empty(): boolean {
      return !!this.manifest && !this.readerPagesVisible
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
      this.destroyAdapter()
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

        if (this.manifest.kind === 'FIXED_PAGE_SEQUENCE') {
          await this.openFixedPageManifest(this.manifest)
        }
        if (requestId !== this.setupRequestId) return

        document.title = `Komga - ${this.bookTitle}`
      } catch (e) {
        if (requestId === this.setupRequestId) this.error = e
      } finally {
        if (requestId === this.setupRequestId) this.loading = false
      }
    },
    async openFixedPageManifest(manifest: ShelfViewManifest) {
      const adapter = new FixedPageAdapter()
      await adapter.open(manifest)
      this.adapter = adapter
      this.normalizeReaderLayout()
      this.syncAdapterState()
    },
    destroyAdapter() {
      this.adapter?.destroy()
      this.adapter = undefined
      this.currentLocator = undefined
    },
    syncAdapterState() {
      if (!this.adapter) {
        this.currentLocator = undefined
        return
      }

      this.currentLocator = this.adapter.getCurrentLocator()
    },
    async previousPage() {
      if (!this.adapter) return

      await this.goToPageIndex(shelfViewPreviousPageIndex(this.currentPageIndex, this.pagesCount, this.readerLayout))
    },
    async nextPage() {
      if (!this.adapter) return

      await this.goToPageIndex(shelfViewNextPageIndex(this.currentPageIndex, this.pagesCount, this.readerLayout))
    },
    async goToPageIndex(pageIndex: number) {
      if (!this.adapter) return

      this.pageImageFailed = false
      await this.adapter.goTo({
        ...this.adapter.getCurrentLocator(),
        page: pageIndex,
      })
      this.syncAdapterState()
    },
    normalizeReaderLayout() {
      if (!this.availableReaderLayouts.includes(this.readerLayout)) {
        this.readerLayout = 'SINGLE_PAGE'
      }
    },
    cycleReaderLayout() {
      const layouts = this.availableReaderLayouts.length > 0 ? this.availableReaderLayouts : SHELFVIEW_FIXED_PAGE_LAYOUTS
      const currentIndex = layouts.indexOf(this.readerLayout)
      this.readerLayout = layouts[(currentIndex + 1) % layouts.length]
      this.syncAdapterState()
    },
    toggleReadingDirection() {
      if (!this.rtlAvailable) return

      this.readingDirection = this.readingDirection === 'RTL' ? 'LTR' : 'RTL'
      this.syncAdapterState()
    },
    cyclePageFit() {
      const currentIndex = SHELFVIEW_FIXED_PAGE_FITS.indexOf(this.pageFit)
      this.pageFit = SHELFVIEW_FIXED_PAGE_FITS[(currentIndex + 1) % SHELFVIEW_FIXED_PAGE_FITS.length]
    },
    cycleImageFilterPreset() {
      if (!this.imageFiltersAvailable) return

      this.imageFilterPreset = shelfViewNextImageFilterPreset(this.imageFilterPreset)
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

.shelfview-reader-shell__title {
  max-width: min(54vw, 720px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelfview-reader-shell__page-controls {
  align-items: center;
  display: flex;
  gap: 4px;
  margin-right: 4px;
}

.shelfview-reader-shell__page-counter {
  font-size: 14px;
  min-width: 56px;
  text-align: center;
}

.shelfview-reader-shell__stage {
  align-items: center;
  display: flex;
  height: 100%;
  justify-content: center;
  overflow: hidden;
  padding: 56px 16px 16px;
  width: 100%;
}

.shelfview-reader-shell__stage--continuous {
  align-items: stretch;
  justify-content: flex-start;
  overflow-y: auto;
}

.shelfview-reader-shell__pages {
  align-items: center;
  display: flex;
  gap: 8px;
  height: 100%;
  justify-content: center;
  width: 100%;
}

.shelfview-reader-shell__pages--continuous {
  flex-direction: column;
  height: auto;
  justify-content: flex-start;
  min-height: 100%;
}

.shelfview-reader-shell__page {
  display: block;
  object-fit: contain;
}

.shelfview-page-content {
  filter:
    brightness(var(--sv-brightness, 1))
    contrast(var(--sv-contrast, 1))
    grayscale(var(--sv-grayscale, 0))
    sepia(var(--sv-sepia, 0))
    invert(var(--sv-invert, 0));
}

.shelfview-reader-shell__page--fit-screen {
  max-height: calc(100vh - 72px);
  max-width: 100%;
}

.shelfview-reader-shell__page--fit-width {
  height: auto;
  max-width: 100%;
  width: 100%;
}

.shelfview-reader-shell__page--fit-height {
  height: calc(100vh - 72px);
  max-width: 100%;
  width: auto;
}

.shelfview-reader-shell__page--fit-original {
  height: auto;
  max-height: none;
  max-width: none;
  width: auto;
}

.shelfview-reader-shell__pages--double .shelfview-reader-shell__page--fit-screen {
  max-width: calc(50vw - 24px);
}

.shelfview-reader-shell__pages--double .shelfview-reader-shell__page--fit-width {
  width: calc(50vw - 24px);
}

.shelfview-reader-shell__pages--continuous .shelfview-reader-shell__page {
  margin-bottom: 12px;
}

.shelfview-reader-shell__pages--continuous .shelfview-reader-shell__page--fit-screen,
.shelfview-reader-shell__pages--continuous .shelfview-reader-shell__page--fit-width {
  height: auto;
  max-height: none;
  max-width: min(100%, 1200px);
  width: 100%;
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

@media (max-width: 600px) {
  .shelfview-reader-shell__title {
    max-width: 38vw;
  }

  .shelfview-reader-shell__page-counter {
    font-size: 13px;
    min-width: 44px;
  }

  .shelfview-reader-shell__page-controls {
    gap: 0;
  }
}
</style>
