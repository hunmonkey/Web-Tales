<template>
  <div>
    <div
      class="shelfview-control-menu__scrim"
      data-testid="shelfview-reader-control-scrim"
      @click="$emit('close')"
    />
    <ul
      class="shelfview-control-menu"
      :style="{ left: left + 'px', top: top + 'px' }"
      role="menu"
      :aria-label="title"
      data-testid="shelfview-reader-control-menu"
    >
      <li
        class="shelfview-control-menu__title"
        role="presentation"
      >{{ title }}</li>
      <li
        v-for="option in options"
        :key="String(option.value)"
        role="none"
      >
        <button
          type="button"
          role="menuitemradio"
          :aria-checked="option.active ? 'true' : 'false'"
          class="shelfview-control-menu__option"
          :class="{ 'shelfview-control-menu__option--active': option.active }"
          :data-testid="`shelfview-reader-${menu}-option-${option.value}`"
          @click="$emit('select', option.value)"
        >
          <v-icon small>{{ option.icon }}</v-icon>
          <span class="shelfview-control-menu__option-label">{{ option.label }}</span>
          <v-icon
            v-if="option.active"
            small
            class="shelfview-control-menu__option-check"
          >mdi-check</v-icon>
        </button>
      </li>
    </ul>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'ShelfViewControlMenu',
  props: {
    menu: {
      type: String,
      required: true,
    },
    title: {
      type: String,
      required: true,
    },
    left: {
      type: Number,
      required: true,
    },
    top: {
      type: Number,
      default: 50,
    },
    options: {
      type: Array,
      required: true,
    },
  },
})
</script>

<style scoped>
.shelfview-control-menu__scrim {
  inset: 0;
  position: fixed;
  z-index: 3;
}

.shelfview-control-menu {
  background: var(--sv-chrome-surface);
  border: 1px solid var(--sv-chrome-border);
  border-radius: var(--tales-radius-md);
  box-shadow: var(--sv-chrome-shadow);
  list-style: none;
  margin: 0;
  /* `vh` is the address-bar-hidden viewport on Chrome for Android, so the cap
     never fired on a phone and long menus ran off the bottom of the screen
     without ever showing a scrollbar. `svh` is the visible height. */
  max-height: calc(100vh - 64px);
  max-height: calc(100svh - 64px);
  min-width: 216px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 8px;
  position: fixed;
  z-index: 4;
}

/* The rows used to touch each other, so a thumb aiming at "fit to screen"
   landed on "fit to width" and silently changed the page scale. */
.shelfview-control-menu li + li {
  margin-top: 4px;
}

.shelfview-control-menu__title {
  color: var(--sv-chrome-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  padding: 4px 10px 8px;
  text-transform: uppercase;
}

.shelfview-control-menu__option {
  align-items: center;
  background: transparent;
  border: none;
  border-radius: var(--tales-radius-sm);
  color: var(--sv-chrome-text);
  cursor: pointer;
  display: flex;
  font-size: 14px;
  gap: 10px;
  min-height: 48px;
  padding: 8px 10px;
  text-align: left;
  width: 100%;
}

.shelfview-control-menu__option .v-icon {
  color: var(--sv-chrome-muted);
}

.shelfview-control-menu__option:hover {
  background: var(--sv-chrome-surface-soft);
}

/* Keyboard focus must stay visible and must not look identical to hover, even on
   the active option, which already carries its own background. */
.shelfview-control-menu__option:focus-visible {
  background: var(--sv-chrome-surface-soft);
  outline: 2px solid var(--sv-chrome-accent);
  outline-offset: -2px;
}

.shelfview-control-menu__option--active {
  background: var(--sv-chrome-accent-soft);
  color: var(--sv-chrome-accent);
}

.shelfview-control-menu__option--active .v-icon {
  color: var(--sv-chrome-accent);
}

.shelfview-control-menu__option-label {
  flex: 1 1 auto;
}

.shelfview-control-menu__option-check {
  flex: 0 0 auto;
}
</style>
