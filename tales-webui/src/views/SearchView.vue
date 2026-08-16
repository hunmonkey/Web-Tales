<template>
  <div>
    <toolbar-sticky v-if="showToolbar">
      <v-toolbar-title>
        <span>{{ $t('search.search_results_for', {name: $route.query.q}) }}</span>
      </v-toolbar-title>
    </toolbar-sticky>

    <multi-select-bar
      v-model="selectedSeries"
      kind="series"
      @unselect-all="selectedSeries = []"
      @mark-read="markSelectedSeriesRead"
      @mark-unread="markSelectedSeriesUnread"
      @edit="editMultipleSeries"
      @delete="deleteSeries"
    />

    <multi-select-bar
      v-model="selectedBooks"
      kind="books"
      :oneshots="selectedOneshots"
      @unselect-all="selectedBooks = []"
      @mark-read="markSelectedBooksRead"
      @mark-unread="markSelectedBooksUnread"
      @edit="editMultipleBooks"
      @bulk-edit="bulkEditMultipleBooks"
      @delete="deleteBooks"
    />

    <v-container fluid>
      <empty-state
        v-if="emptyResults"
        :title="$t('search.no_results')"
        :sub-title="$t('search.search_for_something_else')"
        icon="mdi-magnify"
        icon-color="secondary"
        class="my-4"
      >
      </empty-state>

      <template v-else>
        <horizontal-scroller
          v-if="loaderSeries && loaderSeries.items.length !== 0"
          class="mb-4"
          :tick="loaderSeries.tick"
          @scroll-changed="(percent) => scrollChanged(loaderSeries, percent)"
        >
          <template v-slot:prepend>
            <div class="title">{{ $tc('common.series', 2) }}</div>
          </template>
          <template v-slot:content>
            <item-browser :items="loaderSeries.items"
                          nowrap
                          :edit-function="isAdmin ? singleEditSeries : undefined"
                          :selected.sync="selectedSeries"
                          :selectable="selectedBooks.length === 0"
                          :fixed-item-width="fixedCardWidth"
            />
          </template>
        </horizontal-scroller>

        <horizontal-scroller
          v-if="loaderBooks && loaderBooks.items.length !== 0"
          class="mb-4"
          :tick="loaderBooks.tick"
          @scroll-changed="(percent) => scrollChanged(loaderBooks, percent)"
        >
          <template v-slot:prepend>
            <div class="title">{{ $t('common.books') }}</div>
          </template>
          <template v-slot:content>
            <item-browser :items="loaderBooks.items"
                          :item-context="[ItemContext.SHOW_SERIES]"
                          nowrap
                          :edit-function="isAdmin ? singleEditBook : undefined"
                          :selected.sync="selectedBooks"
                          :selectable="selectedSeries.length === 0"
                          :fixed-item-width="fixedCardWidth"
            />
          </template>
        </horizontal-scroller>

      </template>
    </v-container>

  </div>
</template>

<script lang="ts">
import MultiSelectBar from '@/components/bars/MultiSelectBar.vue'
import ToolbarSticky from '@/components/bars/ToolbarSticky.vue'
import EmptyState from '@/components/EmptyState.vue'
import HorizontalScroller from '@/components/HorizontalScroller.vue'
import ItemBrowser from '@/components/ItemBrowser.vue'
import {BookDto} from '@/types/tales-books'
import {
  BOOK_CHANGED,
  BOOK_DELETED,
  LIBRARY_DELETED,
  READPROGRESS_CHANGED,
  READPROGRESS_DELETED,
  READPROGRESS_SERIES_CHANGED,
  READPROGRESS_SERIES_DELETED,
  SERIES_CHANGED,
  SERIES_DELETED,
} from '@/types/events'
import Vue from 'vue'
import {Oneshot, SeriesDto} from '@/types/tales-series'
import {
  BookSseDto,
  ReadProgressSeriesSseDto,
  ReadProgressSseDto,
  SeriesSseDto,
} from '@/types/tales-sse'
import {throttle} from 'lodash'
import {PageLoader} from '@/types/pageLoader'
import {ItemContext} from '@/types/items'
import {
  BookSearch, SearchConditionAnyOfBook, SearchConditionAnyOfSeries,
  SearchConditionOneShot,
  SearchConditionSeriesId, SearchOperatorIs,
  SearchOperatorIsFalse,
  SeriesSearch,
} from '@/types/tales-search'

export default Vue.extend({
  name: 'SearchView',
  components: {
    EmptyState,
    ToolbarSticky,
    HorizontalScroller,
    ItemBrowser,
    MultiSelectBar,
  },
  data: () => {
    return {
      ItemContext,
      loaderSeries: undefined as unknown as PageLoader<SeriesDto>,
      loaderBooks: undefined as unknown as PageLoader<BookDto>,
      pageSize: 20,
      loading: false,
      selectedSeries: [] as SeriesDto[],
      selectedBooks: [] as BookDto[],
    }
  },
  created() {
    this.$eventHub.$on(LIBRARY_DELETED, this.reloadResults)
    this.$eventHub.$on(SERIES_CHANGED, this.seriesChanged)
    this.$eventHub.$on(SERIES_DELETED, this.seriesChanged)
    this.$eventHub.$on(BOOK_CHANGED, this.bookChanged)
    this.$eventHub.$on(BOOK_DELETED, this.bookChanged)
    this.$eventHub.$on(READPROGRESS_CHANGED, this.readProgressChanged)
    this.$eventHub.$on(READPROGRESS_DELETED, this.readProgressChanged)
    this.$eventHub.$on(READPROGRESS_SERIES_CHANGED, this.readProgressSeriesChanged)
    this.$eventHub.$on(READPROGRESS_SERIES_DELETED, this.readProgressSeriesChanged)
  },
  beforeDestroy() {
    this.$eventHub.$off(LIBRARY_DELETED, this.reloadResults)
    this.$eventHub.$off(SERIES_CHANGED, this.seriesChanged)
    this.$eventHub.$off(SERIES_DELETED, this.seriesChanged)
    this.$eventHub.$off(BOOK_CHANGED, this.bookChanged)
    this.$eventHub.$off(BOOK_DELETED, this.bookChanged)
    this.$eventHub.$off(READPROGRESS_CHANGED, this.readProgressChanged)
    this.$eventHub.$off(READPROGRESS_DELETED, this.readProgressChanged)
    this.$eventHub.$off(READPROGRESS_SERIES_CHANGED, this.readProgressSeriesChanged)
    this.$eventHub.$off(READPROGRESS_SERIES_DELETED, this.readProgressSeriesChanged)
  },
  watch: {
    '$route.query.q': {
      handler: function (val) {
        this.setupLoaders(val)
        this.loadResults(val)
        this.selectedBooks = []
        this.selectedSeries = []
      },
      deep: true,
      immediate: true,
    },
  },
  computed: {
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
    },
    fixedCardWidth(): number {
      return this.$vuetify.breakpoint.xs ? 120 : 150
    },
    showToolbar(): boolean {
      return this.selectedSeries.length === 0 && this.selectedBooks.length === 0
    },
    emptyResults(): boolean {
      return !this.loading &&
        this.loaderSeries?.items.length === 0 &&
        this.loaderBooks?.items.length === 0
    },
    selectedOneshots(): boolean {
      return this.selectedBooks.every(b => b.oneshot)
    },
  },
  methods: {
    async scrollChanged(loader: PageLoader<any>, percent: number) {
      if (percent > 0.95) await loader.loadNext()
    },
    seriesChanged(event: SeriesSseDto) {
      if (this.loaderSeries?.items.some(x => x.id === event.seriesId)) {
        this.reloadResults()
      }
    },
    bookChanged(event: BookSseDto) {
      if (this.loaderBooks?.items.some(x => x.id === event.bookId)) {
        this.reloadResults()
      }
    },
    readProgressChanged(event: ReadProgressSseDto) {
      if (this.loaderBooks?.items.some(x => x.id === event.bookId)) {
        this.reloadResults()
      }
    },
    readProgressSeriesChanged(event: ReadProgressSeriesSseDto) {
      if (this.loaderSeries?.items.some(x => x.id === event.seriesId)) {
        this.reloadResults()
      }
    },
    async singleEditSeries(series: SeriesDto) {
      if (series.oneshot) {
        const book = (await this.$talesBooks.getBooksList({
          condition: new SearchConditionSeriesId(new SearchOperatorIs(series.id)),
        } as BookSearch)).content[0]
        this.$store.dispatch('dialogUpdateOneshots', {series: series, book: book})
      } else
        this.$store.dispatch('dialogUpdateSeries', series)
    },
    async singleEditBook(book: BookDto) {
      if (book.oneshot) {
        const series = (await this.$talesSeries.getOneSeries(book.seriesId))
        this.$store.dispatch('dialogUpdateOneshots', {series: series, book: book})
      } else
        this.$store.dispatch('dialogUpdateBooks', book)
    },
    async markSelectedSeriesRead() {
      await Promise.all(this.selectedSeries.map(s =>
        this.$talesSeries.markAsRead(s.id),
      ))
      this.selectedSeries = await Promise.all(this.selectedSeries.map(s =>
        this.$talesSeries.getOneSeries(s.id),
      ))
    },
    async markSelectedSeriesUnread() {
      await Promise.all(this.selectedSeries.map(s =>
        this.$talesSeries.markAsUnread(s.id),
      ))
      this.selectedSeries = await Promise.all(this.selectedSeries.map(s =>
        this.$talesSeries.getOneSeries(s.id),
      ))
    },
    async editMultipleSeries() {
      if (this.selectedSeries.every(s => s.oneshot)) {
        const books = await Promise.all(this.selectedSeries.map(s => this.$talesBooks.getBooksList({
          condition: new SearchConditionSeriesId(new SearchOperatorIs(s.id)),
        } as BookSearch)))
        const oneshots = this.selectedSeries.map((s, index) => ({series: s, book: books[index].content[0]} as Oneshot))
        this.$store.dispatch('dialogUpdateOneshots', oneshots)
      } else
        this.$store.dispatch('dialogUpdateSeries', this.selectedSeries)
    },
    async editMultipleBooks() {
      if (this.selectedBooks.every(b => b.oneshot)) {
        const series = await Promise.all(this.selectedBooks.map(b => this.$talesSeries.getOneSeries(b.seriesId)))
        const oneshots = this.selectedBooks.map((b, index) => ({series: series[index], book: b} as Oneshot))
        this.$store.dispatch('dialogUpdateOneshots', oneshots)
      } else
        this.$store.dispatch('dialogUpdateBooks', this.selectedBooks)
    },
    bulkEditMultipleBooks() {
      this.$store.dispatch('dialogUpdateBulkBooks', this.selectedBooks)
    },
    deleteSeries() {
      this.$store.dispatch('dialogDeleteSeries', this.selectedSeries)
    },
    deleteBooks() {
      this.$store.dispatch('dialogDeleteBook', this.selectedBooks)
    },
    async markSelectedBooksRead() {
      await Promise.all(this.selectedBooks.map(b =>
        this.$talesBooks.updateReadProgress(b.id, {completed: true}),
      ))
    },
    async markSelectedBooksUnread() {
      await Promise.all(this.selectedBooks.map(b =>
        this.$talesBooks.deleteReadProgress(b.id),
      ))
    },
    reloadResults: throttle(function (this: any) {
      this.loadResults(this.$route.query.q.toString(), true)
    }, 500),
    setupLoaders(search: string) {
      if (search) {
        this.loaderSeries = new PageLoader<SeriesDto>({size: this.pageSize}, (pageable: PageRequest) => this.$talesSeries.getSeriesList({
          fullTextSearch: search,
          condition: new SearchConditionOneShot(new SearchOperatorIsFalse()),
        } as SeriesSearch, pageable))
        this.loaderBooks = new PageLoader<BookDto>({size: this.pageSize}, (pageable: PageRequest) => this.$talesBooks.getBooksList({
          fullTextSearch: search,
        } as BookSearch, pageable))
      } else {
        this.loaderSeries = null as unknown as PageLoader<SeriesDto>
        this.loaderBooks = null as unknown as PageLoader<BookDto>
      }
    },
    async loadResults(search: string, reload: boolean = false) {
      this.selectedBooks = []
      this.selectedSeries = []

      if (search) {
        this.loading = true
        if (reload) {
          Promise.all([
            this.loaderSeries.reload(),
            this.loaderBooks.reload(),
          ]).then(() => {
            this.loading = false
          })
        } else {
          Promise.all([
            this.loaderSeries.loadNext(),
            this.loaderBooks.loadNext(),
          ]).then(() => {
            this.loading = false
          })
        }
      }
    },
  },
})
</script>
<style scoped>
</style>
