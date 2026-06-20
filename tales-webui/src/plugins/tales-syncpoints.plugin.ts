import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import TalesSyncPointsService from '@/services/tales-syncpoints.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance }) {
    Vue.prototype.$talesSyncPoints = new TalesSyncPointsService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesSyncPoints: TalesSyncPointsService;
  }
}
