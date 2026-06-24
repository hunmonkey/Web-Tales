<template>
  <svg
    class="app-logo"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 72 56"
    fill="none"
    role="img"
    :aria-label="ariaLabel"
    :style="rootStyle"
  >
    <path d="M61 3 L63 8 L68 10 L63 12 L61 17 L59 12 L54 10 L59 8 Z" fill="#e0623d"/>
    <path d="M53 0.5 L54.3 3.7 L57.5 5 L54.3 6.3 L53 9.5 L51.7 6.3 L48.5 5 L51.7 3.7 Z" fill="#e0623d"/>
    <path d="M67 16 L68.2 18.8 L71 20 L68.2 21.2 L67 24 L65.8 21.2 L63 20 L65.8 18.8 Z" fill="#e0623d"/>
    <path
      d="M54.2 20.1 L48.3 9.7 L38 3.8 L26 3.8 L15.7 9.7 L9.8 20.1 L9.8 31.9 L15.7 42.3 L26 48.2 L38 48.2 L48.3 42.3 L54.2 31.9 Z"
      :fill="bodyColor"
    />
    <g stroke="#f6f3ee" stroke-width="2.4" fill="none" stroke-linecap="round" stroke-linejoin="round">
      <path d="M32 20 C28 17.5 22 17.5 18 18.6 L18 31 C22 29.9 28 29.9 32 32.4 C36 29.9 42 29.9 46 31 L46 18.6 C42 17.5 36 17.5 32 20 Z"/>
      <path d="M32 20 L32 32.4 M18 31 L18 35 C22 33.9 28 33.9 32 36.4 C36 33.9 42 33.9 46 35 L46 31"/>
    </g>
  </svg>
</template>

<script lang="ts">
import Vue from 'vue'

// Inline logo so the brick-coloured body can be lightened in dark mode, where
// the original #9e3b22 sits too close to the near-black background.
export default Vue.extend({
  name: 'LogoMark',
  props: {
    maxWidth: {
      type: [Number, String],
      default: undefined,
    },
    ariaLabel: {
      type: String,
      default: 'Tales',
    },
  },
  computed: {
    bodyColor(): string {
      return this.$vuetify.theme.dark ? '#c0512e' : '#9e3b22'
    },
    rootStyle(): Record<string, string> {
      const style: Record<string, string> = {
        width: '100%',
        height: 'auto',
        display: 'block',
      }
      if (this.maxWidth !== undefined) {
        style.maxWidth = typeof this.maxWidth === 'number' ? `${this.maxWidth}px` : this.maxWidth
      }
      return style
    },
  },
})
</script>
