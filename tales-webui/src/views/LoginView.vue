<template>
  <main class="auth-shell">
    <section class="auth-shell__panel">
      <div class="auth-shell__logo-row">
        <logo-mark class="auth-shell__logo" :max-width="220"/>
      </div>

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
            <!-- Without type/inputmode an Android keyboard opens without an "@"
                 key and auto-capitalises the first letter, so every sign-in
                 starts with a symbol-layer detour and a typo to undo. -->
            <v-text-field
              v-model="form.login"
              :label="$t('common.email')"
              :error-messages="getErrors('login')"
              :autocomplete="hideLogin ? '' : 'username'"
              type="email"
              inputmode="email"
              autocapitalize="none"
              spellcheck="false"
              outlined
              autofocus
              @blur="$v.form.login.$touch()"
            />

            <!-- Same reveal control the password-change dialog already uses. On
                 a soft keyboard there is no other way to check what was typed. -->
            <v-text-field
              v-model="form.password"
              :label="$t('common.password')"
              :error-messages="getErrors('password')"
              :type="showPassword ? 'text' : 'password'"
              :append-icon="showPassword ? 'mdi-eye-outline' : 'mdi-eye-off-outline'"
              :autocomplete="hideLogin ? '' : 'current-password'"
              outlined
              @click:append="showPassword = !showPassword"
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
            <v-icon left>{{ oauth2ProviderIcon(provider.registrationId) }}</v-icon>
            Sign in with {{ provider.name }}
          </v-btn>
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
import {oauth2ProviderIcon, socialButtons} from '@/types/social'
import {convertErrorCodes} from '@/functions/error-codes'
import {CLIENT_SETTING} from '@/types/tales-clientsettings'

const LOGIN_LOCALE = 'ko'

export default Vue.extend({
  name: 'LoginView',
  components: {LogoMark},
  data: function () {
    return {
      urls,
      socialButtons,
      oauth2ProviderIcon,
      form: {
        login: '',
        password: '',
      },
      showPassword: false,
      snackbar: false,
      snackText: '',
      unclaimed: false,
      oauth2Providers: [] as OAuth2ClientDto[],
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
    rememberMe: {
      get: function (): boolean {
        return this.$store.state.persistedState.rememberMe
      },
      set: function (value: boolean): void {
        this.$store.commit('setRememberMe', value)
      },
    },
  },
  async mounted() {
    this.$store.commit('setLocale', LOGIN_LOCALE)
    this.$store.commit('setTheme', Theme.DARK)

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
        // Only the credentials call is wrapped here. The dispatches after it run
        // once the account is already signed in, so reporting their failures as
        // a wrong password would send the reader off retyping a correct one.
        try {
          await this.$store.dispatch(
            'getMeWithAuth',
            {
              login: this.form.login,
              password: this.form.password,
              rememberMe: this.rememberMe,
            })
        } catch (e) {
          this.showSnack(this.$t(e?.status === 401
            ? 'login.error_invalid_credentials'
            : 'login.error_server_unreachable').toString())
          return
        }

        try {
          await this.$store.dispatch('getLibraries')
          await this.$store.dispatch('getClientSettingsGlobal')
          await this.$store.dispatch('getClientSettingsUser')
          await this.$store.dispatch('applyUserPreferences')

          if (this.$route.query.redirect) {
            await this.$router.push({path: this.$route.query.redirect.toString()})
          } else {
            await this.$router.push({name: 'home'})
          }
        } catch (e) {
          this.showSnack(this.$t('login.error_after_sign_in').toString())
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
/* LogoMark 루트는 inline-flex라 margin auto가 먹지 않는다. 래퍼로 가운데 정렬한다 */
.auth-shell__logo-row {
  display: flex;
  justify-content: center;
}
</style>
