import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import ShelfViewLibraryLocksService from '@/shelfview/services/shelfview-library-locks.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance },
  ) {
    Vue.prototype.$shelfViewLibraryLocks = new ShelfViewLibraryLocksService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $shelfViewLibraryLocks: ShelfViewLibraryLocksService;
  }
}
