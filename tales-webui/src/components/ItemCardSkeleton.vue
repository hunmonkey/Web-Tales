<template>
  <div
    class="item-card-skeleton"
    :style="{width: `${width}px`}"
    aria-hidden="true"
    data-testid="item-card-skeleton"
  >
    <div class="item-card-skeleton__poster"/>
    <div class="item-card-skeleton__line item-card-skeleton__line--title"/>
    <div class="item-card-skeleton__line"/>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'ItemCardSkeleton',
  props: {
    width: {
      type: Number,
      default: 150,
    },
  },
})
</script>

<style scoped>
/* Mirrors the ItemCard surface: same radius, same 0.7071 poster ratio and the
   same two description lines, so the real card drops into the same box instead
   of pushing the page around when it arrives. */
.item-card-skeleton {
  background: var(--tales-surface);
  border: 1px solid var(--tales-border);
  border-radius: var(--tales-radius-md);
  overflow: hidden;
  padding-bottom: 12px;
}

.item-card-skeleton__poster {
  aspect-ratio: 0.7071;
  width: 100%;
}

.item-card-skeleton__line {
  border-radius: 4px;
  height: 12px;
  margin: 10px 12px 0;
}

.item-card-skeleton__line--title {
  height: 14px;
  margin-top: 14px;
}

.item-card-skeleton__poster,
.item-card-skeleton__line {
  animation: item-card-skeleton-pulse 1.4s ease-in-out infinite;
  background: var(--tales-surface-soft);
}

.item-card-skeleton__line:last-child {
  width: 55%;
}

@keyframes item-card-skeleton-pulse {
  0%, 100% {
    opacity: 1;
  }

  50% {
    opacity: 0.55;
  }
}

@media (prefers-reduced-motion: reduce) {
  .item-card-skeleton__poster,
  .item-card-skeleton__line {
    animation: none;
  }
}
</style>
