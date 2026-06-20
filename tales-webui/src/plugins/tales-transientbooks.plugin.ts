import TalesTransientBooksService from '@/services/tales-transientbooks.service'
import {AxiosInstance} from 'axios'
import _Vue from 'vue'

let service: TalesTransientBooksService

export default {
  install (
    Vue: typeof _Vue,
    { http }: { http: AxiosInstance }) {
    service = new TalesTransientBooksService(http)
    Vue.prototype.$talesTransientBooks = service
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesTransientBooks: TalesTransientBooksService;
  }
}
