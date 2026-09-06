<template>
  <div>
    <v-menu offset-y v-model="menuOpen">
      <template v-slot:activator="{ on }">
        <v-btn icon v-on="on" @click.prevent="" :aria-label="$t('common.actions')">
          <v-icon>mdi-dots-vertical</v-icon>
        </v-btn>
      </template>
      <v-list dense>
        <v-list-item @click="scan(false)" v-if="isAdmin">
          <v-list-item-title>{{ $t('menu.scan_library_files') }}</v-list-item-title>
        </v-list-item>
        <v-list-item @click="scan(true)" class="list-warning" v-if="isAdmin">
          <v-list-item-title>{{ $t('menu.scan_library_files_deep') }}</v-list-item-title>
        </v-list-item>
        <v-list-item @click="confirmAnalyzeModal = true" v-if="isAdmin">
          <v-list-item-title>{{ $t('menu.analyze') }}</v-list-item-title>
        </v-list-item>
        <v-list-item @click="confirmRefreshMetadataModal = true" v-if="isAdmin">
          <v-list-item-title>{{ $t('menu.refresh_metadata') }}</v-list-item-title>
        </v-list-item>
        <v-list-item @click="confirmEmptyTrash = true" v-if="isAdmin">
          <v-list-item-title>{{ $t('menu.empty_trash') }}</v-list-item-title>
        </v-list-item>
        <v-list-item @click="confirmRestoreRemoved = true" v-if="isAdmin">
          <v-list-item-title>{{ $t('menu.restore_removed') }}</v-list-item-title>
        </v-list-item>
        <v-list-item @click="edit" v-if="isAdmin">
          <v-list-item-title>{{ $t('menu.edit') }}</v-list-item-title>
        </v-list-item>

        <!-- Library PIN lock (ADR-0022). Labelled entries rather than icons,
             per the admin UI rules; the lock state is fetched when the menu
             opens, so closed menus cost nothing. -->
        <v-list-item @click="pinDialogVisible = true" v-if="isAdmin">
          <v-list-item-title>{{ $t(hasPin ? 'menu.library_pin_change' : 'menu.library_pin_set') }}</v-list-item-title>
        </v-list-item>
        <v-list-item @click="lockNow" v-if="isAdmin && hasPin && unlocked">
          <v-list-item-title>{{ $t('menu.library_lock_now') }}</v-list-item-title>
        </v-list-item>
        <v-list-item @click="confirmRemovePin = true" class="list-warning" v-if="isAdmin && hasPin">
          <v-list-item-title>{{ $t('menu.library_pin_remove') }}</v-list-item-title>
        </v-list-item>

        <v-list-item @click="promptDeleteLibrary"
                     class="list-danger"
                     v-if="isAdmin"
        >
          <v-list-item-title>{{ $t('menu.delete') }}</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>

    <confirmation-dialog
      v-model="confirmAnalyzeModal"
      :title="$t('dialog.analyze_library.title')"
      :body="$t('dialog.analyze_library.body')"
      :button-confirm="$t('dialog.analyze_library.button_confirm')"
      @confirm="analyze"
    />

    <confirmation-dialog
      v-model="confirmRefreshMetadataModal"
      :title="$t('dialog.refresh_library_metadata.title')"
      :body="$t('dialog.refresh_library_metadata.body')"
      :button-confirm="$t('dialog.refresh_library_metadata.button_confirm')"
      @confirm="refreshMetadata"
    />

    <confirmation-dialog
      v-model="confirmEmptyTrash"
      :title="$t('dialog.empty_trash.title')"
      :body="$t('dialog.empty_trash.body')"
      :button-confirm="$t('dialog.empty_trash.button_confirm')"
      @confirm="emptyTrash"
    />

    <confirmation-dialog
      v-model="confirmRestoreRemoved"
      :title="$t('dialog.restore_removed.title')"
      :body="$t('dialog.restore_removed.body', {name: library.name})"
      :button-confirm="$t('dialog.restore_removed.button_confirm')"
      @confirm="restoreRemoved"
    />

    <library-pin-dialog
      v-model="pinDialogVisible"
      :title="$t('dialog.set_library_pin.title', {name: library.name})"
      :body="$t('dialog.set_library_pin.body')"
      :button-confirm="$t('dialog.set_library_pin.button_confirm')"
      :error-message="pinError"
      @clear-error="pinError = ''"
      @confirm="setPin"
    />

    <confirmation-dialog
      v-model="confirmRemovePin"
      :title="$t('dialog.remove_library_pin.title')"
      :body="$t('dialog.remove_library_pin.body', {name: library.name})"
      :button-confirm="$t('dialog.remove_library_pin.button_confirm')"
      button-confirm-color="error"
      @confirm="removePin"
    />
  </div>
</template>
<script lang="ts">
import Vue from 'vue'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import LibraryPinDialog from '@/components/dialogs/LibraryPinDialog.vue'
import {LibraryDto} from '@/types/tales-libraries'
import {LIBRARY_LOCKS_CHANGED, ShelfViewLibraryLockDto} from '@/shelfview/services/shelfview-library-locks.service'

export default Vue.extend({
  name: 'LibraryActionsMenu',
  components: {ConfirmationDialog, LibraryPinDialog},
  props: {
    library: {
      type: Object as () => LibraryDto,
      required: true,
    },
  },
  data: () => {
    return {
      menuOpen: false,
      confirmAnalyzeModal: false,
      confirmRefreshMetadataModal: false,
      confirmEmptyTrash: false,
      confirmRestoreRemoved: false,
      confirmRemovePin: false,
      pinDialogVisible: false,
      pinError: '',
      lockState: undefined as ShelfViewLibraryLockDto | undefined,
    }
  },
  computed: {
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
    },
    hasPin(): boolean {
      return this.lockState !== undefined
    },
    unlocked(): boolean {
      return this.lockState?.unlocked === true
    },
  },
  watch: {
    menuOpen(open) {
      if (open && this.isAdmin) this.loadLockState()
    },
  },
  methods: {
    scan(scanDeep: boolean) {
      this.$talesLibraries.scanLibrary(this.library, scanDeep)
    },
    analyze() {
      this.$talesLibraries.analyzeLibrary(this.library)
    },
    refreshMetadata() {
      this.$talesLibraries.refreshMetadata(this.library)
    },
    emptyTrash() {
      this.$talesLibraries.emptyTrash(this.library)
    },
    restoreRemoved() {
      this.$talesLibraries.restoreRemovedEntries(this.library)
    },
    edit() {
      this.$store.dispatch('dialogEditLibrary', this.library)
    },
    promptDeleteLibrary() {
      this.$store.dispatch('dialogDeleteLibrary', this.library)
    },
    async loadLockState() {
      try {
        const locks = await this.$shelfViewLibraryLocks.getLockStates()
        this.lockState = locks.find(it => it.libraryId === this.library.id)
      } catch (_) {
        this.lockState = undefined
      }
    },
    async setPin(pin: string) {
      try {
        await this.$shelfViewLibraryLocks.setPin(this.library.id, pin)
      } catch (_) {
        this.pinError = this.$t('dialog.set_library_pin.failed').toString()
        return
      }
      this.pinDialogVisible = false
      this.$eventHub.$emit(LIBRARY_LOCKS_CHANGED)
    },
    async removePin() {
      await this.$shelfViewLibraryLocks.removePin(this.library.id)
      this.$eventHub.$emit(LIBRARY_LOCKS_CHANGED)
    },
    // Relocking hides the library from this session immediately, so refresh
    // the sidebar list and leave the library if the user is inside it.
    async lockNow() {
      await this.$shelfViewLibraryLocks.relock(this.library.id)
      await this.$store.dispatch('getLibraries')
      this.$eventHub.$emit(LIBRARY_LOCKS_CHANGED)
      if (this.$route.params.libraryId === this.library.id) this.$router.push({name: 'dashboard'})
    },
  },
})
</script>
<style scoped>
@import "../../styles/list-warning.css";
</style>
