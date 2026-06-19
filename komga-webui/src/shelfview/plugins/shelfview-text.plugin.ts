import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import ShelfViewTextService from '@/shelfview/services/shelfview-text.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance },
  ) {
    Vue.prototype.$shelfViewText = new ShelfViewTextService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $shelfViewText: ShelfViewTextService;
  }
}
