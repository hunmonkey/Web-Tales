import { AxiosInstance } from 'axios'
import _Vue from 'vue'
import TalesReadListsService from '@/services/tales-readlists.service'

export default {
  install (
    Vue: typeof _Vue,
    { http }: { http: AxiosInstance }) {
    Vue.prototype.$talesReadLists = new TalesReadListsService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesReadLists: TalesReadListsService;
  }
}
