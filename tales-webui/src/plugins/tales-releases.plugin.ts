import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import TalesAnnouncementsService from '@/services/tales-announcements.service'
import TalesReleasesService from '@/services/tales-releases.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance }) {
    Vue.prototype.$talesReleases = new TalesReleasesService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesReleases: TalesReleasesService;
  }
}
