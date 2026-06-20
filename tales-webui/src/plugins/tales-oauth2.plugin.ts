import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import TalesOauth2Service from '@/services/tales-oauth2.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance }) {
    Vue.prototype.$talesOauth2 = new TalesOauth2Service(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesOauth2: TalesOauth2Service;
  }
}
