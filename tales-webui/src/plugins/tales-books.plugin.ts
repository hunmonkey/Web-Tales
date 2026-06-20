import TalesBooksService from '@/services/tales-books.service'
import { AxiosInstance } from 'axios'
import _Vue from 'vue'

export default {
  install (
    Vue: typeof _Vue,
    { http }: { http: AxiosInstance }) {
    Vue.prototype.$talesBooks = new TalesBooksService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesBooks: TalesBooksService;
  }
}
