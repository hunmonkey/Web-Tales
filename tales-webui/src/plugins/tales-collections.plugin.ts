import TalesCollectionsService from '@/services/tales-collections.service'
import { AxiosInstance } from 'axios'
import _Vue from 'vue'

export default {
  install (
    Vue: typeof _Vue,
    { http }: { http: AxiosInstance }) {
    Vue.prototype.$talesCollections = new TalesCollectionsService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesCollections: TalesCollectionsService;
  }
}
