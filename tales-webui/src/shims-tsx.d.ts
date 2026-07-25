import Vue, { VNode } from 'vue'

declare global {
  namespace JSX {
    // tslint:disable no-empty-interface
    interface Element extends VNode {
    }

    // tslint:disable no-empty-interface
    interface ElementClass extends Vue {
    }

    // Vue 2.7 already declares JSX.IntrinsicElements with an arbitrary-element
    // index signature in vue/types/jsx.d.ts, so redeclaring it here is a
    // duplicate string index signature (TS2374).
  }
}
