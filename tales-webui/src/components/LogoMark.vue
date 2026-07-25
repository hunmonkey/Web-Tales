<template>
  <div
    class="logo-mark"
    :class="{'logo-mark--symbol-only': symbolOnly}"
    role="img"
    :aria-label="ariaLabel"
    :style="rootStyle"
  >
    <svg
      class="logo-mark__symbol"
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 64 64"
      fill="none"
      aria-hidden="true"
    >
      <path
        class="logo-mark__book"
        d="M8 12C18 12 27 16 31 24C32.2 26.4 31.4 29.1 29 31.6C26.4 34.3 25 36.7 25 40V54C20 50.2 14.3 48.4 8 49V12Z"
      />
      <path
        class="logo-mark__book"
        d="M56 12C46 12 37 16 33 24C31.8 26.4 32.6 29.1 35 31.6C37.6 34.3 39 36.7 39 40V54C44 50.2 49.7 48.4 56 49V12Z"
      />
      <path
        class="logo-mark__page-turn"
        d="M29 37C33 39.2 35 42.2 35 46V54H29V37Z"
      />
    </svg>
    <span v-if="!symbolOnly" class="logo-mark__wordmark" aria-hidden="true">Tales</span>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'

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
    symbolOnly: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    rootStyle(): Record<string, string> {
      const style: Record<string, string> = {
        width: '100%',
      }
      if (this.maxWidth !== undefined) {
        style.maxWidth = typeof this.maxWidth === 'number' ? `${this.maxWidth}px` : this.maxWidth
      }
      return style
    },
  },
})
</script>

<style scoped>
.logo-mark {
  --tales-logo-ink: #132f4c;
  --tales-logo-accent: #b76e4e;
  align-items: center;
  color: #132f4c;
  display: inline-flex;
  gap: 12px;
}

.theme--dark .logo-mark {
  --tales-logo-ink: #f2ede4;
  --tales-logo-accent: #d99a76;
  color: #f2ede4;
}

.logo-mark__symbol {
  display: block;
  flex: 0 0 auto;
  width: clamp(42px, 30%, 58px);
}

.logo-mark__book {
  fill: var(--tales-logo-ink);
}

.logo-mark__page-turn {
  fill: var(--tales-logo-accent);
}

.logo-mark__wordmark {
  font-family: "Iowan Old Style", "Palatino Linotype", "Book Antiqua", Georgia, serif;
  font-size: clamp(26px, 20%, 34px);
  font-weight: 600;
  letter-spacing: -0.055em;
  line-height: 1;
  white-space: nowrap;
}

.logo-mark--symbol-only {
  display: block;
}

.logo-mark--symbol-only .logo-mark__symbol {
  width: 100%;
}
</style>
