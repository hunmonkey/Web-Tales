<template>
  <div>
    <div
      class="shelfview-control-menu__scrim"
      data-testid="shelfview-reader-control-scrim"
      @click="$emit('close')"
    />
    <ul
      class="shelfview-control-menu"
      :style="{ left: left + 'px' }"
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
  background: #1d232b;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45);
  list-style: none;
  margin: 0;
  max-height: calc(100vh - 64px);
  min-width: 200px;
  overflow-y: auto;
  padding: 6px;
  position: fixed;
  top: 50px;
  z-index: 4;
}

.shelfview-control-menu__title {
  color: #9fb0c3;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  padding: 4px 10px 6px;
  text-transform: uppercase;
}

.shelfview-control-menu__option {
  align-items: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #e6ecf3;
  cursor: pointer;
  display: flex;
  font-size: 14px;
  gap: 8px;
  padding: 8px 10px;
  text-align: left;
  width: 100%;
}

.shelfview-control-menu__option:hover {
  background: rgba(255, 255, 255, 0.08);
}

/* Keyboard focus must stay visible and must not look identical to hover, even on
   the active option, which already carries its own background. */
.shelfview-control-menu__option:focus-visible {
  background: rgba(255, 255, 255, 0.08);
  outline: 2px solid #90caf9;
  outline-offset: -2px;
}

.shelfview-control-menu__option--active {
  background: rgba(144, 202, 249, 0.16);
  color: #90caf9;
}

.shelfview-control-menu__option-label {
  flex: 1 1 auto;
}

.shelfview-control-menu__option-check {
  flex: 0 0 auto;
}
</style>
