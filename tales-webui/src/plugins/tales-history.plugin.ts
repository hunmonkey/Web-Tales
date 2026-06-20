import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import TalesHistoryService from '@/services/tales-history.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance }) {
    Vue.prototype.$talesHistory = new TalesHistoryService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesHistory: TalesHistoryService;
  }
}
