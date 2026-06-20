import _Vue from 'vue'
import TalesSseService from '@/services/tales-sse.service'
import {Module} from 'vuex'

const vuexModule: Module<any, any> = {
  state: {
    taskCount: 0,
    taskCountByType: {} as { [key: string]: number },
  },
  mutations: {
    setTaskCount (state, event) {
      state.taskCount = event.count
      state.taskCountByType = event.countByType
    },
  },
}

export default {
  install(
    Vue: typeof _Vue,
    {eventHub, store}: { eventHub: _Vue, store: any },
  ) {
    store.registerModule('talesSse', vuexModule)

    Vue.prototype.$talesSse = new TalesSseService(eventHub, store)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $talesSse: TalesSseService;
  }
}
