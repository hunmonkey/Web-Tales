import _, {LoDashStatic} from 'lodash'
import Vue from 'vue'
// @ts-ignore
import * as lineClamp from 'vue-line-clamp'
import Vuelidate from 'vuelidate'
import {sync} from 'vuex-router-sync'
import App from './App.vue'
import actuator from './plugins/actuator.plugin'
import httpPlugin from './plugins/http.plugin'
import talesBooks from './plugins/tales-books.plugin'
import talesClaim from './plugins/tales-claim.plugin'
import talesFileSystem from './plugins/tales-filesystem.plugin'
import talesLibraries from './plugins/tales-libraries.plugin'
import talesReferential from './plugins/tales-referential.plugin'
import talesSeries from './plugins/tales-series.plugin'
import talesUsers from './plugins/tales-users.plugin'
import talesSse from './plugins/tales-sse.plugin'
import talesTasks from './plugins/tales-tasks.plugin'
import talesOauth2 from './plugins/tales-oauth2.plugin'
import talesLogin from './plugins/tales-login.plugin'
import talesHistory from './plugins/tales-history.plugin'
import talesSettings from './plugins/tales-settings.plugin'
import talesFonts from './plugins/tales-fonts.plugin'
import shelfViewManifest from './shelfview/plugins/shelfview-manifest.plugin'
import shelfViewState from './shelfview/plugins/shelfview-state.plugin'
import shelfViewText from './shelfview/plugins/shelfview-text.plugin'
import {registerShelfViewServiceWorker} from './shelfview/functions/offline-pwa'
import vuetify from './plugins/vuetify'
import logger from './plugins/logger.plugin'
import './public-path'
import router from './router'
import store from './store'
import i18n from './i18n'

Vue.prototype.$_ = _
Vue.prototype.$eventHub = new Vue()

Vue.use(Vuelidate)
Vue.use(lineClamp)

Vue.use(httpPlugin)
Vue.use(logger)
Vue.use(talesSettings, {store: store, http: Vue.prototype.$http})
Vue.use(talesFileSystem, {http: Vue.prototype.$http})
Vue.use(talesSeries, {http: Vue.prototype.$http})
Vue.use(talesBooks, {http: Vue.prototype.$http})
Vue.use(talesReferential, {http: Vue.prototype.$http})
Vue.use(talesClaim, {http: Vue.prototype.$http})
Vue.use(talesUsers, {store: store, http: Vue.prototype.$http})
Vue.use(talesLibraries, {store: store, http: Vue.prototype.$http})
Vue.use(talesSse, {eventHub: Vue.prototype.$eventHub, store: store})
Vue.use(actuator, {http: Vue.prototype.$http})
Vue.use(talesTasks, {http: Vue.prototype.$http})
Vue.use(talesOauth2, {http: Vue.prototype.$http})
Vue.use(talesLogin, {http: Vue.prototype.$http})
Vue.use(talesHistory, {http: Vue.prototype.$http})
Vue.use(talesFonts, {http: Vue.prototype.$http})
Vue.use(shelfViewManifest, {http: Vue.prototype.$http})
Vue.use(shelfViewState, {http: Vue.prototype.$http})
Vue.use(shelfViewText, {http: Vue.prototype.$http})

Vue.config.productionTip = false

sync(store, router)

new Vue({
  router,
  store,
  vuetify,
  i18n,
  render: h => h(App),
}).$mount('#app')

registerShelfViewServiceWorker()

declare module 'vue/types/vue' {
  interface Vue {
    $_: LoDashStatic;
    $eventHub: Vue;
  }
}

declare global {
  interface Window {
    resourceBaseUrl: string
  }
}
