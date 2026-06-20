import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import TalesTasksService from '@/services/tales-tasks.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance }) {
    Vue.prototype.$talesTasks = new TalesTasksService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesTasks: TalesTasksService;
  }
}
