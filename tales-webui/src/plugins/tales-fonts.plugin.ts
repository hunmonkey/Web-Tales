import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import TalesFontsService from '@/services/tales-fonts.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance }) {
    Vue.prototype.$talesFonts = new TalesFontsService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesFonts: TalesFontsService;
  }
}
