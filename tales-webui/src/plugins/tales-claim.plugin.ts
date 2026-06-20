import TalesClaimService from '@/services/tales-claim.service'
import { AxiosInstance } from 'axios'
import _Vue from 'vue'

export default {
  install (
    Vue: typeof _Vue,
    { http }: { http: AxiosInstance }) {
    Vue.prototype.$talesClaim = new TalesClaimService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesClaim: TalesClaimService;
  }
}
