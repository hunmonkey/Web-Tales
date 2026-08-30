<template>
  <v-container fluid class="pa-6">
    <v-row>
      <v-col class="text-h5">{{ $t('common.settings') }}</v-col>
    </v-row>

    <v-tabs v-model="tab">
      <v-tab>{{ $t('common.ui') }}</v-tab>
      <v-tab>{{ $t('account_settings.my_account') }}</v-tab>
      <v-tab v-if="isAdmin">{{ $t('server.tab_title') }}</v-tab>
      <v-tab v-if="isAdmin">{{ $t('users.users') }}</v-tab>
    </v-tabs>

    <v-tabs-items v-model="tab">
      <v-tab-item>
        <v-container fluid class="pa-6">
          <v-row>
            <v-col cols="12" md="8" lg="6" xl="4">
              <v-select
                v-model="theme"
                :items="themes"
                :label="$t('home.theme')"
              />

              <v-select
                v-model="locale"
                :items="locales"
                :label="$t('home.translation')"
              />

              <!-- Moved here from the removed 서재 sidebar row (SV-1294). -->
              <v-select
                v-model="librarySort"
                :items="librarySorts"
                :label="$t('libraries_sort.title')"
              />

              <v-btn v-if="!showReorder"
                     outlined
                     data-testid="reorder-libraries"
                     @click="showReorder = true"
              >{{ $t('common.reorder') }}
              </v-btn>
              <!-- Only mounted on demand: opening the panel switches the sort
                   to MANUAL, which must not happen just by visiting settings. -->
              <reorder-libraries v-if="showReorder" @dismiss="showReorder = false"/>
            </v-col>
          </v-row>
        </v-container>
      </v-tab-item>

      <v-tab-item>
        <account-settings/>
      </v-tab-item>

      <v-tab-item v-if="isAdmin">
        <ui-settings/>
        <server-settings/>
        <server-management/>
      </v-tab-item>

      <v-tab-item v-if="isAdmin">
        <settings-users/>
      </v-tab-item>
    </v-tabs-items>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import AccountSettings from '@/views/AccountView.vue'
// Registered as `UiSettings`, not `UISettings`: Vue hyphenates the latter to
// `u-i-settings`, so `<ui-settings/>` in the template would never match it.
import UiSettings from '@/views/UISettings.vue'
import ServerSettings from '@/views/ServerSettings.vue'
import ServerManagement from '@/views/ServerManagement.vue'
import SettingsUsers from '@/views/SettingsUsers.vue'
import ReorderLibraries from '@/components/ReorderLibraries.vue'
import {Theme} from '@/types/themes'
import {CLIENT_SETTING, LIBRARY_SORTS, LibrarySort} from '@/types/tales-clientsettings'

export default Vue.extend({
  name: 'PreferencesView',
  components: {AccountSettings, UiSettings, ServerSettings, ServerManagement, SettingsUsers, ReorderLibraries},
  data: function () {
    return {
      tab: 0,
      showReorder: false,
      locales: ['ko', 'en', 'ja', 'zh-Hans']
        .filter((x: string) => this.$i18n.availableLocales.includes(x))
        .map((x: string) => ({text: this.$i18n.t('common.locale_name', x), value: x})),
    }
  },
  // The old /settings/users address redirects here with ?tab=users, so
  // bookmarks and the add-user dialog's way back land on the users tab. The
  // add route must select it too: its dialog renders through the router-view
  // inside UsersList, which only exists while the users tab is active.
  created() {
    if ((this.$route.query.tab === 'users' || this.$route.name === 'settings-users-add') && this.isAdmin) this.tab = 3
  },
  computed: {
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
    librarySorts(): object[] {
      return LIBRARY_SORTS.map(value => ({
        value,
        text: this.$t(`libraries_sort.${value.toLowerCase()}`) as string,
      }))
    },
    librarySort: {
      get: function (): LibrarySort {
        return this.$store.getters.getClientSettingsLibrariesSort
      },
      set: function (sort: LibrarySort): void {
        if (sort === this.librarySort) return
        this.saveUserPreference(CLIENT_SETTING.WEBUI_LIBRARIES_SORT, sort)
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
  },
})
</script>
