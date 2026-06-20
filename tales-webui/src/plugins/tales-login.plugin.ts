import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import TalesLoginService from '@/services/tales-login.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance }) {
    Vue.prototype.$talesLogin = new TalesLoginService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesLogin: TalesLoginService;
  }
}
