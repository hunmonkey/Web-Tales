import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import ShelfViewStateService from '@/shelfview/services/shelfview-state.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance },
  ) {
    Vue.prototype.$shelfViewState = new ShelfViewStateService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $shelfViewState: ShelfViewStateService;
  }
}
