import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import TalesAnnouncementsService from '@/services/tales-announcements.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance }) {
    Vue.prototype.$talesAnnouncements = new TalesAnnouncementsService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesAnnouncements: TalesAnnouncementsService;
  }
}
