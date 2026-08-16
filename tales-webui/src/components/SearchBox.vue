<template>
  <div>
    <v-autocomplete
      v-model="selectedItem"
      :placeholder="$t('search.search')"
      :title="$t('searchbox.shortcut_hint')"
      :no-data-text="$t('searchbox.no_results')"
      :loading="loading"
      :items="results"
      :hide-no-data="!showResults"
      clearable
      solo
      hide-details
      no-filter
      return-object
      prepend-inner-icon="mdi-magnify"
      append-icon=""
      item-text="id"
      auto-select-first
      :search-input.sync="search"
      :menu-props="{maxHeight: $vuetify.breakpoint.height * .9, minWidth: $vuetify.breakpoint.mdAndUp ? $vuetify.breakpoint.width * .4 : $vuetify.breakpoint.width * .8}"
      @keydown.esc="clear"
      @blur="clear"
      ref="searchbox"
    >
      <template v-slot:selection>
      </template>

      <template v-slot:item="data">
        <template v-if="typeof data.item !== 'object'">
          <v-list-item-content>{{ data.item }}</v-list-item-content>
        </template>

        <template v-if="data.item.type === 'search'">
          <v-list-item-content>{{ $t('searchbox.search_all') }}</v-list-item-content>
        </template>

        <template v-if="data.item.type === 'series'">
          <v-img :src="seriesThumbnailUrl(data.item.id)"
                 height="50"
                 max-width="35"
                 class="my-1 mx-3"
                 contain
          >
                <span v-if="data.item.booksUnreadCount !== 0"
                      class="white--text pa-0 px-1 text-caption"
                      :style="{background: 'orange', position: 'absolute', right: 0}"
                >
                  {{ data.item.booksUnreadCount }}
                </span>
          </v-img>
          <v-list-item-content>
            <v-list-item-title>{{ data.item.metadata.title }}</v-list-item-title>
            <v-list-item-subtitle>{{
                $t('searchbox.in_library', {library: getLibraryName(data.item)})
              }}
            </v-list-item-subtitle>
            <v-list-item-subtitle v-if="data.item.booksMetadata.releaseDate">{{
                new Intl.DateTimeFormat($i18n.locale, {
                  year: 'numeric',
                  timeZone: 'UTC'
                }).format(new Date(data.item.booksMetadata.releaseDate))
              }}
            </v-list-item-subtitle>
          </v-list-item-content>
        </template>

        <template v-if="data.item.type === 'book'">
          <v-img :src="bookThumbnailUrl(data.item.id)"
                 height="50"
                 max-width="35"
                 class="my-1 mx-3"
                 contain
          >
            <div class="unread" v-if="isUnread(data.item)"/>
          </v-img>

          <v-list-item-content>
            <v-list-item-title>{{ data.item.metadata.title }}</v-list-item-title>
            <v-list-item-subtitle v-if="!data.item.oneshot">{{ data.item.seriesTitle }} - {{
                data.item.metadata.number
              }}
            </v-list-item-subtitle>
            <v-list-item-subtitle>{{
                $t('searchbox.in_library', {library: getLibraryName(data.item)})
              }}
            </v-list-item-subtitle>
          </v-list-item-content>
        </template>

      </template>
    </v-autocomplete>
  </div>
</template>

<script lang="ts">
import {bookThumbnailUrl, seriesThumbnailUrl} from '@/functions/urls'
import {debounce} from 'lodash'
import Vue from 'vue'
import {BookDto} from '@/types/tales-books'
import {SeriesDto} from '@/types/tales-series'
import {getReadProgress} from '@/functions/book-progress'
import {ReadStatus} from '@/types/enum-books'
import {
  BookSearch,
  SearchConditionOneShot,
  SearchOperatorIsFalse,
  SeriesSearch,
} from '@/types/tales-search'

export default Vue.extend({
  name: 'SearchBox',
  data: function () {
    return {
      selectedItem: null as unknown as any,
      search: null,
      showResults: false,
      loading: false,
      series: [] as SeriesDto[],
      books: [] as BookDto[],
      pageSize: 10,
    }
  },
  watch: {
    selectedItem(val, old) {
      if (val && val.hasOwnProperty('type')) {
        this.$nextTick(() => {
          this.selectedItem = undefined
        })

        if (val.type === 'series') this.$router.push({name: 'browse-series', params: {seriesId: val.id}})
        else if (val.type === 'book' && val.oneshot) this.$router.push({
          name: 'browse-oneshot',
          params: {seriesId: val.seriesId},
        })
        else if (val.type === 'book') this.$router.push({name: 'browse-book', params: {bookId: val.id}})
        else if (val.type === 'search') this.searchDetails()

        //@ts-ignore
        this.$refs.searchbox.blur()
      }
    },
    search(val) {
      this.searchItems(val)
    },
    showResults(val) {
      !val && this.clear()
    },
  },
  computed: {
    results(): object[] {
      const results = []
      if (this.search) {
        results.push({type: 'search'})
        if (this.series.length > 0) {
          results.push({header: this.$tc('common.series', 2).toString().toUpperCase()})
          results.push(...this.series.map(o => ({...o, type: 'series'})))
        }
        if (this.books.length > 0) {
          results.push({header: this.$t('common.books').toString().toUpperCase()})
          results.push(...this.books.map(o => ({...o, type: 'book'})))
        }
      }
      return results
    },
  },
  methods: {
    // Lets the shell hand keyboard focus to the search field.
    focusSearch() {
      const searchbox = this.$refs.searchbox as any
      searchbox?.focus?.()
    },
    getLibraryName(item: BookDto | SeriesDto): string {
      return this.$store.getters.getLibraryById(item.libraryId).name
    },
    searchItems: debounce(async function (this: any, query: string) {
      if (query) {
        this.loading = true
        this.series = (await this.$talesSeries.getSeriesList({
          fullTextSearch: query,
          condition: new SearchConditionOneShot(new SearchOperatorIsFalse()),
        } as SeriesSearch, {size: this.pageSize})).content
        this.books = (await this.$talesBooks.getBooksList({
          fullTextSearch: query,
        } as BookSearch, {size: this.pageSize})).content
        this.showResults = true
        this.loading = false
      } else {
        this.clear()
      }
    }, 500),
    clear() {
      this.search = null
      this.showResults = false
      this.series = []
      this.books = []
    },
    searchDetails() {
      const s = this.search
      this.clear()
      this.$router.push({name: 'search', query: {q: s}}).catch(e => {
      })
    },
    isUnread(book: BookDto): boolean {
      return getReadProgress(book) === ReadStatus.UNREAD
    },
    seriesThumbnailUrl(seriesId: string): string {
      return seriesThumbnailUrl(seriesId)
    },
    bookThumbnailUrl(bookId: string): string {
      return bookThumbnailUrl(bookId)
    },
  },
})
</script>

<style scoped>
.unread {
  border-left: 15px solid transparent;
  border-right: 15px solid var(--v-secondary-base);
  border-bottom: 15px solid transparent;
  height: 0;
  width: 0;
  position: absolute;
  right: 0;
  z-index: 2;
}
</style>
