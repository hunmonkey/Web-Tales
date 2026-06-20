import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import TalesMetricsService from '@/services/tales-metrics.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance }) {
    Vue.prototype.$talesMetrics = new TalesMetricsService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesMetrics: TalesMetricsService;
  }
}
