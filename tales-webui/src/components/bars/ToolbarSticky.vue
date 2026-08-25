<template>
  <v-toolbar flat
             :color="color"
             class="sticky-bar"
             :elevation="elevation"
  >
    <slot/>
  </v-toolbar>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'ToolbarSticky',
  props: {
    elevation: {
      type: Number,
      default: undefined,
    },
    color: {
      type: String,
      default: 'contrast-1',
    },
  },
})
</script>

<style scoped>
/* SV-1285. The bar used to stick 56px/64px down the viewport, which was the
   height of the app bar it sat under. SV-1201 removed that app bar, so the
   offset only left an empty strip the page scrolled through above the bar. It
   now sticks to the very top and the content stays hidden behind it. */
.sticky-bar {
  position: -webkit-sticky;
  position: sticky;
  top: 0;
  z-index: 2
}
</style>
