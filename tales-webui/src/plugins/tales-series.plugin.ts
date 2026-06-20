import TalesSeriesService from '@/services/tales-series.service'
import { AxiosInstance } from 'axios'
import _Vue from 'vue'

export default {
  install (
    Vue: typeof _Vue,
    { http }: { http: AxiosInstance }) {
    Vue.prototype.$talesSeries = new TalesSeriesService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesSeries: TalesSeriesService;
  }
}
