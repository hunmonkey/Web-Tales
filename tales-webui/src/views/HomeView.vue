<template>
  <div class="app-shell fill-height">
    <confirmation-dialog
      v-model="logoutConfirmDialog"
      :title="$t('dialog.logout.dialog_title')"
      :body="$t('dialog.logout.body')"
      :confirm-text="$t('dialog.logout.confirm')"
      :button-confirm="$t('navigation.logout')"
      button-confirm-color="error"
      @confirm="logout"
    />

    <!-- The app bar is gone (SV-1201), and with it the only way to reopen the
         drawer once it is closed. This is that way back, and the only icon the
         shell adds; it hides itself while the drawer is open. -->
    <v-btn
      v-if="!drawerVisible"
      icon
      fixed
      top
      :left="!$vuetify.rtl"
      :right="$vuetify.rtl"
      class="app-shell__menu-button"
      :aria-label="$t('navigation.open_menu')"
      @click="drawerVisible = true"
    >
      <v-icon>mdi-menu</v-icon>
    </v-btn>

    <v-navigation-drawer
      app
      v-model="drawerVisible"
      :right="$vuetify.rtl"
      width="208"
      class="app-shell__drawer"
    >
      <search-box
        ref="searchBox"
        class="app-shell__search"
      />

      <v-slide-x-transition hide-on-leave>
        <reorder-libraries v-if="showReorder" @dismiss="showReorder = false"/>
      </v-slide-x-transition>

      <template v-if="!showReorder">
        <v-list nav dense class="app-shell__nav">
          <!--   KEEP READING (이어보기)     -->
          <v-list-item :to="{name:'dashboard'}">
            <v-list-item-content>
              <v-list-item-title>{{ $t('dashboard.keep_reading') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>

          <!--   LIBRARIES     -->
          <v-list-item :to="{name:'libraries', params: {libraryId: LIBRARIES_ALL}}">
            <v-list-item-content>
              <v-list-item-title>{{ $t('navigation.libraries') }}</v-list-item-title>
            </v-list-item-content>
            <v-list-item-action class="ma-0">
              <libraries-actions-menu @reorder="showReorder = true"/>
            </v-list-item-action>
          </v-list-item>

          <!--   PINNED LIBRARIES     -->
          <v-list-item v-for="(l, index) in librariesPinned"
                       :key="index"
                       :to="{name:'libraries', params: {libraryId: l.id}}"
          >
            <v-list-item-content>
              <v-list-item-title>{{ l.name }}</v-list-item-title>
              <v-list-item-subtitle
                v-if="l.unavailable"
                class="error--text caption"
              >{{ $t('common.unavailable') }}
              </v-list-item-subtitle>
            </v-list-item-content>
            <v-list-item-action class="ma-0" v-if="isAdmin">
              <library-actions-menu :library="l"/>
            </v-list-item-action>
          </v-list-item>

          <!--   UNPINNED LIBRARIES     -->
          <v-list-group no-action
                        sub-group
                        v-if="librariesUnpinned.length > 0"
                        v-model="expandUnpinned"
          >
            <template v-slot:activator>
              <v-list-item-title>{{ $t('common.more') }}</v-list-item-title>
            </template>

            <v-list-item v-for="(l, index) in librariesUnpinned"
                         :key="index"
                         :to="{name:'libraries', params: {libraryId: l.id}}"
            >
              <v-list-item-content>
                <v-list-item-title>{{ l.name }}</v-list-item-title>
                <v-list-item-subtitle
                  v-if="l.unavailable"
                  class="error--text caption"
                >{{ $t('common.unavailable') }}
                </v-list-item-subtitle>
              </v-list-item-content>
              <v-list-item-action class="ma-0">
                <library-actions-menu :library="l"/>
              </v-list-item-action>
            </v-list-item>
          </v-list-group>

          <!--   ADD FOLDER
                 A labelled entry rather than the `+` icon it replaces:
                 `CLAUDE.md` requires admin actions to be recognisable controls,
                 not icons a user has to guess at. Reuses `dialogAddLibrary`,
                 so no new API. -->
          <v-list-item v-if="isAdmin" @click="addLibrary">
            <v-list-item-content>
              <v-list-item-title>{{ $t('welcome.add_library') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>

          <!--   USERS     -->
          <v-list-item v-if="isAdmin" :to="{name: 'settings-users'}">
            <v-list-item-content>
              <v-list-item-title>{{ $t('users.users') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>

          <!--   PREFERENCES     -->
          <v-list-item :to="{name: 'preferences'}">
            <v-list-item-content>
              <v-list-item-title>{{ $t('common.settings') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>

          <!--   LOGOUT     -->
          <v-list-item @click="logoutConfirmDialog = true">
            <v-list-item-content>
              <v-list-item-title>{{ $t('navigation.logout') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </v-list>

        <v-spacer/>
      </template>
    </v-navigation-drawer>

    <v-main class="app-shell__main fill-height">
      <reusable-dialogs/>
      <toaster-notification/>
      <router-view/>
    </v-main>
  </div>
</template>

<script lang="ts">
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import ReusableDialogs from '@/components/ReusableDialogs.vue'
import LibraryActionsMenu from '@/components/menus/LibraryActionsMenu.vue'
import SearchBox from '@/components/SearchBox.vue'
import {resolveShellShortcut} from '@/functions/shell-shortcuts'
import Vue from 'vue'
import {LIBRARIES_ALL} from '@/types/library'
import ToasterNotification from '@/components/ToasterNotification.vue'
import {LibraryDto} from '@/types/tales-libraries'
import LibrariesActionsMenu from '@/components/menus/LibrariesActionsMenu.vue'
import ReorderLibraries from '@/components/ReorderLibraries.vue'

export default Vue.extend({
  name: 'HomeView',
  components: {
    ConfirmationDialog,
    ReorderLibraries,
    LibrariesActionsMenu,
    ToasterNotification,
    LibraryActionsMenu,
    SearchBox,
    ReusableDialogs,
  },
  data: function () {
    return {
      LIBRARIES_ALL,
      drawerVisible: this.$vuetify.breakpoint.lgAndUp,
      expandUnpinned: false,
      showReorder: false,
      logoutConfirmDialog: false,
    }
  },
  // The four admin requests that used to run here fed the announcement and
  // media-check badges and the update screen, all of which are gone (SV-1200,
  // SV-1204a). Signing in as an admin now issues none of them.
  created() {
    this.checkRoute(this.$route)
  },
  mounted() {
    document.addEventListener('keydown', this.onShellShortcut)
  },
  beforeDestroy() {
    document.removeEventListener('keydown', this.onShellShortcut)
  },
  watch: {
    $route(to, from) {
      this.checkRoute(to)
    },
  },
  computed: {
    libraries(): LibraryDto[] {
      return this.$store.getters.getLibraries
    },
    librariesPinned(): LibraryDto[] {
      return this.$store.getters.getLibrariesPinned
    },
    librariesUnpinned(): LibraryDto[] {
      return this.$store.getters.getLibrariesUnpinned
    },
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
    },
  },
  methods: {
    // Browsing had no shortcuts at all; these two cover the moves people repeat
    // most. The decision itself lives in functions/shell-shortcuts.ts.
    onShellShortcut(event: KeyboardEvent) {
      const shortcut = resolveShellShortcut(event, {
        drawerVisible: this.drawerVisible,
        drawerIsOverlay: !this.$vuetify.breakpoint.lgAndUp,
      })
      if (!shortcut) return

      event.preventDefault()
      if (shortcut === 'FOCUS_SEARCH') this.focusSearchBox()
      else this.drawerVisible = false
    },
    focusSearchBox() {
      const searchBox = this.$refs.searchBox as any
      searchBox?.focusSearch?.()
    },
    checkRoute(to) {
      if (this.librariesUnpinned.some(it => it.id === to.params.libraryId)) this.expandUnpinned = true
      else if (this.librariesPinned.some(it => it.id === to.params.libraryId)) this.expandUnpinned = false
    },
    logout() {
      this.$store.dispatch('logout')
      this.$router.push({name: 'login', query: {'logout': true}})
    },
    addLibrary() {
      this.$store.dispatch('dialogAddLibrary')
    },
  },
})
</script>
