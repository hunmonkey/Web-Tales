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
        aria-label="Close reader"
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
              :aria-label="readerLayoutLabel"
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
              :aria-label="readingDirectionLabel"
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
              :aria-label="pageFitLabel"
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
              :aria-label="imageFilterPresetLabel"
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

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              aria-label="Bookmarks"
              data-testid="shelfview-reader-bookmarks-toggle"
              v-on="on"
              @click="bookmarksDrawerOpen = true"
            >
              <v-icon>mdi-bookmark-multiple-outline</v-icon>
            </v-btn>
          </template>
          <span>Bookmarks</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              aria-label="Add bookmark"
              data-testid="shelfview-reader-bookmark-add"
              v-on="on"
              @click="addBookmarkForCurrentPage"
            >
              <v-icon>mdi-bookmark-plus-outline</v-icon>
            </v-btn>
          </template>
          <span>Add bookmark</span>
        </v-tooltip>

        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-btn
              icon
              aria-label="Show keyboard shortcuts"
              data-testid="shelfview-reader-shortcuts-toggle"
              v-on="on"
              @click="showShortcutHelp = true"
            >
              <v-icon>mdi-keyboard-outline</v-icon>
            </v-btn>
          </template>
          <span>Shortcuts</span>
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

      <span
        v-if="readerStateNoticeVisible"
        class="shelfview-reader-shell__sync-status"
        :class="readerStateNoticeClass"
        data-testid="shelfview-reader-sync-status"
      >
        {{ readerStateMessage }}
      </span>

      <v-tooltip bottom>
        <template v-slot:activator="{ on }">
          <v-btn
            icon
            aria-label="Open legacy reader"
            :to="legacyReaderLocation"
            v-on="on"
          >
            <v-icon>mdi-book-open-page-variant</v-icon>
          </v-btn>
        </template>
        <span>{{ $t('browse_book.read_book') }}</span>
      </v-tooltip>
    </v-toolbar>

    <p
      id="shelfview-reader-shortcut-hint"
      class="shelfview-reader-shell__sr-only"
    >
      Use arrow keys, Page Up, Page Down, Home, End and touch swipes to navigate. Press question mark for shortcuts.
    </p>

    <p
      class="shelfview-reader-shell__sr-only"
      aria-live="polite"
      data-testid="shelfview-reader-live-status"
    >
      {{ readerLiveStatus }}
    </p>

    <main
      ref="readerStage"
      class="shelfview-reader-shell__stage"
      :class="stageClasses"
      tabindex="0"
      role="application"
      :aria-label="readerAriaLabel"
      aria-describedby="shelfview-reader-shortcut-hint"
      data-testid="shelfview-reader-stage"
      @keydown="onReaderKeydown"
      @scroll="onReaderStageScroll"
      @touchstart="onReaderTouchStart"
      @touchend="onReaderTouchEnd"
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

    <aside
      v-if="bookmarksDrawerOpen"
      class="shelfview-reader-shell__bookmarks-drawer"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shelfview-bookmarks-title"
      data-testid="shelfview-bookmarks-drawer"
    >
      <header class="shelfview-reader-shell__bookmarks-header">
        <h2 id="shelfview-bookmarks-title">Bookmarks</h2>
        <v-btn
          icon
          aria-label="Close bookmarks"
          data-testid="shelfview-bookmarks-close"
          @click="closeBookmarksDrawer"
        >
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </header>

      <v-btn
        block
        color="primary"
        data-testid="shelfview-bookmarks-add"
        @click="addBookmarkForCurrentPage"
      >
        <v-icon left>mdi-bookmark-plus-outline</v-icon>
        Save page
      </v-btn>

      <p
        v-if="bookmarks.length === 0"
        class="shelfview-reader-shell__bookmarks-empty"
        data-testid="shelfview-bookmarks-empty"
      >
        No bookmarks
      </p>

      <ul
        v-else
        class="shelfview-reader-shell__bookmarks-list"
      >
        <li
          v-for="bookmark in bookmarks"
          :key="bookmark.id"
          class="shelfview-reader-shell__bookmark-row"
        >
          <button
            type="button"
            class="shelfview-reader-shell__bookmark-link"
            data-testid="shelfview-bookmark-go"
            @click="goToBookmark(bookmark)"
          >
            {{ bookmarkLabel(bookmark) }}
          </button>
          <v-btn
            icon
            small
            aria-label="Delete bookmark"
            data-testid="shelfview-bookmark-delete"
            @click="deleteBookmark(bookmark)"
          >
            <v-icon small>mdi-delete-outline</v-icon>
          </v-btn>
        </li>
      </ul>
    </aside>

    <section
      v-if="showShortcutHelp"
      class="shelfview-reader-shell__shortcut-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="shelfview-reader-shortcuts-title"
      data-testid="shelfview-reader-shortcuts"
    >
      <div class="shelfview-reader-shell__shortcut-panel">
        <h2 id="shelfview-reader-shortcuts-title">Shortcuts</h2>
        <dl>
          <template v-for="shortcut in shortcutRows">
            <dt :key="`${shortcut.keys}-keys`">{{ shortcut.keys }}</dt>
            <dd :key="`${shortcut.keys}-label`">{{ shortcut.label }}</dd>
          </template>
        </dl>
        <v-btn
          color="primary"
          data-testid="shelfview-reader-shortcuts-close"
          @click="closeShortcutHelp"
        >
          Close
        </v-btn>
      </div>
    </section>
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
  ShelfViewBookmarkDto,
  ShelfViewFixedPagePreferenceSettings,
  ShelfViewPositionDto,
} from '@/shelfview/models/shelfview-state'
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
import {
  ShelfViewReaderInputAction,
  ShelfViewTouchPoint,
  shelfViewReaderKeyboardAction,
  shelfViewShouldIgnoreShortcutTarget,
  shelfViewTouchAction,
} from '@/shelfview/functions/reader-input'

interface ShelfViewPageSource {
  pageIndex: number,
  url: string,
}

type ShelfViewReaderStateStatus =
  | 'IDLE'
  | 'PENDING'
  | 'SAVING'
  | 'SAVED'
  | 'ERROR'
  | 'OFFLINE'

const SHELFVIEW_FIXED_PAGE_FORMAT = 'FIXED_PAGE_SEQUENCE'
const POSITION_SAVE_DEBOUNCE_MS = 2000
const PREFERENCE_SAVE_DEBOUNCE_MS = 600

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
    showShortcutHelp: false,
    bookmarks: [] as ShelfViewBookmarkDto[],
    bookmarksDrawerOpen: false,
    readerStateStatus: 'IDLE' as ShelfViewReaderStateStatus,
    readerStateMessage: '',
    positionSaveTimer: undefined as number | undefined,
    preferenceSaveTimer: undefined as number | undefined,
    touchStart: undefined as ShelfViewTouchPoint | undefined,
    setupRequestId: 0,
  }),
  async mounted() {
    window.addEventListener('online', this.onReaderOnline)
    window.addEventListener('offline', this.onReaderOffline)
    window.addEventListener('pagehide', this.onPageHide)
    document.addEventListener('visibilitychange', this.onVisibilityChange)
    await this.setup(this.bookId)
  },
  beforeDestroy() {
    this.setupRequestId += 1
    void this.flushReaderState()
    this.clearReaderStateTimers()
    window.removeEventListener('online', this.onReaderOnline)
    window.removeEventListener('offline', this.onReaderOffline)
    window.removeEventListener('pagehide', this.onPageHide)
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
    this.destroyAdapter()
  },
  async beforeRouteUpdate(to, from, next) {
    if (to.params.bookId !== from.params.bookId) {
      await this.flushReaderState()
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
    readerStateNoticeVisible(): boolean {
      return this.readerStateStatus !== 'IDLE' && this.readerStateMessage.length > 0
    },
    readerStateNoticeClass(): object {
      return {
        'shelfview-reader-shell__sync-status--error': this.readerStateStatus === 'ERROR' || this.readerStateStatus === 'OFFLINE',
        'shelfview-reader-shell__sync-status--saved': this.readerStateStatus === 'SAVED',
      }
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
    readerAriaLabel(): string {
      return `${this.bookTitle} reader, page ${this.currentPageLabel} of ${this.pagesCount}`
    },
    readerLiveStatus(): string {
      return `Page ${this.currentPageLabel} of ${this.pagesCount}`
    },
    shortcutRows(): Array<{ keys: string, label: string }> {
      return [
        {keys: 'Arrow Left / Arrow Right', label: 'Move pages'},
        {keys: 'Page Up / Page Down', label: 'Move pages'},
        {keys: 'Space', label: 'Next page'},
        {keys: 'Home / End', label: 'First or last page'},
        {keys: '? / F1', label: 'Show shortcuts'},
      ]
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
      this.bookmarks = []
      this.bookmarksDrawerOpen = false
      this.clearReaderStateTimers()
      this.setReaderStateStatus('IDLE', '')
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
          const savedPosition = await this.loadShelfViewState(bookId)
          if (requestId !== this.setupRequestId) return

          await this.openFixedPageManifest(this.manifest, savedPosition)
        }
        if (requestId !== this.setupRequestId) return

        document.title = `Komga - ${this.bookTitle}`
      } catch (e) {
        if (requestId === this.setupRequestId) this.error = e
      } finally {
        if (requestId === this.setupRequestId) this.loading = false
      }
    },
    async loadShelfViewState(bookId: string): Promise<ShelfViewPositionDto | undefined> {
      try {
        const [savedPosition, bookmarks, preference] = await Promise.all([
          this.$shelfViewState.getPosition(bookId),
          this.$shelfViewState.getBookmarks(bookId),
          this.$shelfViewState.getPreference({
            scopeType: 'BOOK',
            scopeId: bookId,
            format: SHELFVIEW_FIXED_PAGE_FORMAT,
          }),
        ])

        this.bookmarks = bookmarks
        this.applyPreferenceSettings(preference.effectiveSettings, false)
        this.setReaderStateStatus('SAVED', 'Restored')

        return savedPosition
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', 'Reader state unavailable')
        return undefined
      }
    },
    async openFixedPageManifest(
      manifest: ShelfViewManifest,
      savedPosition?: ShelfViewPositionDto,
    ) {
      const adapter = new FixedPageAdapter()
      await adapter.open(manifest)
      this.adapter = adapter
      this.normalizeReaderLayout()
      await this.restoreSavedPosition(savedPosition)
      this.syncAdapterState()
      this.$nextTick(() => this.focusReaderStage())
    },
    async restoreSavedPosition(savedPosition?: ShelfViewPositionDto) {
      if (!this.adapter || !savedPosition) return

      try {
        await this.adapter.goTo(savedPosition.locator)
      } catch (e) {
        this.setReaderStateStatus('ERROR', 'Saved page unavailable')
      }
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
      this.schedulePositionSave()
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
      this.schedulePreferenceSave()
    },
    toggleReadingDirection() {
      if (!this.rtlAvailable) return

      this.readingDirection = this.readingDirection === 'RTL' ? 'LTR' : 'RTL'
      this.syncAdapterState()
      this.schedulePreferenceSave()
    },
    cyclePageFit() {
      const currentIndex = SHELFVIEW_FIXED_PAGE_FITS.indexOf(this.pageFit)
      this.pageFit = SHELFVIEW_FIXED_PAGE_FITS[(currentIndex + 1) % SHELFVIEW_FIXED_PAGE_FITS.length]
      this.schedulePreferenceSave()
    },
    cycleImageFilterPreset() {
      if (!this.imageFiltersAvailable) return

      this.imageFilterPreset = shelfViewNextImageFilterPreset(this.imageFilterPreset)
      this.schedulePreferenceSave()
    },
    applyPreferenceSettings(
      settings: ShelfViewFixedPagePreferenceSettings,
      persist = false,
    ) {
      if (settings.layout && this.availableReaderLayouts.includes(settings.layout)) {
        this.readerLayout = settings.layout
      }
      if (settings.readingDirection && (settings.readingDirection === 'LTR' || (settings.readingDirection === 'RTL' && this.rtlAvailable))) {
        this.readingDirection = settings.readingDirection
      }
      if (settings.pageFit && SHELFVIEW_FIXED_PAGE_FITS.includes(settings.pageFit)) {
        this.pageFit = settings.pageFit
      }
      if (settings.imageFilterPreset && this.imageFiltersAvailable) {
        this.imageFilterPreset = settings.imageFilterPreset
      }
      if (!this.imageFiltersAvailable) this.imageFilterPreset = 'ORIGINAL'

      this.normalizeReaderLayout()
      if (persist) this.schedulePreferenceSave()
    },
    currentPreferenceSettings(): ShelfViewFixedPagePreferenceSettings {
      return {
        layout: this.readerLayout,
        readingDirection: this.readingDirection,
        pageFit: this.pageFit,
        imageFilterPreset: this.imageFilterPreset,
      }
    },
    schedulePositionSave() {
      if (!this.adapter || !this.currentLocator) return

      if (this.positionSaveTimer !== undefined) window.clearTimeout(this.positionSaveTimer)
      this.setReaderStateStatus('PENDING', 'Saving')
      this.positionSaveTimer = window.setTimeout(() => {
        void this.flushPositionSave()
      }, POSITION_SAVE_DEBOUNCE_MS)
    },
    schedulePreferenceSave() {
      if (!this.bookId || !this.manifest || this.manifest.kind !== 'FIXED_PAGE_SEQUENCE') return

      if (this.preferenceSaveTimer !== undefined) window.clearTimeout(this.preferenceSaveTimer)
      this.setReaderStateStatus('PENDING', 'Saving')
      this.preferenceSaveTimer = window.setTimeout(() => {
        void this.flushPreferenceSave()
      }, PREFERENCE_SAVE_DEBOUNCE_MS)
    },
    async flushReaderState() {
      await Promise.all([
        this.flushPositionSave(),
        this.flushPreferenceSave(),
      ])
    },
    async flushPositionSave() {
      if (this.positionSaveTimer !== undefined) {
        window.clearTimeout(this.positionSaveTimer)
        this.positionSaveTimer = undefined
      }
      if (!this.adapter || !this.currentLocator) return
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', 'Offline')
        return
      }

      try {
        this.setReaderStateStatus('SAVING', 'Saving')
        await this.$shelfViewState.savePosition(this.bookId, {
          locator: this.currentLocator,
          progression: this.adapter.getProgression(),
        })
        this.setReaderStateStatus('SAVED', 'Saved')
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', 'Save failed')
      }
    },
    async flushPreferenceSave() {
      if (this.preferenceSaveTimer !== undefined) {
        window.clearTimeout(this.preferenceSaveTimer)
        this.preferenceSaveTimer = undefined
      }
      if (!this.bookId || !this.manifest || this.manifest.kind !== 'FIXED_PAGE_SEQUENCE') return
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', 'Offline')
        return
      }

      try {
        this.setReaderStateStatus('SAVING', 'Saving')
        await this.$shelfViewState.savePreference({
          scopeType: 'BOOK',
          scopeId: this.bookId,
          format: SHELFVIEW_FIXED_PAGE_FORMAT,
          settings: this.currentPreferenceSettings(),
        })
        this.setReaderStateStatus('SAVED', 'Saved')
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', 'Save failed')
      }
    },
    clearReaderStateTimers() {
      if (this.positionSaveTimer !== undefined) {
        window.clearTimeout(this.positionSaveTimer)
        this.positionSaveTimer = undefined
      }
      if (this.preferenceSaveTimer !== undefined) {
        window.clearTimeout(this.preferenceSaveTimer)
        this.preferenceSaveTimer = undefined
      }
    },
    setReaderStateStatus(
      status: ShelfViewReaderStateStatus,
      message: string,
    ) {
      this.readerStateStatus = status
      this.readerStateMessage = message
    },
    browserOffline(): boolean {
      return typeof navigator !== 'undefined' && navigator.onLine === false
    },
    focusReaderStage() {
      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      readerStage?.focus()
    },
    closeShortcutHelp() {
      this.showShortcutHelp = false
      this.$nextTick(() => this.focusReaderStage())
    },
    closeBookmarksDrawer() {
      this.bookmarksDrawerOpen = false
      this.$nextTick(() => this.focusReaderStage())
    },
    async addBookmarkForCurrentPage() {
      if (!this.currentLocator) return
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', 'Offline')
        return
      }

      try {
        const bookmark = await this.$shelfViewState.createBookmark(this.bookId, {
          locator: this.currentLocator,
          label: `Page ${this.currentPageNumber}`,
        })
        this.bookmarks = [
          ...this.bookmarks.filter(existing => existing.id !== bookmark.id),
          bookmark,
        ]
        this.setReaderStateStatus('SAVED', 'Bookmark saved')
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', 'Bookmark failed')
      }
    },
    async goToBookmark(bookmark: ShelfViewBookmarkDto) {
      if (!this.adapter) return

      try {
        await this.adapter.goTo(bookmark.locator)
        this.syncAdapterState()
        this.schedulePositionSave()
        this.closeBookmarksDrawer()
      } catch (e) {
        this.setReaderStateStatus('ERROR', 'Bookmark unavailable')
      }
    },
    async deleteBookmark(bookmark: ShelfViewBookmarkDto) {
      if (this.browserOffline()) {
        this.setReaderStateStatus('OFFLINE', 'Offline')
        return
      }

      try {
        await this.$shelfViewState.deleteBookmark(bookmark.id)
        this.bookmarks = this.bookmarks.filter(existing => existing.id !== bookmark.id)
        this.setReaderStateStatus('SAVED', 'Bookmark deleted')
      } catch (e) {
        this.setReaderStateStatus(this.browserOffline() ? 'OFFLINE' : 'ERROR', 'Bookmark failed')
      }
    },
    bookmarkLabel(bookmark: ShelfViewBookmarkDto): string {
      if (bookmark.label) return bookmark.label
      if (bookmark.locator.kind === 'PAGE') return `Page ${bookmark.locator.page + 1}`
      return 'Bookmark'
    },
    onReaderOnline() {
      if (this.readerStateStatus === 'OFFLINE') {
        this.setReaderStateStatus('PENDING', 'Saving')
        void this.flushReaderState()
      }
    },
    onReaderOffline() {
      this.setReaderStateStatus('OFFLINE', 'Offline')
    },
    onVisibilityChange() {
      if (document.visibilityState === 'hidden') void this.flushReaderState()
    },
    onPageHide() {
      void this.flushReaderState()
    },
    async onReaderKeydown(e: KeyboardEvent) {
      if (e.ctrlKey || e.altKey || e.metaKey) return
      if (shelfViewShouldIgnoreShortcutTarget(e.target)) return

      const action = shelfViewReaderKeyboardAction(e.key, this.readingDirection)
      if (!action) return

      e.preventDefault()
      await this.runReaderInputAction(action)
    },
    onReaderTouchStart(e: TouchEvent) {
      const touch = e.changedTouches[0]
      if (!touch) return

      this.touchStart = {
        x: touch.clientX,
        y: touch.clientY,
      }
    },
    async onReaderTouchEnd(e: TouchEvent) {
      const touch = e.changedTouches[0]
      if (!this.touchStart || !touch) return

      const action = shelfViewTouchAction(
        this.touchStart,
        {x: touch.clientX, y: touch.clientY},
        this.readingDirection,
      )
      this.touchStart = undefined

      if (!action) return

      if (e.cancelable) e.preventDefault()
      await this.runReaderInputAction(action)
    },
    onReaderStageScroll() {
      if (this.readerLayout !== 'CONTINUOUS_SCROLL' || !this.adapter) return

      const pageIndex = this.nearestContinuousPageIndex()
      if (pageIndex === undefined || pageIndex === this.currentPageIndex) return

      void this.adapter.goTo({
        ...this.adapter.getCurrentLocator(),
        page: pageIndex,
      })
      this.syncAdapterState()
      this.schedulePositionSave()
    },
    nearestContinuousPageIndex(): number | undefined {
      const readerStage = this.$refs.readerStage as HTMLElement | undefined
      if (!readerStage) return undefined

      const pages = Array.from(readerStage.querySelectorAll<HTMLElement>('[data-page-index]'))
      if (pages.length === 0) return undefined

      const stageTop = readerStage.getBoundingClientRect().top
      let closestPageIndex: number | undefined
      let closestDistance = Number.POSITIVE_INFINITY

      pages.forEach(page => {
        const pageIndex = Number(page.dataset.pageIndex)
        if (!Number.isFinite(pageIndex)) return

        const distance = Math.abs(page.getBoundingClientRect().top - stageTop)
        if (distance < closestDistance) {
          closestDistance = distance
          closestPageIndex = pageIndex
        }
      })

      return closestPageIndex
    },
    async runReaderInputAction(action: ShelfViewReaderInputAction) {
      switch (action) {
        case 'NEXT':
          await this.nextPage()
          break
        case 'PREVIOUS':
          await this.previousPage()
          break
        case 'FIRST':
          await this.goToPageIndex(0)
          break
        case 'LAST':
          await this.goToPageIndex(this.pagesCount - 1)
          break
        case 'SHOW_SHORTCUTS':
          this.showShortcutHelp = true
          break
      }
    },
    async closeBook() {
      await this.flushReaderState()

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

.shelfview-reader-shell__sync-status {
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 6px;
  color: #f5f5f5;
  font-size: 12px;
  line-height: 1;
  margin-left: 8px;
  max-width: 120px;
  overflow: hidden;
  padding: 5px 7px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelfview-reader-shell__sync-status--error {
  border-color: #ef9a9a;
  color: #ffcdd2;
}

.shelfview-reader-shell__sync-status--saved {
  border-color: #a5d6a7;
  color: #c8e6c9;
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

.shelfview-reader-shell__stage:focus {
  outline: 2px solid #90caf9;
  outline-offset: -6px;
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

.shelfview-reader-shell__bookmarks-drawer {
  background: #202020;
  bottom: 0;
  box-shadow: -8px 0 24px rgba(0, 0, 0, 0.36);
  color: #f5f5f5;
  max-width: min(360px, 92vw);
  overflow-y: auto;
  padding: 16px;
  position: fixed;
  right: 0;
  top: 48px;
  width: 360px;
  z-index: 3;
}

.shelfview-reader-shell__bookmarks-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.shelfview-reader-shell__bookmarks-header h2 {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
}

.shelfview-reader-shell__bookmarks-empty {
  color: #d0d0d0;
  font-size: 14px;
  margin: 18px 0 0;
}

.shelfview-reader-shell__bookmarks-list {
  list-style: none;
  margin: 16px 0 0;
  padding: 0;
}

.shelfview-reader-shell__bookmark-row {
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  gap: 8px;
  min-height: 44px;
}

.shelfview-reader-shell__bookmark-link {
  background: transparent;
  border: 0;
  color: #f5f5f5;
  cursor: pointer;
  flex: 1;
  font: inherit;
  min-width: 0;
  overflow: hidden;
  padding: 10px 0;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shelfview-reader-shell__shortcut-dialog {
  align-items: center;
  background: rgba(0, 0, 0, 0.72);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 16px;
  position: fixed;
  z-index: 4;
}

.shelfview-reader-shell__shortcut-panel {
  background: #202020;
  border-radius: 8px;
  color: #f5f5f5;
  max-width: 420px;
  padding: 20px;
  width: 100%;
}

.shelfview-reader-shell__shortcut-panel h2 {
  font-size: 20px;
  font-weight: 500;
  margin: 0 0 16px;
}

.shelfview-reader-shell__shortcut-panel dl {
  display: grid;
  gap: 8px 16px;
  grid-template-columns: auto 1fr;
  margin: 0 0 18px;
}

.shelfview-reader-shell__shortcut-panel dt {
  color: #bbdefb;
  font-weight: 600;
}

.shelfview-reader-shell__shortcut-panel dd {
  margin: 0;
}

.shelfview-reader-shell__sr-only {
  height: 1px;
  left: -10000px;
  overflow: hidden;
  position: absolute;
  top: auto;
  width: 1px;
}

.full-width {
  width: 100%;
}

@media (prefers-reduced-motion: reduce) {
  .shelfview-reader-shell * {
    scroll-behavior: auto !important;
  }
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

  .shelfview-reader-shell__sync-status {
    max-width: 72px;
    padding: 5px;
  }
}
</style>
