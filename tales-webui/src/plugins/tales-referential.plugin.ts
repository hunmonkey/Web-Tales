import TalesReferentialService from '@/services/tales-referential.service'
import { AxiosInstance } from 'axios'
import _Vue from 'vue'

export default {
  install (
    Vue: typeof _Vue,
    { http }: { http: AxiosInstance }) {
    Vue.prototype.$talesReferential = new TalesReferentialService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesReferential: TalesReferentialService;
  }
}
