<template>
  <main class="startup-shell">
    <logo-mark :max-width="220"/>
    <v-progress-linear
      class="startup-shell__progress"
      color="primary"
      height="4"
      indeterminate
      rounded
    />
  </main>
</template>

<script lang="ts">
import LogoMark from '@/components/LogoMark.vue'
import Vue from 'vue'

export default Vue.extend({
  name: 'StartupView',
  components: {LogoMark},
  async mounted() {
    try {
      if (this.$route.query.xAuthToken) {
        try {
          await this.$talesLogin.setCookie(this.$route.query.xAuthToken.toString())
        } catch (e) {
          this.$debug(e.message)
        }
      }

      await this.$store.dispatch('getMe')
      await this.$store.dispatch('getLibraries')
      await this.$store.dispatch('getClientSettingsGlobal')
      await this.$store.dispatch('getClientSettingsUser')
      const redirect = this.$route.query.redirect?.toString()
      const isSafeRedirect = !!redirect &&
        redirect.startsWith('/') &&
        !redirect.startsWith('//') &&
        !redirect.startsWith('/startup') &&
        !redirect.startsWith('/login')
      const target = isSafeRedirect ? redirect : {name: 'home'}
      this.$router.replace(target).catch(() => {})
    } catch (e) {
      this.$router.push({name: 'login', query: {redirect: this.$route.query.redirect}})
    }
  },
})
</script>

<style scoped>

</style>
