<template>
  <div>
    <v-bottom-navigation
      v-if="bottomNavigation"
      grow color="primary"
      :app="$vuetify.breakpoint.smAndUp"
      :fixed="bottomNavigation"
    >
      <v-btn :to="{name: 'browse-libraries', params: {libraryId: libraryId}}">
        <span class="caption">{{ $t('library_navigation.browse_series') }}</span>
        <v-icon>mdi-bookshelf</v-icon>
      </v-btn>

      <v-btn :to="{name: 'browse-books', params: {libraryId: libraryId}}">
        <span class="caption">{{ $t('library_navigation.browse_books') }}</span>
        <v-icon>mdi-book-multiple</v-icon>
      </v-btn>

    </v-bottom-navigation>

    <template
      v-if="!bottomNavigation"
    >
      <v-btn :to="{name: 'browse-libraries', params: {libraryId: libraryId}}"
             text
             class="mx-1"
      >
        {{ $t('library_navigation.browse_series') }}
      </v-btn>

      <v-btn :to="{name: 'browse-books', params: {libraryId: libraryId}}"
             text
             class="mx-1"
      >
        {{ $t('library_navigation.browse_books') }}
      </v-btn>

    </template>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'

// Only the series and books tabs remain (SV-1204d/e). The collection and
// read-list tabs each needed a count query and two SSE subscriptions to decide
// whether to show themselves; with those gone this component is pure links.
export default Vue.extend({
  name: 'LibraryNavigation',
  props: {
    libraryId: {
      type: String,
      required: true,
    },
    bottomNavigation: {
      type: Boolean,
      default: false,
    },
  },
})
</script>

<style scoped>

</style>
