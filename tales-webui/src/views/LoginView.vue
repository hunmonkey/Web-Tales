<template>
  <main class="auth-shell">
    <section class="auth-shell__panel">
      <logo-mark class="auth-shell__logo" :max-width="220"/>

      <v-card flat class="auth-card">
        <form novalidate @submit.prevent="performLogin">
          <v-alert
            v-if="unclaimed"
            class="mb-6"
            type="info"
            icon="mdi-account-plus"
            prominent
            text
            v-html="$t('login.unclaimed_html')"
          />

          <div v-if="!hideLogin">
            <v-text-field
              v-model="form.login"
              :label="$t('common.email')"
              :error-messages="getErrors('login')"
              :autocomplete="hideLogin ? '' : 'username'"
              outlined
              autofocus
              @blur="$v.form.login.$touch()"
            />

            <v-text-field
              v-model="form.password"
              :label="$t('common.password')"
              :error-messages="getErrors('password')"
              type="password"
              :autocomplete="hideLogin ? '' : 'current-password'"
              outlined
              @input="$v.form.password.$touch()"
              @blur="$v.form.password.$touch()"
            />

            <v-checkbox
              v-model="rememberMe"
              :label="$t('common.remember-me')"
              hide-details
              class="mt-0 mb-6"
            />

            <v-btn
              block
              large
              color="primary"
              type="submit"
              class="auth-card__primary-action"
              :disabled="unclaimed"
            >
              {{ $t('login.login') }}
            </v-btn>

            <v-btn
              v-if="unclaimed"
              block
              large
              color="primary"
              class="auth-card__primary-action mt-3"
              @click="claim"
            >
              {{ $t('login.create_user_account') }}
            </v-btn>

            <v-divider v-if="oauth2Providers.length" class="my-6"/>
          </div>

          <v-btn
            v-for="provider in oauth2Providers"
            :key="provider.registrationId"
            block
            large
            class="mb-3"
            :disabled="unclaimed"
            :class="$_.get(socialButtons[provider.registrationId.toLowerCase()], 'text') ? `${socialButtons[provider.registrationId.toLowerCase()].text}--text` : undefined"
            :color="$_.get(socialButtons[provider.registrationId.toLowerCase()], 'color')"
            @click="oauth2Login(provider)"
          >
            <v-icon left>mdi-{{ provider.registrationId }}</v-icon>
            Sign in with {{ provider.name }}
          </v-btn>

          <v-row dense class="auth-card__preferences">
            <v-col cols="6">
              <v-select
                v-model="locale"
                :items="locales"
                :label="$t('home.translation')"
                prepend-icon="mdi-translate"
                hide-details
              />
            </v-col>

            <v-col cols="6">
              <v-select
                v-model="theme"
                :items="themes"
                :label="$t('home.theme')"
                :prepend-icon="themeIcon"
                hide-details
              />
            </v-col>
          </v-row>
        </form>
      </v-card>
    </section>

    <v-snackbar
      v-model="snackbar"
      bottom
      color="error"
    >
      {{ snackText }}
      <v-btn
        text
        @click="snackbar = false"
      >{{ $t('common.close') }}
      </v-btn>
    </v-snackbar>
  </main>
</template>

<script lang="ts">
import LogoMark from '@/components/LogoMark.vue'
import Vue from 'vue'
import {email, required} from 'vuelidate/lib/validators'
import {Theme} from '@/types/themes'
import {OAuth2ClientDto} from '@/types/tales-oauth2'
import urls from '@/functions/urls'
import {socialButtons} from '@/types/social'
import {convertErrorCodes} from '@/functions/error-codes'
import {CLIENT_SETTING} from '@/types/tales-clientsettings'

export default Vue.extend({
  name: 'LoginView',
  components: {LogoMark},
  data: function () {
    return {
      urls,
      socialButtons,
      form: {
        login: '',
        password: '',
      },
      snackbar: false,
      snackText: '',
      unclaimed: false,
      oauth2Providers: [] as OAuth2ClientDto[],
      locales: this.$i18n.availableLocales.map((x: any) => ({text: this.$i18n.t('common.locale_name', x), value: x})),
      clientSettings: {} as Record<string, ClientSettingDto>,
    }
  },
  validations: {
    form: {
      login: {required, email},
      password: {required},
    },
  },
  computed: {
    hideLogin(): boolean {
      return !this.unclaimed
        && this.oauth2Providers.length > 0
        && (this.clientSettings[CLIENT_SETTING.WEBUI_OAUTH2_HIDE_LOGIN]?.value === 'true')
    },
    autoOauth2Login(): boolean {
      return !this.unclaimed
        && this.oauth2Providers.length == 1
        && (this.clientSettings[CLIENT_SETTING.WEBUI_OAUTH2_AUTO_LOGIN]?.value === 'true')
        && !this.$route.query.error
        && !this.$route.query.logout
    },
    locale: {
      get: function (): string {
        return this.$i18n.locale
      },
      set: function (locale: string): void {
        if (this.$i18n.availableLocales.includes(locale)) {
          this.$store.commit('setLocale', locale)
        }
      },
    },

    rememberMe: {
      get: function (): boolean {
        return this.$store.state.persistedState.rememberMe
      },
      set: function (value: boolean): void {
        this.$store.commit('setRememberMe', value)
      },
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
        }
      },
    },
  },
  async mounted() {
    this.getClaimStatus()
    this.clientSettings = await this.$talesSettings.getClientSettingsGlobal()
    this.oauth2Providers = await this.$talesOauth2.getProviders()
    if (this.$route.query.error) this.showSnack(convertErrorCodes(this.$route.query.error.toString()))
    if (this.hideLogin && this.autoOauth2Login) this.oauth2Login(this.oauth2Providers[0])
  },
  methods: {
    oauth2Login(provider: OAuth2ClientDto) {
      const url = `${urls.originNoSlash}/oauth2/authorization/${provider.registrationId}`
      const height = 600
      const width = 600
      const y = window.top!.outerHeight / 2 + window.top!.screenY - (height / 2)
      const x = window.top!.outerWidth / 2 + window.top!.screenX - (width / 2)
      window.open(url, 'oauth2Login',
        `toolbar=no,
        location=off,
        status=no,
        menubar=no,
        scrollbars=yes,
        resizable=yes,
        top=${y},
        left=${x},
        width=${height},
        height=${width}`,
      )
    },
    getErrors(fieldName: string): string[] {
      const errors = [] as string[]

      const field = this.$v.form!![fieldName] as any
      if (field && field.$invalid && field.$dirty) {
        if (!field.required) errors.push(this.$t('common.required').toString())
        if (!field.email) errors.push(this.$t('dialog.add_user.field_email_error').toString())
      }
      return errors
    },
    async getClaimStatus() {
      this.unclaimed = !(await this.$talesClaim.getClaimStatus()).isClaimed
    },
    async performLogin() {
      if (this.isUserValid()) {
        try {
          await this.$store.dispatch(
            'getMeWithAuth',
            {
              login: this.form.login,
              password: this.form.password,
              rememberMe: this.rememberMe,
            })

          await this.$store.dispatch('getLibraries')
          await this.$store.dispatch('getClientSettingsGlobal')
          await this.$store.dispatch('getClientSettingsUser')

          if (this.$route.query.redirect) {
            await this.$router.push({path: this.$route.query.redirect.toString()})
          } else {
            await this.$router.push({name: 'home'})
          }
        } catch (e) {
          this.showSnack(e?.message)
        }
      }
    },
    showSnack(message: string) {
      this.snackText = message
      this.snackbar = true
    },
    isUserValid(): boolean {
      this.$v.$touch()
      return !this.$v.$invalid
    },
    async claim() {
      if (this.isUserValid()) {
        try {
          await this.$talesClaim.claimServer({
            email: this.form.login,
            password: this.form.password,
          } as ClaimAdmin)

          await this.performLogin()
        } catch (e) {
          this.showSnack(e.message)
        }
      }
    },
  },
})
</script>

<style scoped>

</style>
