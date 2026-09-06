<template>
  <div :style="$vuetify.breakpoint.xs ? 'margin-bottom: 56px' : undefined">
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

    <confirmation-dialog
      v-model="removeResumeDialog"
      :title="$t('dialog.delete_read_progress.dialog_title')"
      :body="removeResumeBody"
      :button-confirm="$t('dialog.delete_read_progress.button_confirm')"
      button-confirm-color="error"
      @confirm="removeResume"
    />

    <v-container fluid class="dashboard">
      <header class="dashboard__heading">
        <div>
          <div class="dashboard__eyebrow">Tales</div>
          <h1 class="dashboard__title">{{ $t('dashboard.keep_reading') }}</h1>
        </div>
        <!-- The heading now names the resume grid, so the bulk controls belong
             next to it rather than above the cards (SV-1297). -->
        <div v-if="resumeBooks.length > 0" class="resume-actions">
          <v-checkbox
            :input-value="allResumeSelected"
            :indeterminate="someResumeSelected"
            :label="$t('dashboard.resume_select_all')"
            class="ma-0 pa-0"
            color="secondary"
            dense
            hide-details
            data-testid="dashboard-resume-select-all"
            @change="toggleAllResume"
          />
          <v-btn
            text
            small
            :disabled="selectedResume.length === 0"
            data-testid="dashboard-resume-remove-selected"
            @click="confirmRemoveResume(selectedResumeBooks)"
          >{{ $t('dashboard.resume_remove_selected', {count: selectedResume.length}) }}
          </v-btn>
          <v-btn
            text
            small
            data-testid="dashboard-resume-remove-all"
            @click="confirmRemoveResume(resumeBooks)"
          >{{ $t('dashboard.resume_remove_all') }}
          </v-btn>
        </div>
      </header>

      <!-- Puts the books that are actually in progress on the first screen, so
           coming back to Tales does not start with scrolling. A wrapping grid of
           compact cards laid straight on the page, rather than the boxed
           horizontal row it replaces (SV-1289): the row hid everything past the
           first screenful behind a sideways scroll. Cover opens the reader,
           title opens the book, and the cross drops the book out of the grid. -->
      <section
        v-if="resumeBooks.length > 0"
        class="mb-8"
        data-testid="dashboard-resume"
      >
        <div class="resume-grid">
          <article
            v-for="book in resumeBooks"
            :key="book.id"
            class="resume-card mx-2 my-2"
            :style="{width: resumeCardWidth + 'px'}"
            data-testid="dashboard-resume-card"
          >
            <div class="resume-card__cover">
              <router-link
                :to="resumeReadTo(book)"
                class="link-none"
                :aria-label="$t('dashboard.resume_reading')"
              >
                <img :src="resumeThumbnailUrl(book)" alt=""/>
              </router-link>
              <v-progress-linear
                :value="resumePercentage(book)"
                color="secondary"
                height="4"
                class="resume-card__progress"
              />
              <v-checkbox
                :input-value="selectedResume.includes(book.id)"
                :aria-label="$t('dashboard.resume_select')"
                class="resume-card__select ma-0 pa-0"
                color="secondary"
                dense
                hide-details
                :ripple="false"
                data-testid="dashboard-resume-select"
                @change="setResumeSelection(book.id, $event)"
              />
              <v-btn
                icon
                x-small
                class="resume-card__remove"
                :aria-label="$t('dashboard.resume_remove')"
                :title="$t('dashboard.resume_remove')"
                data-testid="dashboard-resume-remove"
                @click="confirmRemoveResume([book])"
              >
                <v-icon small>mdi-close</v-icon>
              </v-btn>
            </div>
            <router-link
              :to="resumeDetailTo(book)"
              class="resume-card__title link-none"
              :title="resumeTitle(book)"
              data-testid="dashboard-resume-title"
            >{{ resumeTitle(book) }}</router-link>
            <p class="resume-card__meta">{{ resumeProgressLabel(book) }}</p>
          </article>
        </div>
      </section>

      <empty-state v-if="allEmpty && !loading"
                   :title="$t('common.nothing_to_show')"
                   icon="mdi-help-circle-outline"
                   icon-color="secondary"
      >
      </empty-state>

      <template v-for="(section, i) in sections">
        <!-- The section titles come from the saved view config, so the shelves
             can be drawn in their final positions while their pages load. -->
        <div
          v-if="loading && (!section.loader || section.loader.items.length === 0)"
          :key="`skeleton-${i}`"
          class="dashboard-section mb-8"
          data-testid="dashboard-section-skeleton"
        >
          <div class="dashboard-section__title">{{ $t(`dashboard.${section.value.toLowerCase()}`) }}</div>
          <div class="dashboard-section__skeleton-row">
            <item-card-skeleton
              v-for="n in 8"
              :key="`skeleton-${i}-${n}`"
              :width="fixedCardWidth"
              class="my-2 mx-2"
            />
          </div>
        </div>

        <horizontal-scroller
          v-bind:key="i"
          v-else-if="section.loader && section.loader.items.length !== 0"
          class="dashboard-section mb-8"
          :tick="section.loader.tick"
          @scroll-changed="(percent) => scrollChanged(section.loader, percent)"
        >
          <template v-slot:prepend>
            <div class="dashboard-section__title">{{ $t(`dashboard.${section.value.toLowerCase()}`) }}</div>
          </template>
          <template v-slot:content>
            <item-browser v-if="section.type ===SectionType.BOOK"
                          :items="section.loader.items"
                          :item-context="section.itemContext"
                          nowrap
                          :edit-function="isAdmin ? singleEditBook : undefined"
                          :selected.sync="selectedBooks"
                          :selectable="selectedSeries.length === 0"
                          :fixed-item-width="fixedCardWidth"
            />
            <item-browser v-if="section.type === SectionType.SERIES"
                          :items="section.loader.items"
                          :item-context="section.itemContext"
                          nowrap
                          :edit-function="isAdmin ? singleEditSeries : undefined"
                          :selected.sync="selectedSeries"
                          :selectable="selectedBooks.length === 0"
                          :fixed-item-width="fixedCardWidth"
            />
          </template>
        </horizontal-scroller>
      </template>

      <v-fab-transition>
        <v-btn
          rounded
          color="primary"
          bottom
          right
          fixed
          class="dashboard__edit-view"
          @click="modalEditRecommended = true"
        >
          <v-icon left>mdi-tune-variant</v-icon>
          {{ $t('dialog.edit_recommended.dialog_title') }}
        </v-btn>
      </v-fab-transition>

      <edit-recommended-dialog
        v-model="modalEditRecommended"
        :view-config.sync="viewConfig"
        @reset-defaults="resetDefaultView"
      />
    </v-container>
  </div>
</template>

<script lang="ts">
import MultiSelectBar from '@/components/bars/MultiSelectBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import HorizontalScroller from '@/components/HorizontalScroller.vue'
import ItemBrowser from '@/components/ItemBrowser.vue'
import ItemCardSkeleton from '@/components/ItemCardSkeleton.vue'
import {ReadStatus} from '@/types/enum-books'
import {BookDto} from '@/types/tales-books'
import {
  BOOK_ADDED,
  BOOK_CHANGED,
  BOOK_DELETED,
  READPROGRESS_CHANGED,
  READPROGRESS_DELETED,
  READPROGRESS_SERIES_CHANGED,
  READPROGRESS_SERIES_DELETED,
  SERIES_ADDED,
  SERIES_CHANGED,
  SERIES_DELETED,
  THUMBNAILBOOK_ADDED,
  THUMBNAILBOOK_DELETED,
} from '@/types/events'
import Vue from 'vue'
import {Oneshot, SeriesDto} from '@/types/tales-series'
import {LIBRARIES_ALL} from '@/types/library'
import {random, throttle} from 'lodash'
import {subMonths} from 'date-fns'
import {
  BookSseDto,
  ReadProgressSeriesSseDto,
  ReadProgressSseDto,
  SeriesSseDto,
  ThumbnailBookSseDto,
} from '@/types/tales-sse'
import {LibraryDto} from '@/types/tales-libraries'
import {PageLoader} from '@/types/pageLoader'
import {ItemContext} from '@/types/items'
import {RawLocation} from 'vue-router'
import {bookThumbnailUrl} from '@/functions/urls'
import {getReadProgressPercentage} from '@/functions/book-progress'
import {getBookReadRouteFromMedia} from '@/functions/book-format'
import {
  BookSearch,
  SearchConditionAllOfBook,
  SearchConditionAnyOfBook,
  SearchConditionAnyOfSeries,
  SearchConditionBook,
  SearchConditionLibraryId,
  SearchConditionReadStatus,
  SearchConditionReleaseDate,
  SearchConditionSeriesId,
  SearchOperatorAfter,
  SearchOperatorIs,
} from '@/types/tales-search'
import {
  CLIENT_SETTING,
  ClientSettingsRecommendedView,
  ClientSettingsRecommendedViewSection,
  ClientSettingUserUpdateDto,
  RECOMMENDED_DEFAULT,
  RecommendedViewSection,
} from '@/types/tales-clientsettings'
import EditRecommendedDialog from '@/components/dialogs/EditRecommendedDialog.vue'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'

interface SectionConfig {
  loader: PageLoader<any> | undefined,
  type: SectionType,
  value: RecommendedViewSection,
  itemContext?: ItemContext[] | undefined,
}

enum SectionType {
  SERIES,
  BOOK,
}

export default Vue.extend({
  name: 'DashboardView',
  components: {
    ConfirmationDialog,
    EditRecommendedDialog,
    HorizontalScroller,
    ItemCardSkeleton,
    EmptyState,
    ItemBrowser,
    MultiSelectBar,
  },
  data: () => {
    return {
      ItemContext,
      SectionType,
      loading: false,
      modalEditRecommended: false,
      loaderRecentlyAddedSeries: undefined as PageLoader<SeriesDto> | undefined,
      loaderRecentlyUpdatedSeries: undefined as PageLoader<SeriesDto> | undefined,
      loaderRecentlyAddedBooks: undefined as PageLoader<BookDto> | undefined,
      loaderKeepReadingBooks: undefined as PageLoader<BookDto> | undefined,
      loaderOnDeckBooks: undefined as PageLoader<BookDto> | undefined,
      loaderRecentlyReleasedBooks: undefined as PageLoader<BookDto> | undefined,
      loaderRecentlyReadBooks: undefined as PageLoader<BookDto> | undefined,
      selectedSeries: [] as SeriesDto[],
      selectedBooks: [] as BookDto[],
      // Keep Reading posters are cached for a day like every other image, so a
      // replaced cover only reaches them through a new URL. ItemCard does the
      // same for the cards. Keyed by book id: the grid holds several covers.
      resumeThumbnailCacheBust: {} as Record<string, string>,
      removeResumeDialog: false,
      // Books queued for the confirmation dialog: one card's cross, the current
      // selection, or the whole grid all end up here.
      resumeToRemove: [] as BookDto[],
      // Ids rather than DTOs, so a reload that replaces the objects keeps the
      // selection.
      selectedResume: [] as string[],
    }
  },
  created() {
    this.$eventHub.$on(SERIES_ADDED, this.seriesChanged)
    this.$eventHub.$on(SERIES_CHANGED, this.seriesChanged)
    this.$eventHub.$on(SERIES_DELETED, this.seriesChanged)
    this.$eventHub.$on(BOOK_ADDED, this.bookChanged)
    this.$eventHub.$on(BOOK_CHANGED, this.bookChanged)
    this.$eventHub.$on(BOOK_DELETED, this.bookChanged)
    this.$eventHub.$on(READPROGRESS_CHANGED, this.readProgressChanged)
    this.$eventHub.$on(READPROGRESS_DELETED, this.readProgressChanged)
    this.$eventHub.$on(READPROGRESS_SERIES_CHANGED, this.readProgressSeriesChanged)
    this.$eventHub.$on(READPROGRESS_SERIES_DELETED, this.readProgressSeriesChanged)
    this.$eventHub.$on(THUMBNAILBOOK_ADDED, this.thumbnailBookChanged)
    this.$eventHub.$on(THUMBNAILBOOK_DELETED, this.thumbnailBookChanged)
  },
  beforeDestroy() {
    this.$eventHub.$off(SERIES_ADDED, this.seriesChanged)
    this.$eventHub.$off(SERIES_CHANGED, this.seriesChanged)
    this.$eventHub.$off(SERIES_DELETED, this.seriesChanged)
    this.$eventHub.$off(BOOK_ADDED, this.bookChanged)
    this.$eventHub.$off(BOOK_CHANGED, this.bookChanged)
    this.$eventHub.$off(BOOK_DELETED, this.bookChanged)
    this.$eventHub.$off(READPROGRESS_CHANGED, this.readProgressChanged)
    this.$eventHub.$off(READPROGRESS_DELETED, this.readProgressChanged)
    this.$eventHub.$off(READPROGRESS_SERIES_CHANGED, this.readProgressSeriesChanged)
    this.$eventHub.$off(READPROGRESS_SERIES_DELETED, this.readProgressSeriesChanged)
    this.$eventHub.$off(THUMBNAILBOOK_ADDED, this.thumbnailBookChanged)
    this.$eventHub.$off(THUMBNAILBOOK_DELETED, this.thumbnailBookChanged)
  },
  mounted() {
    this.setupLoaders(this.libraryId)
    this.loadAll()
  },
  props: {
    libraryId: {
      type: String,
      default: LIBRARIES_ALL,
    },
  },
  watch: {
    libraryIds() {
      this.setupLoaders(this.libraryId)
      this.loadAll()
    },
    '$store.state.talesLibraries.libraries': {
      handler(val) {
        if (val.length === 0) this.$router.push({name: 'welcome'})
        else this.reload()
      },
    },
    '$store.getters.getLibrariesPinned': {
      handler(val) {
        if (val.length === 0) this.$router.push({name: 'no-pins'})
      },
    },
  },
  computed: {
    settingsKey(): string {
      return `${CLIENT_SETTING.WEBUI_RECOMMENDED}.${this.libraryId}`.toLowerCase()
    },
    viewConfig: {
      get: function (): ClientSettingsRecommendedView {
        try {
          return JSON.parse(this.$store.getters.getClientSettings[this.settingsKey].value) as ClientSettingsRecommendedView
        } catch (_) {
        }
        return RECOMMENDED_DEFAULT
      },
      set: async function (view: ClientSettingsRecommendedView) {
        let newSettings = {} as Record<string, ClientSettingUserUpdateDto>
        newSettings[this.settingsKey] = {
          value: JSON.stringify(view),
        }
        await this.$talesSettings.updateClientSettingUser(newSettings)
        await this.$store.dispatch('getClientSettingsUser')
        this.setupLoaders(this.libraryId)
        this.loadAll(true)
      },
    },
    sections(): SectionConfig[] {
      const sections = [] as SectionConfig[]
      this.viewConfig.sections.forEach((it: ClientSettingsRecommendedViewSection) => {
        const config = this.getSectionConfig(it.section)
        if (config != undefined) sections.push(config)
      })
      return sections
    },
    library(): LibraryDto | undefined {
      return this.getLibraryLazy(this.libraryId)
    },
    libraryIds(): string[] {
      return this.libraryId !== LIBRARIES_ALL ? [this.libraryId] : this.$store.getters.getLibrariesPinned.map((it: LibraryDto) => it.id)
    },
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
    },
    fixedCardWidth(): number {
      if (this.$vuetify.breakpoint.xs) return 128
      if (this.$vuetify.breakpoint.sm) return 150
      if (this.$vuetify.breakpoint.md) return 166
      return 180
    },
    allEmpty(): boolean {
      return (this.loaderRecentlyAddedSeries == undefined || this.loaderRecentlyAddedSeries?.items.length === 0) &&
        (this.loaderRecentlyUpdatedSeries == undefined || this.loaderRecentlyUpdatedSeries?.items.length === 0) &&
        (this.loaderRecentlyAddedBooks == undefined || this.loaderRecentlyAddedBooks?.items.length === 0) &&
        (this.loaderKeepReadingBooks == undefined || this.loaderKeepReadingBooks?.items.length === 0) &&
        (this.loaderOnDeckBooks == undefined || this.loaderOnDeckBooks?.items.length === 0) &&
        (this.loaderRecentlyReleasedBooks == undefined || this.loaderRecentlyReleasedBooks?.items.length === 0) &&
        (this.loaderRecentlyReadBooks == undefined || this.loaderRecentlyReadBooks?.items.length === 0)
    },
    // Every book still in progress, most recently read first. Empty until the
    // loader has returned something, when the Keep Reading section is switched
    // off, or when nothing is in progress, in which case the dashboard simply
    // starts with its shelves as before.
    //
    // Deliberately not gated on `loading`. A reload keeps the previous items
    // until the new page replaces them, so gating here would unmount the grid
    // on every read-progress event and shift the page up and back down - the
    // exact movement SV-1060 exists to remove.
    resumeBooks(): BookDto[] {
      return this.loaderKeepReadingBooks?.items ?? []
    },
    // Deliberately smaller than `fixedCardWidth`: this grid is a reminder of
    // what is open, not a shelf to browse.
    resumeCardWidth(): number {
      if (this.$vuetify.breakpoint.xs) return 96
      if (this.$vuetify.breakpoint.sm) return 104
      if (this.$vuetify.breakpoint.md) return 112
      return 120
    },
    // Filtered against the grid rather than trusted as-is: a reload can drop a
    // book that is still in `selectedResume`.
    selectedResumeBooks(): BookDto[] {
      return this.resumeBooks.filter(b => this.selectedResume.includes(b.id))
    },
    allResumeSelected(): boolean {
      return this.resumeBooks.length > 0 && this.selectedResumeBooks.length === this.resumeBooks.length
    },
    someResumeSelected(): boolean {
      return this.selectedResumeBooks.length > 0 && !this.allResumeSelected
    },
    removeResumeBody(): string {
      if (this.resumeToRemove.length === 0) return ''
      if (this.resumeToRemove.length === 1)
        return this.$t('dialog.delete_read_progress.warning', {
          name: this.resumeTitle(this.resumeToRemove[0]),
        }).toString()
      return this.$t('dialog.delete_read_progress.warning_multiple', {
        count: this.resumeToRemove.length,
      }).toString()
    },
    selectedOneshots(): boolean {
      return this.selectedBooks.every(b => b.oneshot)
    },
  },
  methods: {
    async resetDefaultView() {
      await this.$talesSettings.deleteClientSettingUser([this.settingsKey])
      await this.$store.dispatch('getClientSettingsUser')
      this.setupLoaders(this.libraryId)
      this.loadAll(true)
    },
    hasSection(section: RecommendedViewSection): boolean {
      return this.viewConfig.sections.some(it => it.section === section)
    },
    getSectionConfig(section: RecommendedViewSection): SectionConfig | undefined {
      switch (section) {
        case RecommendedViewSection.KEEP_READING:
          // Drawn as the resume banner above the shelves. A shelf here would
          // repeat the same books under the same title, so the section is
          // enabled only to decide whether the banner is loaded at all.
          return undefined
        case RecommendedViewSection.ON_DECK:
          return {
            loader: this.loaderOnDeckBooks,
            type: SectionType.BOOK,
            value: section,
            itemContext: [ItemContext.SHOW_SERIES],
          }
        case RecommendedViewSection.RECENTLY_RELEASED_BOOKS:
          return {
            loader: this.loaderRecentlyReleasedBooks,
            type: SectionType.BOOK,
            value: section,
            itemContext: [ItemContext.RELEASE_DATE, ItemContext.SHOW_SERIES],
          }
        case RecommendedViewSection.RECENTLY_ADDED_BOOKS:
          return {
            loader: this.loaderRecentlyAddedBooks,
            type: SectionType.BOOK,
            value: section,
            itemContext: [ItemContext.SHOW_SERIES],
          }
        case RecommendedViewSection.RECENTLY_ADDED_SERIES:
          return {
            loader: this.loaderRecentlyAddedSeries,
            type: SectionType.SERIES,
            value: section,
          }
        case RecommendedViewSection.RECENTLY_UPDATED_SERIES:
          return {
            loader: this.loaderRecentlyUpdatedSeries,
            type: SectionType.SERIES,
            value: section,
          }
        case RecommendedViewSection.RECENTLY_READ_BOOKS:
          return {
            loader: this.loaderRecentlyReadBooks,
            type: SectionType.BOOK,
            value: section,
            itemContext: [ItemContext.SHOW_SERIES],
          }
        default:
          return undefined
      }
    },
    async scrollChanged(loader: PageLoader<any>, percent: number) {
      if (percent > 0.95) await loader.loadNext()
    },
    getRequestLibraryId(libraryId: string): string[] {
      return libraryId !== LIBRARIES_ALL ? [libraryId] : this.$store.getters.getLibrariesPinned.map((it: LibraryDto) => it.id)
    },
    seriesChanged(event: SeriesSseDto) {
      if (this.libraryId === LIBRARIES_ALL || event.libraryId === this.libraryId) {
        this.reload()
      }
    },
    bookChanged(event: BookSseDto) {
      if (this.libraryId === LIBRARIES_ALL || event.libraryId === this.libraryId) {
        this.reload()
      }
    },
    readProgressChanged(event: ReadProgressSseDto) {
      if (this.loaderKeepReadingBooks?.items.some(b => b.id === event.bookId)) this.reload()
      else if (this.loaderRecentlyAddedBooks?.items.some(b => b.id === event.bookId)) this.reload()
      else if (this.loaderOnDeckBooks?.items.some(b => b.id === event.bookId)) this.reload()
      else if (this.loaderRecentlyReleasedBooks?.items.some(b => b.id === event.bookId)) this.reload()
      else if (this.loaderRecentlyReadBooks?.items.some(b => b.id === event.bookId)) this.reload()
    },
    readProgressSeriesChanged(event: ReadProgressSeriesSseDto) {
      if (this.loaderRecentlyUpdatedSeries?.items.some(s => s.id === event.seriesId)) this.reload()
      else if (this.loaderRecentlyAddedSeries?.items.some(s => s.id === event.seriesId)) this.reload()
    },
    // Only the Keep Reading grid needs this. The cards below it are ItemCard
    // instances, which already bust their own poster cache on the same event.
    thumbnailBookChanged(event: ThumbnailBookSseDto) {
      if (event.selected && this.resumeBooks.some(b => b.id === event.bookId)) {
        this.$set(this.resumeThumbnailCacheBust, event.bookId, '?' + random(1000))
      }
    },
    resumeTitle(book: BookDto): string {
      const series = book.seriesTitle
      return series && !book.oneshot ? `${series} · ${book.metadata.title}` : book.metadata.title
    },
    resumeThumbnailUrl(book: BookDto): string {
      return bookThumbnailUrl(book.id) + (this.resumeThumbnailCacheBust[book.id] ?? '')
    },
    resumePercentage(book: BookDto): number {
      return getReadProgressPercentage(book)
    },
    resumeProgressLabel(book: BookDto): string {
      return this.$t('dashboard.resume_progress', {
        page: book.readProgress?.page ?? 1,
        total: book.media.pagesCount,
      }).toString()
    },
    resumeDetailTo(book: BookDto): RawLocation {
      return book.oneshot
        ? {name: 'browse-oneshot', params: {seriesId: book.seriesId}}
        : {name: 'browse-book', params: {bookId: book.id}}
    },
    resumeReadTo(book: BookDto): RawLocation {
      return {
        name: getBookReadRouteFromMedia(book.media),
        params: {bookId: book.id},
      }
    },
    setResumeSelection(bookId: string, selected: boolean) {
      const rest = this.selectedResume.filter((id: string) => id !== bookId)
      this.selectedResume = selected ? [...rest, bookId] : rest
    },
    toggleAllResume(selected: boolean) {
      this.selectedResume = selected ? this.resumeBooks.map(b => b.id) : []
    },
    confirmRemoveResume(books: BookDto[]) {
      if (books.length === 0) return
      this.resumeToRemove = books
      this.removeResumeDialog = true
    },
    // Deleting the read progress is what takes a book out of Keep Reading;
    // the server has no separate "hide" state. The books go back to unread and
    // start from page one when opened again - hence the confirmation.
    async removeResume() {
      const books = this.resumeToRemove
      if (books.length === 0) return
      this.resumeToRemove = []
      // One failed request must not skip the reload: the others already went
      // through and the grid would otherwise keep showing them (SV-1302).
      await Promise.all(books.map(b => this.$talesBooks.deleteReadProgress(b.id).catch(() => undefined)))
      this.selectedResume = []
      // The SSE event that follows shares the 5s reload throttle; the grid the
      // user just acted on should not wait for it.
      await this.loaderKeepReadingBooks?.reload()
    },
    reload: throttle(function (this: any) {
      this.loadAll(true)
    }, 5000),
    setupLoaders(libraryId: string) {
      const requestLibraries = this.getRequestLibraryId(libraryId)
      const baseBookConditions = [] as SearchConditionBook[]
      if (requestLibraries)
        baseBookConditions.push(new SearchConditionAnyOfSeries(
          requestLibraries.map((it: string) => new SearchConditionLibraryId(new SearchOperatorIs(it))),
        ))

      this.loaderKeepReadingBooks = this.hasSection(RecommendedViewSection.KEEP_READING) ? new PageLoader<BookDto>(
        {sort: ['readProgress.readDate,desc']},
        (pageable: PageRequest) => this.$talesBooks.getBooksList({
          condition: new SearchConditionAllOfBook([...baseBookConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.IN_PROGRESS))]),
        } as BookSearch, pageable),
      ) : undefined
      this.loaderOnDeckBooks = this.hasSection(RecommendedViewSection.ON_DECK) ? new PageLoader<BookDto>(
        {},
        (pageable: PageRequest) => this.$talesBooks.getBooksOnDeck(requestLibraries, pageable),
      ) : undefined
      this.loaderRecentlyAddedBooks = this.hasSection(RecommendedViewSection.RECENTLY_ADDED_BOOKS) ? new PageLoader<BookDto>(
        {sort: ['createdDate,desc']},
        (pageable: PageRequest) => this.$talesBooks.getBooksList({
          condition: new SearchConditionAllOfBook(baseBookConditions),
        } as BookSearch, pageable),
      ) : undefined
      this.loaderRecentlyReleasedBooks = this.hasSection(RecommendedViewSection.RECENTLY_RELEASED_BOOKS) ? new PageLoader<BookDto>(
        {sort: ['metadata.releaseDate,desc']},
        (pageable: PageRequest) => this.$talesBooks.getBooksList({
          condition: new SearchConditionAllOfBook([...baseBookConditions, new SearchConditionReleaseDate(new SearchOperatorAfter(subMonths(new Date(), 1)))]),
        } as BookSearch, pageable),
      ) : undefined
      this.loaderRecentlyReadBooks = this.hasSection(RecommendedViewSection.RECENTLY_READ_BOOKS) ? new PageLoader<BookDto>(
        {sort: ['readProgress.readDate,desc']},
        (pageable: PageRequest) => this.$talesBooks.getBooksList({
          condition: new SearchConditionAllOfBook([...baseBookConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.READ))]),
        } as BookSearch, pageable),
      ) : undefined

      this.loaderRecentlyAddedSeries = this.hasSection(RecommendedViewSection.RECENTLY_ADDED_SERIES) ? new PageLoader<SeriesDto>(
        {},
        (pageable: PageRequest) => this.$talesSeries.getNewSeries(requestLibraries, false, pageable),
      ) : undefined
      this.loaderRecentlyUpdatedSeries = this.hasSection(RecommendedViewSection.RECENTLY_UPDATED_SERIES) ? new PageLoader<SeriesDto>(
        {},
        (pageable: PageRequest) => this.$talesSeries.getUpdatedSeries(requestLibraries, false, pageable),
      ) : undefined
    },
    loadAll(reload: boolean = false) {
      this.loading = true
      if (this.library != undefined) document.title = `Tales - ${this.library.name}`
      this.selectedSeries = []
      this.selectedBooks = []
      this.selectedResume = []

      const loaders = [
        this.loaderKeepReadingBooks,
        this.loaderOnDeckBooks,
        this.loaderRecentlyReleasedBooks,
        this.loaderRecentlyAddedBooks,
        this.loaderRecentlyAddedSeries,
        this.loaderRecentlyUpdatedSeries,
        this.loaderRecentlyReadBooks,
      ]

      // Each loader is settled on its own rather than through a shared
      // Promise.all rejection. A single failing section must not leave `loading`
      // true, because that pins the skeletons on screen, hides the resume banner
      // and suppresses the empty state, with no way back other than a reload.
      // The sections that did load are shown; the ones that failed stay empty.
      Promise.all(loaders.map(loader => {
        const request = reload ? loader?.reload() : loader?.loadNext()
        return request?.catch(() => undefined)
      })).then(async () => {
        this.loading = false
        // The resume grid has no sideways scroll to page more books in, so the
        // remaining pages are fetched up front. Bounded by how many books are
        // actually in progress; reload() keeps every page already loaded.
        try {
          while (this.loaderKeepReadingBooks?.hasNextPage) await this.loaderKeepReadingBooks.loadNext()
        } catch (_) {
        }
      })
    },
    async singleEditSeries(series: SeriesDto) {
      if (series.oneshot) {
        let book = (await this.$talesBooks.getBooksList({
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
      this.selectedSeries = []
    },
    async markSelectedSeriesUnread() {
      await Promise.all(this.selectedSeries.map(s =>
        this.$talesSeries.markAsUnread(s.id),
      ))
      this.selectedSeries = []
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
    deleteSeries() {
      this.$store.dispatch('dialogDeleteSeries', this.selectedSeries)
    },
    deleteBooks() {
      this.$store.dispatch('dialogDeleteBook', this.selectedBooks)
    },
    bulkEditMultipleBooks() {
      this.$store.dispatch('dialogUpdateBulkBooks', this.selectedBooks)
    },
    async markSelectedBooksRead() {
      await Promise.all(this.selectedBooks.map(b =>
        this.$talesBooks.updateReadProgress(b.id, {completed: true}),
      ))
      this.selectedBooks = []
    },
    async markSelectedBooksUnread() {
      await Promise.all(this.selectedBooks.map(b =>
        this.$talesBooks.deleteReadProgress(b.id),
      ))
      this.selectedBooks = []
    },
    getLibraryLazy(libraryId: string): LibraryDto | undefined {
      if (libraryId !== LIBRARIES_ALL) {
        return this.$store.getters.getLibraryById(libraryId)
      } else {
        return undefined
      }
    },
  },
})
</script>

<style scoped>
/* Matches the horizontal-scroller row the real shelf uses, and clips rather
   than scrolls so placeholders never invite interaction. */
.dashboard-section__skeleton-row {
  display: flex;
  flex-wrap: nowrap;
  overflow: hidden;
}

/* Cards share one fixed width, so wrapping lines them up in columns as well
   as rows. */
.resume-grid {
  display: flex;
  flex-wrap: wrap;
}

.resume-card {
  flex: 0 0 auto;
}

.resume-card__cover {
  border-radius: var(--tales-radius-sm);
  overflow: hidden;
  position: relative;
}

.resume-card__cover img {
  aspect-ratio: 0.7071;
  display: block;
  object-fit: cover;
  width: 100%;
}

.resume-card__progress {
  bottom: 0;
  left: 0;
  position: absolute;
  right: 0;
}

/* Wraps under the heading on a narrow screen rather than squeezing the title. */
.resume-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

/* Same treatment as the cross opposite it, for the same reason. */
.resume-card__select {
  background: rgba(0, 0, 0, 0.55);
  border-radius: 50%;
  left: 4px;
  padding: 2px;
  position: absolute;
  top: 4px;
}

/* Always visible rather than shown on hover: a touch screen has no hover, and
   this is the only way out of the grid. */
.resume-card__remove {
  background: rgba(0, 0, 0, 0.55);
  position: absolute;
  right: 4px;
  top: 4px;
}

.resume-card__title {
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: var(--tales-text);
  display: -webkit-box;
  font-size: 0.82rem;
  font-weight: 600;
  line-height: 1.3;
  margin-top: 6px;
  overflow: hidden;
}

.resume-card__meta {
  color: var(--tales-muted);
  font-size: 0.72rem;
  margin: 2px 0 0;
}
</style>
