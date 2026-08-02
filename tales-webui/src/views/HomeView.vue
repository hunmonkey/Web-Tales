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

    <v-app-bar
      app
      flat
      color="transparent"
      class="app-shell__topbar"
    >
      <v-badge
        dot
        offset-x="15"
        offset-y="20"
        :value="drawerVisible ? 0 : $store.state.booksToCheck + $store.getters.getUnreadAnnouncementsCount()"
        :color="$store.state.booksToCheck ? 'accent' : 'info'"
        class="ms-n3"
      >
        <v-app-bar-nav-icon @click.stop="toggleDrawer"/>
      </v-badge>

      <logo-mark
        v-if="$vuetify.breakpoint.smAndDown"
        class="app-shell__topbar-brand"
        :max-width="$vuetify.breakpoint.xs ? 38 : 110"
        :symbol-only="$vuetify.breakpoint.xs"
      />

      <search-box
        ref="searchBox"
        class="app-shell__search flex-fill"
      />

    </v-app-bar>

    <v-navigation-drawer
      app
      v-model="drawerVisible"
      :right="$vuetify.rtl"
      width="292"
      class="app-shell__drawer"
    >
      <v-list-item @click="$router.push({name: 'home'})" inactive class="app-shell__brand">
        <logo-mark :max-width="146"/>

        <v-tooltip left>
          <template v-slot:activator="{ on }">
            <v-progress-linear
              :active="taskCount > 0"
              indeterminate
              absolute
              bottom
              height="5"
              color="secondary"
              v-on="on"
            />
          </template>
          <div class="mb-2">{{ $tc('common.pending_tasks', taskCount) }}</div>
          <div v-for="taskType in Object.keys(taskCountByType)"
               :key="taskType"
          >{{ taskType }}: {{ taskCountByType[taskType] }}
          </div>
        </v-tooltip>
      </v-list-item>

      <v-divider/>

      <v-slide-x-transition hide-on-leave>
        <reorder-libraries v-if="showReorder" @dismiss="showReorder = false"/>
      </v-slide-x-transition>

      <template v-if="!showReorder">
        <v-list nav dense class="app-shell__nav">
          <!--   KEEP READING (이어보기)     -->
          <v-list-item :to="{name:'dashboard'}">
            <v-list-item-icon>
              <v-icon>mdi-book-open-page-variant</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>{{ $t('dashboard.keep_reading') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>

          <!--   LIBRARIES     -->
          <v-list-item :to="{name:'libraries', params: {libraryId: LIBRARIES_ALL}}">
            <v-list-item-icon>
              <v-icon>mdi-book-multiple</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>{{ $t('navigation.libraries') }}</v-list-item-title>
            </v-list-item-content>
            <v-list-item-action v-if="isAdmin" class="ma-0">
              <v-btn icon @click.stop.capture.prevent="addLibrary" :aria-label="$t('welcome.add_library')">
                <v-icon>mdi-plus</v-icon>
              </v-btn>
            </v-list-item-action>
            <v-list-item-action class="ma-0">
              <libraries-actions-menu @reorder="showReorder = true"/>
            </v-list-item-action>
          </v-list-item>

          <!--   PINNED LIBRARIES     -->
          <v-list-item v-for="(l, index) in librariesPinned"
                       :key="index"
                       :to="{name:'libraries', params: {libraryId: l.id}}"
          >
            <v-list-item-icon>
            </v-list-item-icon>
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

          <!--   IMPORT     -->
          <v-list-group v-if="isAdmin"
                        prepend-icon="mdi-import"
                        no-action
                        v-model="expandImport"
          >
            <template v-slot:activator>
              <v-list-item-title>{{ $t('book_import.title') }}</v-list-item-title>
            </template>

            <v-list-item :to="{name: 'import-books'}">
              <v-list-item-title>{{ $t('common.books') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'import-readlist'}">
              <v-list-item-title>{{ $t('common.readlist') }}</v-list-item-title>
            </v-list-item>
          </v-list-group>

          <!--   MEDIA MANAGEMENT     -->
          <v-list-group v-if="isAdmin"
                        no-action
                        v-model="expandMediaManagement"
          >
            <template v-slot:prependIcon>
              <v-badge
                dot
                inline
                :value="$store.state.booksToCheck"
                color="accent"
              >
                <v-icon>mdi-book-cog</v-icon>
              </v-badge>
            </template>
            <template v-slot:activator>
              <v-list-item-title>{{ $t('common.media') }}</v-list-item-title>
            </template>

            <v-list-item :to="{name: 'media-analysis'}">
              <v-badge
                dot
                inline
                :value="$store.state.booksToCheck"
                color="accent"
              >
                <v-list-item-title>{{ $t('media_analysis.media_analysis') }}</v-list-item-title>
              </v-badge>
            </v-list-item>

            <v-list-item :to="{name: 'missing-posters'}">
              <v-list-item-title>{{ $t('missing_posters.title') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'duplicate-files'}">
              <v-list-item-title>{{ $t('duplicates.title') }}</v-list-item-title>
            </v-list-item>

            <v-list-group no-action
                          sub-group
                          v-model="expandDuplicatePages"
            >
              <template v-slot:activator>
                <v-list-item-title>{{ $t('duplicate_pages.title') }}</v-list-item-title>
              </template>

              <v-list-item :to="{name: 'settings-duplicate-pages-known'}">
                <v-list-item-title>{{ $t('duplicate_pages.known') }}</v-list-item-title>
              </v-list-item>

              <v-list-item :to="{name: 'settings-duplicate-pages-unknown'}">
                <v-list-item-title>{{ $t('duplicate_pages.new') }}</v-list-item-title>
              </v-list-item>
            </v-list-group>
          </v-list-group>

          <!--   SETTINGS     -->
          <v-list-group v-if="isAdmin"
                        no-action
                        v-model="expandSettings"
          >
            <template v-slot:prependIcon>
              <v-badge
                dot
                inline
                :value="$store.getters.getUnreadAnnouncementsCount()"
                color="info"
              >
                <v-icon>mdi-cog</v-icon>
              </v-badge>
            </template>
            <template v-slot:activator>
              <v-list-item-title>{{ $t('server.tab_title') }}</v-list-item-title>
            </template>

            <v-list-item :to="{name: 'settings-users'}">
              <v-list-item-title>{{ $t('users.users') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'settings-server'}">
              <v-list-item-title>{{ $t('common.settings') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'settings-ui'}">
              <v-list-item-title>{{ $t('common.ui') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'metrics'}">
              <v-list-item-title>{{ $t('metrics.title') }}</v-list-item-title>
            </v-list-item>
          </v-list-group>

          <!--   ACCOUNT     -->
          <v-list-group prepend-icon="mdi-account"
                        no-action
                        v-model="expandAccount"
          >
            <template v-slot:activator>
              <v-list-item-title>{{ $t('account_settings.my_account') }}</v-list-item-title>
            </template>

            <v-list-item :to="{name: 'account-me'}">
              <v-list-item-title>{{ $t('account_settings.details') }}</v-list-item-title>
            </v-list-item>

            <v-list-item :to="{name: 'account-activity'}">
              <v-list-item-title>{{ $t('users.authentication_activity') }}</v-list-item-title>
            </v-list-item>
          </v-list-group>

          <v-list-item @click="logoutConfirmDialog = true">
            <v-list-item-icon>
              <v-icon>mdi-power</v-icon>
            </v-list-item-icon>
            <v-list-item-content>
              <v-list-item-title>{{ $t('navigation.logout') }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </v-list>

        <v-divider/>

        <v-list dense class="app-shell__preferences mt-2">
          <v-list-item>
            <v-list-item-icon>
              <v-icon>{{ themeIcon }}</v-icon>
            </v-list-item-icon>
            <v-select
              class="py-2"
              dense
              v-model="theme"
              :items="themes"
              :label="$t('home.theme')"
            ></v-select>
          </v-list-item>

          <v-list-item>
            <v-list-item-icon>
              <v-icon>mdi-translate</v-icon>
            </v-list-item-icon>
            <v-select
              dense
              class="py-2"
              v-model="locale"
              :items="locales"
              :label="$t('home.translation')"
            >
            </v-select>
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
import LogoMark from '@/components/LogoMark.vue'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import ReusableDialogs from '@/components/ReusableDialogs.vue'
import LibraryActionsMenu from '@/components/menus/LibraryActionsMenu.vue'
import SearchBox from '@/components/SearchBox.vue'
import {resolveShellShortcut} from '@/functions/shell-shortcuts'
import {Theme} from '@/types/themes'
import {CLIENT_SETTING} from '@/types/tales-clientsettings'
import Vue from 'vue'
import {LIBRARIES_ALL} from '@/types/library'
import ToasterNotification from '@/components/ToasterNotification.vue'
import {MediaStatus} from '@/types/enum-books'
import {LibraryDto} from '@/types/tales-libraries'
import {BookSearch, SearchConditionAnyOfBook, SearchConditionMediaStatus, SearchOperatorIs} from '@/types/tales-search'
import LibrariesActionsMenu from '@/components/menus/LibrariesActionsMenu.vue'
import ReorderLibraries from '@/components/ReorderLibraries.vue'

export default Vue.extend({
  name: 'HomeView',
  components: {
    LogoMark,
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
      locales: ['ko', 'en', 'ja', 'zh-Hans']
        .filter((x: string) => this.$i18n.availableLocales.includes(x))
        .map((x: string) => ({text: this.$i18n.t('common.locale_name', x), value: x})),
      expandSettings: false,
      expandDuplicatePages: false,
      expandMediaManagement: false,
      expandImport: false,
      expandAccount: false,
      expandUnpinned: false,
      showReorder: false,
      logoutConfirmDialog: false,
    }
  },
  async created() {
    if (this.isAdmin) {
      this.$actuator.getInfo()
        .then(x => this.$store.commit('setActuatorInfo', x))
      this.$talesBooks.getBooksList({
        condition: new SearchConditionAnyOfBook([
          new SearchConditionMediaStatus(new SearchOperatorIs(MediaStatus.ERROR)),
          new SearchConditionMediaStatus(new SearchOperatorIs(MediaStatus.UNSUPPORTED)),
        ]),
      } as BookSearch, {size: 0} as PageRequest)
        .then(x => this.$store.commit('setBooksToCheck', x.totalElements))
      this.$talesAnnouncements.getAnnouncements()
        .then(x => this.$store.commit('setAnnouncements', x))
      this.$talesReleases.getReleases()
        .then(x => this.$store.commit('setReleases', x))
    }
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
    taskCount(): number {
      return this.$store.state.talesSse.taskCount
    },
    taskCountByType(): { [key: string]: number } {
      return this.$store.state.talesSse.taskCountByType
    },
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
    themes(): object[] {
      return [
        {text: this.$i18n.t(Theme.LIGHT), value: Theme.LIGHT},
        {text: this.$i18n.t(Theme.DARK), value: Theme.DARK},
        {text: this.$i18n.t(Theme.SYSTEM), value: Theme.SYSTEM},
      ]
    },
    themeIcon(): string {
      switch (this.theme) {
        case Theme.LIGHT:
          return 'mdi-brightness-7'
        case Theme.DARK:
          return 'mdi-brightness-3'
        case Theme.SYSTEM:
          return 'mdi-brightness-auto'
      }
      return ''
    },

    theme: {
      get: function (): Theme {
        return this.$store.state.persistedState.theme
      },
      set: function (theme: Theme): void {
        if (Object.values(Theme).includes(theme)) {
          this.$store.commit('setTheme', theme)
          this.saveUserPreference(CLIENT_SETTING.WEBUI_THEME, theme)
        }
      },
    },
    locale: {
      get: function (): string {
        return this.$i18n.locale
      },
      set: function (locale: string): void {
        if (this.$i18n.availableLocales.includes(locale)) {
          this.$store.commit('setLocale', locale)
          this.saveUserPreference(CLIENT_SETTING.WEBUI_LOCALE, locale)
        }
      },
    },
  },
  methods: {
    // The setting is applied locally first; persisting it is what makes it follow
    // the account to the next sign-in, so a failure here is not worth blocking on.
    saveUserPreference(key: CLIENT_SETTING, value: string) {
      this.$store.dispatch('saveUserPreference', {key: key, value: value})
        .catch(e => this.$debug(e.message))
    },
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
      this.expandSettings = to.path.includes('/settings/')
      this.expandMediaManagement = to.path.includes('/media-management/')
      this.expandImport = to.path.includes('/import/')
      this.expandDuplicatePages = to.path.includes('/duplicate-pages/')
      this.expandAccount = to.path.includes('/account/')
      if (this.librariesUnpinned.some(it => it.id === to.params.libraryId)) this.expandUnpinned = true
      else if (this.librariesPinned.some(it => it.id === to.params.libraryId)) this.expandUnpinned = false
    },
    toggleDrawer() {
      this.drawerVisible = !this.drawerVisible
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
