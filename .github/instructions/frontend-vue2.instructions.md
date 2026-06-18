---
name: ShelfView Frontend Vue 2
description: Vue 2, TypeScript, Vuex, Vuetify and reader UI rules
applyTo: "komga-webui/src/**/*.ts,komga-webui/src/**/*.vue,komga-webui/src/**/*.scss"
---

# Frontend rules

- This is Vue 2.6, TypeScript 4.9, Vuex 3, Vuetify 2. Do not introduce Vue 3-only patterns.
- Follow the existing component, service, Vuex, i18n, router, and test conventions.
- Keep ShelfView code isolated under `komga-webui/src/shelfview` where practical.
- Components do not call HTTP directly; use the existing service/API layer.
- UI consumes the unified Manifest and a reader adapter; do not scatter extension checks across components.
- Apply visual filters only to page content, never the toolbar or dialogs.
- Render TXT as text nodes. Do not use untrusted `v-html`.
- Keep keyboard focus, reduced motion, touch behavior, and mobile orientation in tests or acceptance notes.
- Cancel stale page requests and release observers/listeners on destroy.
- Do not download an entire large archive into browser memory for normal server reading.
