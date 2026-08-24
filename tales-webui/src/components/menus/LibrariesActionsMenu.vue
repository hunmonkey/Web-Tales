<template>
  <div>
    <v-menu offset-y>
      <template v-slot:activator="{ on }">
        <v-btn icon v-on="on" @click.prevent="" :aria-label="$t('common.actions')">
          <v-icon>mdi-dots-vertical</v-icon>
        </v-btn>
      </template>
      <v-list dense>
        <v-subheader>{{ $t('libraries_sort.title') }}</v-subheader>
        <v-list-item
          v-for="option in sortOptions"
          :key="option.value"
          :data-testid="`libraries-sort-${option.value.toLowerCase()}`"
          @click="selectSort(option.value)"
        >
          <v-list-item-title>{{ option.text }}</v-list-item-title>
          <v-list-item-icon class="ma-0 ms-2">
            <v-icon v-if="option.value === librarySort" small>mdi-check</v-icon>
          </v-list-item-icon>
        </v-list-item>

        <v-divider/>

        <v-list-item @click="reorder">
          <v-list-item-title>{{ $t('common.reorder') }}</v-list-item-title>
        </v-list-item>
        <v-list-item @click="scan(false)" v-if="isAdmin">
          <v-list-item-title>{{ $t('server.server_management.button_scan_libraries') }}</v-list-item-title>
        </v-list-item>
        <v-list-item @click="scan(true)" class="list-warning" v-if="isAdmin">
          <v-list-item-title>{{ $t('server.server_management.button_scan_libraries_deep') }}</v-list-item-title>
        </v-list-item>
        <v-list-item @click="confirmEmptyTrash = true" v-if="isAdmin">
          <v-list-item-title>{{ $t('server.server_management.button_empty_trash') }}</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>

    <confirmation-dialog
      v-model="confirmEmptyTrash"
      :title="$t('dialog.empty_trash.title')"
      :body="$t('dialog.empty_trash.body')"
      :button-confirm="$t('dialog.empty_trash.button_confirm')"
      @confirm="emptyTrash"
    />
  </div>
</template>
<script lang="ts">
import Vue from 'vue'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import {CLIENT_SETTING, LIBRARY_SORTS, LibrarySort} from '@/types/tales-clientsettings'

export default Vue.extend({
  name: 'LibrariesActionsMenu',
  components: {ConfirmationDialog},
  data: () => {
    return {
      confirmEmptyTrash: false,
    }
  },
  computed: {
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
    },
    librarySort(): LibrarySort {
      return this.$store.getters.getClientSettingsLibrariesSort
    },
    sortOptions(): Array<{ value: LibrarySort, text: string }> {
      return LIBRARY_SORTS.map(value => ({
        value,
        text: this.$t(`libraries_sort.${value.toLowerCase()}`) as string,
      }))
    },
  },
  methods: {
    selectSort(sort: LibrarySort) {
      if (sort === this.librarySort) return

      this.$store.dispatch('saveUserPreference', {
        key: CLIENT_SETTING.WEBUI_LIBRARIES_SORT,
        value: sort,
      })
    },
    reorder() {
      this.$emit('reorder')
    },
    scan(scanDeep: boolean) {
      this.$store.state.talesLibraries.libraries.forEach(library => {
        this.$talesLibraries.scanLibrary(library, scanDeep)
      })
    },
    emptyTrash() {
      this.$store.state.talesLibraries.libraries.forEach(library => {
        this.$talesLibraries.emptyTrash(library)
      })
    },
  },
})
</script>
<style scoped>
@import "../../styles/list-warning.css";
</style>
