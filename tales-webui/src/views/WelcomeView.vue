<template>
  <main class="welcome-shell">
    <section class="welcome-shell__panel text-center">
      <logo-mark class="welcome-shell__logo" :max-width="240"/>
      <v-card flat class="welcome-shell__card">
        <h1 class="text-h5 mt-4">{{ $t('welcome.welcome_message') }}</h1>
        <p class="text-body-1 text--secondary mt-3 mb-7">{{ $t('welcome.no_libraries_yet') }}</p>
        <v-btn v-if="isAdmin" color="primary" large @click="addLibrary">
          <v-icon left>mdi-bookshelf</v-icon>
          {{ $t('welcome.add_library') }}
        </v-btn>
      </v-card>
    </section>
  </main>
</template>

<script lang="ts">
import LogoMark from '@/components/LogoMark.vue'
import Vue from 'vue'

export default Vue.extend({
  name: 'WelcomeView',
  components: {LogoMark},
  computed: {
    isAdmin (): boolean {
      return this.$store.getters.meAdmin
    },
  },
  mounted () {
    if (this.$store.state.talesLibraries.libraries.length !== 0) {
      this.$router.push({ name: 'dashboard' })
    }
  },
  watch: {
    '$store.state.talesLibraries.libraries': {
      handler(val){
        if(val.length !== 0) this.$router.push({name: 'dashboard'})
      },
    },
  },
  methods: {
    addLibrary () {
      this.$store.dispatch('dialogAddLibrary')
    },
  },
})
</script>

<style scoped>

</style>
