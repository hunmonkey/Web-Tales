import '@mdi/font/css/materialdesignicons.css'
import 'typeface-roboto/index.css'
import Vue from 'vue'
import Vuetify from 'vuetify/lib'
import colors from 'vuetify/lib/util/colors'

import {Touch} from 'vuetify/lib/directives'
import i18n from '@/i18n'
import IconFormatLineSpacingDown from '@/components/icons/IconFormatLineSpacingDown.vue'

Vue.use(Vuetify, {
  directives: {
    Touch,
  },
})

export default new Vuetify({
  icons: {
    iconfont: 'mdi',
    values: {
      formatLineSpacingDown: {
        component: IconFormatLineSpacingDown,
      },
    },
  },

  lang: {
    t: (key, ...params) => i18n.t(key, params).toString(),
  },

  theme: {
    options: {
      customProperties: true,
    },
    themes: {
      light: {
        base: '#f3f6f8',
        surface: '#ffffff',
        'surface-variant': '#e8eef3',
        primary: '#22577a',
        secondary: '#b76e4e',
        accent: '#b76e4e',
        muted: '#617084',
        border: '#d9e1e8',
        'contrast-1': '#edf2f5',
        'contrast-light-2': '#536273',
        'diff': colors.green.lighten4,
      },
      dark: {
        base: '#0b111a',
        surface: '#151e2b',
        'surface-variant': '#202c3d',
        primary: '#83c5e8',
        secondary: '#d99a76',
        accent: '#d99a76',
        muted: '#99a7b7',
        border: '#2d3a4b',
        'contrast-1': '#182331',
        'contrast-light-2': '#aab7c5',
        'diff': colors.green.darken4,
      },
    },
  },
})
