import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import TalesPageHashesService from '@/services/tales-pagehashes.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance }) {
    Vue.prototype.$talesPageHashes = new TalesPageHashesService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesPageHashes: TalesPageHashesService;
  }
}
