import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import ShelfViewManifestService from '@/shelfview/services/shelfview-manifest.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance },
  ) {
    Vue.prototype.$shelfViewManifest = new ShelfViewManifestService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $shelfViewManifest: ShelfViewManifestService;
  }
}
