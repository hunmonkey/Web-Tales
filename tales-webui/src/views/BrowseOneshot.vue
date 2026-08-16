<template>
  <div v-if="!$_.isEmpty(book) && !$_.isEmpty(series)">
    <toolbar-sticky>
      <v-tooltip bottom :disabled="!isAdmin">
        <template v-slot:activator="{ on }">
          <v-btn icon
                 v-on="on"
                 :to="parentLocation"
                 :aria-label="$t('common.back')"
          >
            <rtl-icon icon="mdi-arrow-left" rtl="mdi-arrow-right"/>
          </v-btn>
        </template>
        <span>{{ $t('common.go_to_library') }}</span>
      </v-tooltip>

      <!--   Action menu   -->
      <oneshot-actions-menu v-if="book"
                            :book="book"
                            :series="series"
      />

      <v-btn icon @click="editBook" v-if="isAdmin" :aria-label="$t('menu.edit_metadata')">
        <v-icon>mdi-pencil</v-icon>
      </v-btn>

      <v-spacer/>

    </toolbar-sticky>

    <v-container fluid class="pa-6">
      <v-row>
        <v-col cols="4" sm="4" md="auto" lg="auto" xl="auto">
          <item-card
            v-if="book.hasOwnProperty('id')"
            :width="$vuetify.breakpoint.xsOnly ? 128 : 212"
            :item="book"
            thumbnail-only
            no-link
            :action-menu="false"
          ></item-card>
          <div v-if="isInProgress"
               class="text-caption text-center mt-1"
               :title="$t('common.read_on', {date: readProgressDate})"
          >
            {{ $tc('common.pages_left', pagesLeft) }}
          </div>
          <div v-else-if="isRead"
               class="text-caption text-center mt-1"
          >
            {{ $t('common.read_on', {date: readProgressDate}) }}
          </div>
        </v-col>

        <v-col cols="8">
          <v-container>
            <v-row>
              <v-col class="py-1">
                <span class="text-h6">{{ book.metadata.title }}</span>
                <router-link
                  class="caption link-underline"
                  :class="$vuetify.breakpoint.smAndUp ? 'mx-1' : ''"
                  :style="$vuetify.breakpoint.xsOnly ? 'display: block' : ''"
                  :to="{name:'browse-libraries', params: {libraryId: book.libraryId }}"
                >{{ $t('searchbox.in_library', {library: getLibraryName(book)}) }}
                </router-link>
              </v-col>
            </v-row>

            <v-row class="text-body-2">
              <v-col class="py-1 pe-0" cols="auto" v-if="series.metadata && series.metadata.ageRating">
                <v-chip label small link
                        :to="{name:'browse-libraries', params: {libraryId: series.libraryId }, query: {ageRating: [new SearchConditionAgeRating(new SearchOperatorIs(series.metadata.ageRating.toString()))]}}"
                >
                  {{ series.metadata.ageRating }}+
                </v-chip>
              </v-col>
              <v-col class="py-1 pe-0" cols="auto" v-if="series.metadata.language">
                <v-chip label small link
                        :to="{name:'browse-libraries', params: {libraryId: series.libraryId }, query: {language: [new SearchConditionLanguage(new SearchOperatorIs(series.metadata.language))]}}"
                >
                  {{ languageDisplay }}
                </v-chip>
              </v-col>
              <v-col class="py-1 pe-0" cols="auto"
                     v-if="series.metadata.readingDirection">
                <v-chip label small>
                  {{ $t(`enums.reading_direction.${series.metadata.readingDirection}`) }}
                </v-chip>
              </v-col>
            </v-row>

            <v-row class="text-caption" align="center">
              <v-col cols="auto" v-if="book.media.status === MediaStatus.UNKNOWN">
                {{ $t('book_card.unknown') }}
              </v-col>

              <v-col cols="auto" v-else>
                {{ $tc('common.pages_n', book.media.pagesCount) }}
              </v-col>

              <v-col cols="auto" v-if="book.metadata.releaseDate">
                {{
                  new Intl.DateTimeFormat($i18n.locale, {
                    dateStyle: 'long',
                    timeZone: 'UTC'
                  }).format(new Date(book.metadata.releaseDate))
                }}
              </v-col>

              <v-col class="py-1 pe-0"
                     cols="auto"
                     v-if="book.media.status === MediaStatus.OUTDATED">
                <v-tooltip bottom :disabled="!isAdmin">
                  <template v-slot:activator="{ on }">
                    <v-chip label small color="warning" v-on="on">
                      {{ $t('common.outdated') }}
                    </v-chip>
                  </template>
                  {{ $t('browse_book.outdated_tooltip') }}
                </v-tooltip>
              </v-col>

              <v-col class="py-1 pe-0" cols="auto" v-if="unavailable">
                <v-chip label small color="error">
                  {{ $t('common.unavailable') }}
                </v-chip>
              </v-col>
            </v-row>

            <template v-if="$vuetify.breakpoint.smAndUp">
              <v-row class="align-center">
                <v-col cols="auto">
                  <v-btn color="accent"
                         small
                         :title="$t('browse_book.read_book')"
                         :to="{name: readRouteName, params: { bookId: book.id}, query: { context: context.origin, contextId: context.id}}"
                         :disabled="!canRead"
                  >
                    <v-icon left small>mdi-book-open-page-variant</v-icon>
                    {{ $t('common.read') }}
                  </v-btn>
                </v-col>

                <v-col cols="auto">
                  <v-btn small
                         :title="$t('browse_book.read_incognito')"
                         :to="{name: readRouteName, params: { bookId: book.id}, query: { context: context.origin, contextId: context.id, incognito: true}}"
                         :disabled="!canRead"
                  >
                    <v-icon left small>mdi-incognito</v-icon>
                    {{ $t('common.read') }}
                  </v-btn>
                </v-col>

                <v-col cols="auto" v-if="canDownload">
                  <v-btn :title="$t('browse_book.download_file')"
                         small
                         :href="fileUrl">
                    <v-icon left small>mdi-file-download</v-icon>
                    {{ $t('common.download') }}
                  </v-btn>
                </v-col>
              </v-row>

              <v-row v-if="book.metadata.summary">
                <v-col>
                  <read-more v-model="readMore">{{ book.metadata.summary }}</read-more>
                </v-col>
              </v-row>
            </template>
          </v-container>
        </v-col>
      </v-row>

      <template v-if="$vuetify.breakpoint.xsOnly">
        <v-row class="align-center">
          <v-col cols="auto">
            <!-- `small` is 28px, which is barely half of what a thumb needs for
                 the one button this whole screen exists to offer. -->
            <v-btn color="accent"
                   large
                   :title="$t('browse_book.read_book')"
                   :to="{name: readRouteName, params: { bookId: book.id}, query: { context: context.origin, contextId: context.id}}"
                   :disabled="!canRead"
            >
              <v-icon left small>mdi-book-open-page-variant</v-icon>
              {{ $t('common.read') }}
            </v-btn>
          </v-col>

          <v-col cols="auto">
            <!-- Both buttons used to read "읽기"; the only thing separating them
                 was a `title` tooltip, which a touch screen never shows. Picking
                 the wrong one silently stops saving reading progress. -->
            <v-btn large
                   :title="$t('browse_book.read_incognito')"
                   :to="{name: readRouteName, params: { bookId: book.id}, query: { context: context.origin, contextId: context.id, incognito: true}}"
                   :disabled="!canRead"
            >
              <v-icon left small>mdi-incognito</v-icon>
              {{ $t('browse_book.read_incognito') }}
            </v-btn>
          </v-col>

          <v-col cols="auto" v-if="canDownload">
            <v-btn :title="$t('browse_book.download_file')"
                   small
                   :href="fileUrl">
              <v-icon left small>mdi-file-download</v-icon>
              {{ $t('common.download') }}
            </v-btn>
          </v-col>
        </v-row>

        <v-row v-if="book.metadata.summary">
          <v-col>
            <read-more v-model="readMore">{{ book.metadata.summary }}</read-more>
          </v-col>
        </v-row>
      </template>

      <!--  Publisher    -->
      <v-row v-if="series.metadata.publisher" class="align-center text-caption">
        <v-col cols="4" sm="3" md="2" xl="1" class="py-1 text-uppercase">{{ $t('common.publisher') }}</v-col>
        <v-col cols="8" sm="9" md="10" xl="11" class="py-1">
          <v-chip
            class="me-2"
            :title="series.metadata.publisher"
            :to="{name:'browse-libraries', params: {libraryId: series.libraryId }, query: {publisher: [new SearchConditionPublisher(new SearchOperatorIs(series.metadata.publisher))]}}"
            label
            small
            outlined
            link
          >{{ series.metadata.publisher }}
          </v-chip>
        </v-col>
      </v-row>

      <!--  Genres    -->
      <v-row v-if="series.metadata.genres.length > 0" class="align-center text-caption">
        <v-col cols="4" sm="3" md="2" xl="1" class="py-1 text-uppercase">{{ $t('common.genre') }}</v-col>
        <v-col cols="8" sm="9" md="10" xl="11" class="py-1 text-capitalize">
          <vue-horizontal>
            <template v-slot:btn-prev>
              <v-btn icon small :aria-label="$t('common.scroll_left')">
                <v-icon>mdi-chevron-left</v-icon>
              </v-btn>
            </template>

            <template v-slot:btn-next>
              <v-btn icon small :aria-label="$t('common.scroll_right')">
                <v-icon>mdi-chevron-right</v-icon>
              </v-btn>
            </template>
            <v-chip v-for="(t, i) in $_.sortBy(series.metadata.genres)"
                    :key="i"
                    class="me-2"
                    :title="t"
                    :to="{name:'browse-libraries', params: {libraryId: series.libraryId }, query: {genre: [new SearchConditionGenre(new SearchOperatorIs(t))]}}"
                    label
                    small
                    outlined
                    link
            >{{ t }}
            </v-chip>
          </vue-horizontal>
        </v-col>
      </v-row>

      <v-row v-for="role in displayedRoles"
             :key="role"
             class="align-center text-caption"
      >
        <v-col cols="4" sm="3" md="2" xl="1" class="py-1 text-uppercase">
          {{ $te(`author_roles.${role}`) ? $t(`author_roles.${role}`) : role }}
        </v-col>
        <v-col cols="8" sm="9" md="10" xl="11" class="py-1">
          <vue-horizontal>
            <template v-slot:btn-prev>
              <v-btn icon small :aria-label="$t('common.scroll_left')">
                <v-icon>mdi-chevron-left</v-icon>
              </v-btn>
            </template>

            <template v-slot:btn-next>
              <v-btn icon small :aria-label="$t('common.scroll_right')">
                <v-icon>mdi-chevron-right</v-icon>
              </v-btn>
            </template>
            <v-chip v-for="(name, i) in authorsByRole[role]"
                    :key="i"
                    class="me-2"
                    :title="name"
                    :to="{name:'browse-libraries', params: {libraryId: book.libraryId }, query: {[role]: [name]}}"
                    label
                    small
                    outlined
                    link
            >{{ name }}
            </v-chip>
          </vue-horizontal>
        </v-col>
      </v-row>

      <v-row v-if="book.metadata.tags.length > 0" class="align-center text-caption">
        <v-col cols="4" sm="3" md="2" xl="1" class="py-1 text-uppercase">{{ $i18n.t('common.tags') }}</v-col>
        <v-col cols="8" sm="9" md="10" xl="11" class="py-1 text-capitalize">
          <vue-horizontal>
            <template v-slot:btn-prev>
              <v-btn icon small :aria-label="$t('common.scroll_left')">
                <v-icon>mdi-chevron-left</v-icon>
              </v-btn>
            </template>

            <template v-slot:btn-next>
              <v-btn icon small :aria-label="$t('common.scroll_right')">
                <v-icon>mdi-chevron-right</v-icon>
              </v-btn>
            </template>
            <v-chip v-for="(t, i) in book.metadata.tags"
                    :key="i"
                    class="me-2"
                    :title="t"
                    :to="{name:'browse-libraries', params: {libraryId: book.libraryId}, query: {tag: [new SearchConditionTag(new SearchOperatorIs(t))]}}"
                    label
                    small
                    outlined
                    link
            >{{ t }}
            </v-chip>
          </vue-horizontal>
        </v-col>
      </v-row>


      <v-row>
        <v-col cols="12">
        </v-col>
      </v-row>

      <v-row v-if="book.metadata.links.length > 0" class="align-center text-caption">
        <v-col class="py-1 text-uppercase" cols="4" sm="3" md="2" xl="1">{{ $t('browse_book.links') }}</v-col>
        <v-col class="py-1" cols="8" sm="9" md="10" xl="11">
          <v-chip
            v-for="(link, i) in book.metadata.links"
            :href="link.url"
            rel="noreferrer"
            target="_blank"
            class="me-2"
            label
            small
            outlined
            link
            :key="i"
          >
            {{ link.label }}
            <v-icon
              x-small
              color="grey"
              class="ps-1"
            >
              mdi-open-in-new
            </v-icon>
          </v-chip>
        </v-col>
      </v-row>

      <v-row class="align-center text-caption">
        <v-col class="py-1 text-uppercase" cols="4" sm="3" md="2" xl="1">{{ $t('browse_book.size') }}</v-col>
        <v-col class="py-1" cols="8" sm="9" md="10" xl="11">{{ book.size }}</v-col>
      </v-row>

      <v-row v-if="book.media.comment" class="align-center text-caption">
        <v-col class="py-1 text-uppercase" cols="4" sm="3" md="2" xl="1">{{ $t('browse_book.comment') }}</v-col>
        <v-col class="py-1 error--text font-weight-bold" cols="8" sm="9" md="10" xl="11">{{ mediaComment }}</v-col>
      </v-row>

      <v-row class="align-center text-caption">
        <v-col class="py-1 text-uppercase" cols="4" sm="3" md="2" xl="1">{{ $t('browse_book.format') }}</v-col>
        <v-col class="py-1" cols="8" sm="9" md="10" xl="11">{{ format.type }}</v-col>
      </v-row>

      <v-row v-if="book.metadata.isbn" class="align-center text-caption">
        <v-col class="py-1 text-uppercase" cols="4" sm="3" md="2" xl="1">{{ $t('browse_book.isbn') }}</v-col>
        <v-col class="py-1" cols="8" sm="9" md="10" xl="11">{{ book.metadata.isbn }}</v-col>
      </v-row>

      <v-row class="align-center text-caption">
        <v-col class="py-1 text-uppercase" cols="4" sm="3" md="2" xl="1">{{ $t('browse_book.file') }}</v-col>
        <v-col class="py-1 metadata-value" cols="8" sm="9" md="10" xl="11">{{ book.url }}</v-col>
      </v-row>

      <v-row class="align-center text-caption">
        <v-col class="py-1 text-uppercase" cols="4" sm="3" md="2" xl="1">{{ $t('browse_book.date_created') }}</v-col>
        <v-col class="py-1" cols="8" sm="9" md="10" xl="11">{{
            new Intl.DateTimeFormat($i18n.locale, {
              dateStyle: 'long',
              timeStyle: 'short'
            }).format(new Date(book.created))
          }}
        </v-col>
      </v-row>

      <v-row class="align-center text-caption">
        <v-col class="py-1 text-uppercase" cols="4" sm="3" md="2" xl="1">{{ $t('browse_book.date_modified') }}</v-col>
        <v-col class="py-1" cols="8" sm="9" md="10" xl="11">{{
            new Intl.DateTimeFormat($i18n.locale, {
              dateStyle: 'long',
              timeStyle: 'short'
            }).format(new Date(book.lastModified))
          }}
        </v-col>
      </v-row>

    </v-container>

  </div>
</template>

<script lang="ts">
import ItemCard from '@/components/ItemCard.vue'
import ToolbarSticky from '@/components/bars/ToolbarSticky.vue'
import {groupAuthorsByRole} from '@/functions/authors'
import {getBookFormatFromMedia, getBookReadRouteFromMedia} from '@/functions/book-format'
import {getPagesLeft, getReadProgress, getReadProgressPercentage} from '@/functions/book-progress'
import {getBookTitleCompact} from '@/functions/book-title'
import {bookFileUrl, seriesThumbnailUrl} from '@/functions/urls'
import {MediaStatus, ReadStatus} from '@/types/enum-books'
import {
  BOOK_CHANGED,
  BOOK_DELETED,
  LIBRARY_DELETED,
  READPROGRESS_CHANGED,
  READPROGRESS_DELETED,
  SERIES_CHANGED,
  SERIES_DELETED,
} from '@/types/events'
import Vue from 'vue'
import {BookDto, BookFormat} from '@/types/tales-books'
import {Context, ContextOrigin} from '@/types/context'
import ReadMore from '@/components/ReadMore.vue'
import VueHorizontal from 'vue-horizontal'
import {authorRoles} from '@/types/author-roles'
import {convertErrorCodes} from '@/functions/error-codes'
import RtlIcon from '@/components/RtlIcon.vue'
import {
  BookSseDto,
  LibrarySseDto,
  ReadProgressSseDto,
  SeriesSseDto,
} from '@/types/tales-sse'
import {RawLocation} from 'vue-router/types/router'
import {Oneshot, SeriesDto} from '@/types/tales-series'
import OneshotActionsMenu from '@/components/menus/OneshotActionsMenu.vue'
import {
  BookSearch,
  SearchConditionAgeRating,
  SearchConditionGenre,
  SearchConditionLanguage,
  SearchConditionPublisher,
  SearchConditionSeriesId,
  SearchConditionTag,
  SearchOperatorIs,
} from '@/types/tales-search'

const tags = require('language-tags')

export default Vue.extend({
  name: 'BrowseOneshot',
  components: {
    OneshotActionsMenu,
    ReadMore, ToolbarSticky, ItemCard, VueHorizontal, RtlIcon,
  },
  data: () => {
    return {
      SearchConditionPublisher,
      SearchConditionGenre,
      SearchConditionTag,
      SearchConditionLanguage,
      SearchConditionAgeRating,
      SearchOperatorIs,
      MediaStatus,
      ContextOrigin,
      book: {} as BookDto,
      series: {} as SeriesDto,
      context: {} as Context,
      readMore: false,
    }
  },
  async created() {
    this.loadSeries(this.seriesId)
    this.$eventHub.$on(SERIES_CHANGED, this.seriesChanged)
    this.$eventHub.$on(SERIES_DELETED, this.seriesDeleted)
    this.$eventHub.$on(BOOK_CHANGED, this.bookChanged)
    this.$eventHub.$on(BOOK_DELETED, this.bookDeleted)
    this.$eventHub.$on(READPROGRESS_CHANGED, this.readProgressChanged)
    this.$eventHub.$on(READPROGRESS_DELETED, this.readProgressChanged)
    this.$eventHub.$on(LIBRARY_DELETED, this.libraryDeleted)
  },
  beforeDestroy() {
    this.$eventHub.$off(SERIES_CHANGED, this.seriesChanged)
    this.$eventHub.$off(SERIES_DELETED, this.seriesDeleted)
    this.$eventHub.$off(BOOK_CHANGED, this.bookChanged)
    this.$eventHub.$off(BOOK_DELETED, this.bookDeleted)
    this.$eventHub.$off(READPROGRESS_CHANGED, this.readProgressChanged)
    this.$eventHub.$off(READPROGRESS_DELETED, this.readProgressChanged)
    this.$eventHub.$off(LIBRARY_DELETED, this.libraryDeleted)
  },
  props: {
    seriesId: {
      type: String,
      required: true,
    },
  },
  async beforeRouteUpdate(to, from, next) {
    if (to.params.seriesId !== from.params.seriesId) {
      this.readMore = false
      this.loadSeries(to.params.seriesId)
    }

    next()
  },
  computed: {
    readRouteName(): string {
      return getBookReadRouteFromMedia(this.book.media)
    },
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
    },
    unavailable(): boolean {
      return this.book.deleted || this.$store.getters.getLibraryById(this.book.libraryId).unavailable
    },
    canRead(): boolean {
      return this.book.media.status === 'READY' && this.$store.getters.mePageStreaming && !this.unavailable
    },
    canDownload(): boolean {
      return this.$store.getters.meFileDownload && !this.unavailable
    },
    thumbnailUrl(): string {
      return seriesThumbnailUrl(this.seriesId)
    },
    fileUrl(): string {
      return bookFileUrl(this.book.id)
    },
    format(): BookFormat {
      return getBookFormatFromMedia(this.book.media)
    },
    authorsByRole(): any {
      return groupAuthorsByRole(this.book.metadata.authors)
    },
    isRead(): boolean {
      return getReadProgress(this.book) === ReadStatus.READ
    },
    isUnread(): boolean {
      return getReadProgress(this.book) === ReadStatus.UNREAD
    },
    isInProgress(): boolean {
      return getReadProgress(this.book) === ReadStatus.IN_PROGRESS
    },
    pagesLeft(): number {
      return getPagesLeft(this.book)
    },
    readProgressPercentage(): number {
      return getReadProgressPercentage(this.book)
    },
    readProgressDate(): string | undefined {
      if (this.book.readProgress)
        return new Intl.DateTimeFormat(this.$i18n.locale, {
          dateStyle: 'medium',
          timeStyle: 'short',
        } as Intl.DateTimeFormatOptions).format(this.book.readProgress.readDate)
      return undefined
    },
    mediaComment(): string {
      return convertErrorCodes(this.book.media.comment)
    },
    parentLocation(): RawLocation {
      return {name: 'browse-libraries', params: {libraryId: this.book.libraryId}}
    },
    displayedRoles(): string[] {
      const allRoles = this.$_.uniq([...authorRoles, ...(this.book.metadata.authors.map(x => x.role))])
      return allRoles.filter(x => this.authorsByRole[x])
    },
    languageDisplay(): string {
      return tags(this.series.metadata.language)?.language()?.descriptions()[0] || this.series.metadata.language
    },
  },
  methods: {
    getLibraryName(item: BookDto): string {
      return this.$store.getters.getLibraryById(item.libraryId).name
    },
    libraryDeleted(event: LibrarySseDto) {
      if (event.libraryId === this.book.libraryId) {
        this.$router.push({name: 'home'})
      }
    },
    seriesChanged(event: SeriesSseDto) {
      if (event.seriesId === this.seriesId)
        this.$talesSeries.getOneSeries(this.seriesId)
          .then(v => this.series = v)
    },
    seriesDeleted(event: SeriesSseDto) {
      if (event.seriesId === this.seriesId) {
        this.$router.push({name: 'browse-libraries', params: {libraryId: this.series.libraryId}})
      }
    },
    bookChanged(event: BookSseDto) {
      if (event.bookId === this.book.id) this.loadBook(this.seriesId)
    },
    bookDeleted(event: BookSseDto) {
      if (event.bookId === this.book.id) {
        this.$router.push({name: 'browse-libraries', params: {libraryId: this.book.libraryId}})
      }
    },
    readProgressChanged(event: ReadProgressSseDto) {
      if (event.bookId === this.book.id) this.loadBook(this.seriesId)
    },
    async loadSeries(seriesId: string) {
      this.$talesSeries.getOneSeries(seriesId)
        .then(v => this.series = v)

      // parse query params to get context and contextId
      if (this.$route.query.contextId && this.$route.query.context
        && Object.values(ContextOrigin).includes(this.$route.query.context as ContextOrigin)) {
        this.context = {
          origin: this.$route.query.context as ContextOrigin,
          id: this.$route.query.contextId as string,
        }
        this.series.context = this.context
      }

      await this.loadBook(seriesId)
    },
    async loadBook(seriesId: string) {
      this.book = (await this.$talesBooks.getBooksList({
        condition: new SearchConditionSeriesId(new SearchOperatorIs(seriesId)),
      } as BookSearch)).content[0]

      // parse query params to get context and contextId
      if (this.$route.query.contextId && this.$route.query.context
        && Object.values(ContextOrigin).includes(this.$route.query.context as ContextOrigin)) {
        this.context = {
          origin: this.$route.query.context as ContextOrigin,
          id: this.$route.query.contextId as string,
        }
        this.book.context = this.context
      }

      if (this.$_.has(this.book, 'metadata.title')) {
        document.title = `Tales - ${getBookTitleCompact(this.book.metadata.title, this.book.seriesTitle)}`
      }

    },
    analyze() {
      this.$talesBooks.analyzeBook(this.book)
    },
    refreshMetadata() {
      this.$talesBooks.refreshMetadata(this.book)
    },
    editBook() {
      this.$store.dispatch('dialogUpdateOneshots', {series: this.series, book: this.book} as Oneshot)
    },
  },
})
</script>

<style scoped>
/* A Korean file name has no spaces to break at, so on a 412px screen it ran out
   of its column and scrolled the whole page sideways. Vuetify 2 has no
   text-break utility, hence the local class. */
.metadata-value {
  word-break: break-all;
}
</style>
