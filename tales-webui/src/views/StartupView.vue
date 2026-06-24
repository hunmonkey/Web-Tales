<template>
  <div class="pa-6 d-flex align-center justify-center" style="min-height: 100vh">
    <v-img src="../assets/logo.png"
           :max-width="logoWidth"
    />
  </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'StartupView',
  computed: {
    logoWidth(): number {
      let l = 100
      switch (this.$vuetify.breakpoint.name) {
        case 'xs':
          l = 100
        case 'sm':
        case 'md':
          l = 200
        case 'lg':
        case 'xl':
        default:
          l = 300
      }
      return l
    },
  },
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
      const target = (redirect && !redirect.startsWith('/startup') && !redirect.startsWith('/login'))
        ? redirect
        : {name: 'home'}
      this.$router.replace(target).catch(() => {})
    } catch (e) {
      this.$router.push({name: 'login', query: {redirect: this.$route.query.redirect}})
    }
  },
})
</script>

<style scoped>

</style>
