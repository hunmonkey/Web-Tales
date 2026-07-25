<template>
  <div class="tales-empty-state text-center" :style="rootStyle">
    <div v-if="icon !== ''" class="tales-empty-state__icon">
      <v-icon
        :color="iconColor"
        :size="iconSize"
      >
        {{ icon }}
      </v-icon>
    </div>
    <h1 class="text-h5">{{ title }}</h1>
    <p v-if="subTitle" class="text-body-1 text--secondary mt-2">{{ subTitle }}</p>
    <slot/>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'EmptyState',
  props: {
    title: String,
    subTitle: String,
    icon: String,
    iconColor: String,
    iconSize: {
      type: [Number, String],
      default: 52,
    },
    size: {
      type: [Number, String],
      default: 400,
    },
  },
  computed: {
    rootStyle(): Record<string, string> {
      const width = typeof this.size === 'number' ? `${this.size}px` : this.size
      return {maxWidth: width}
    },
  },
})
</script>

<style scoped>

</style>
