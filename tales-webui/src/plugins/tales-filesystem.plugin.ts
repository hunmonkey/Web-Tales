import TalesFilesystemService from '@/services/tales-filesystem.service'
import { AxiosInstance } from 'axios'
import _Vue from 'vue'

export default {
  install (
    Vue: typeof _Vue,
    { http }: { http: AxiosInstance }) {
    Vue.prototype.$talesFileSystem = new TalesFilesystemService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesFileSystem: TalesFilesystemService;
  }
}
