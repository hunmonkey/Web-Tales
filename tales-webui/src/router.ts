import urls from '@/functions/urls'
import Vue from 'vue'
import Router from 'vue-router'
import store from './store'
import {LIBRARIES_ALL, LIBRARY_ROUTE} from '@/types/library'
import {isShelfViewReaderEnabled} from '@/shelfview/feature-flags'

const qs = require('qs')

Vue.use(Router)

const lStore = store as any

const adminGuard = (to: any, from: any, next: any) => {
  if (!lStore.getters.meAdmin) next({name: 'home'})
  else next()
}

const noLibraryGuard = (to: any, from: any, next: any) => {
  if (lStore.state.talesLibraries.libraries.length === 0) {
    next({name: 'welcome'})
  } else next()
}

const noLibraryNorPinGuard = (to: any, from: any, next: any) => {
  if (lStore.state.talesLibraries.libraries.length === 0) {
    next({name: 'welcome'})
  } else if (lStore.getters.getLibrariesPinned.length === 0) {
    next({name: 'no-pins'})
  } else next()
}

const shelfViewReaderGuard = (to: any, from: any, next: any) => {
  if (isShelfViewReaderEnabled()) {
    next()
  } else {
    next({name: 'read-book', params: {bookId: to.params.bookId}, query: to.query})
  }
}

const getLibraryRoute = (libraryId: string) => {
  switch ((lStore.getters.getLibraryRoute(libraryId) as LIBRARY_ROUTE)) {
    // COLLECTIONS is deliberately absent (SV-1204d): the enum value stays so a
    // stored preference still parses, but it now falls through to the default.
    case LIBRARY_ROUTE.BROWSE:
      return 'browse-libraries'
    case LIBRARY_ROUTE.BOOKS:
      return 'browse-books'
    default:
      return 'browse-libraries'
  }
}

const router = new Router({
  mode: 'history',
  base: urls.base,
  parseQuery(query: string) {
    return qs.parse(query)
  },
  stringifyQuery(query: Object) {
    const res = qs.stringify(query)
    return res ? `?${res}` : ''
  },
  routes: [
    {
      path: '/',
      component: () => import(/* webpackChunkName: "home" */ './views/HomeView.vue'),
      children: [
        {
          // SV-1201: the app no longer opens onto the dashboard. `home` is now
          // the empty main area, so guards that fall back to it land on nothing
          // rather than on a screen the owner asked to stop being the entry.
          path: '',
          name: 'home',
          component: () => import(/* webpackChunkName: "empty-main" */ './views/EmptyMainView.vue'),
        },
        {
          path: '/welcome',
          name: 'welcome',
          component: () => import(/* webpackChunkName: "welcome" */ './views/WelcomeView.vue'),
        },
        {
          path: '/no-pins',
          name: 'no-pins',
          component: () => import(/* webpackChunkName: "no-pins" */ './views/NoPinnedLibraries.vue'),
        },
        {
          path: '/dashboard',
          name: 'dashboard',
          beforeEnter: noLibraryNorPinGuard,
          component: () => import(/* webpackChunkName: "dashboard" */ './views/DashboardView.vue'),
        },
        {
          path: '/settings/users',
          name: 'settings-users',
          beforeEnter: adminGuard,
          component: () => import(/* webpackChunkName: "settings-users" */ './views/SettingsUsers.vue'),
          children: [
            {
              path: '/settings/users/add',
              name: 'settings-users-add',
              component: () => import(/* webpackChunkName: "settings-user" */ './components/dialogs/UserAddDialog.vue'),
            },
          ],
        },
        {
          path: '/settings',
          name: 'preferences',
          component: () => import(/* webpackChunkName: "preferences" */ './views/PreferencesView.vue'),
        },
        {
          // Server settings, UI settings and account details became tabs of one
          // preferences screen (SV-1203). Kept as redirects so existing
          // bookmarks do not dead-end.
          path: '/settings/server',
          redirect: {name: 'preferences'},
        },
        {
          path: '/settings/ui',
          redirect: {name: 'preferences'},
        },
        {
          path: '/account/me',
          redirect: {name: 'preferences'},
        },
        {
          path: '/libraries/:libraryId?',
          name: 'libraries',
          redirect: (route) => ({
            name: getLibraryRoute(route.params.libraryId || LIBRARIES_ALL),
            params: {libraryId: route.params.libraryId || LIBRARIES_ALL},
          }),
        },
        {
          // The per-library Recommended view was removed. Kept as a redirect so
          // existing bookmarks and stored library routes do not dead-end.
          path: '/libraries/:libraryId/recommended',
          redirect: (route) => ({
            name: 'browse-libraries',
            params: {libraryId: route.params.libraryId},
          }),
        },
        {
          path: '/libraries/:libraryId/books',
          name: 'browse-books',
          beforeEnter: noLibraryGuard,
          component: () => import(/* webpackChunkName: "browse-books" */ './views/BrowseBooks.vue'),
          props: (route) => ({libraryId: route.params.libraryId}),
        },
        {
          path: '/libraries/:libraryId/series',
          name: 'browse-libraries',
          beforeEnter: noLibraryGuard,
          component: () => import(/* webpackChunkName: "browse-libraries" */ './views/BrowseLibraries.vue'),
          props: (route) => ({libraryId: route.params.libraryId}),
        },
        {
          path: '/series/:seriesId',
          name: 'browse-series',
          component: () => import(/* webpackChunkName: "browse-series" */ './views/BrowseSeries.vue'),
          props: (route) => ({seriesId: route.params.seriesId}),
        },
        {
          path: '/book/:bookId',
          name: 'browse-book',
          component: () => import(/* webpackChunkName: "browse-book" */ './views/BrowseBook.vue'),
          props: (route) => ({bookId: route.params.bookId}),
        },
        {
          path: '/oneshot/:seriesId',
          name: 'browse-oneshot',
          component: () => import(/* webpackChunkName: "browse-oneshot" */ './views/BrowseOneshot.vue'),
          props: (route) => ({seriesId: route.params.seriesId}),
        },
        {
          path: '/search',
          name: 'search',
          component: () => import(/* webpackChunkName: "search" */ './views/SearchView.vue'),
        },
      ],
    },
    {
      path: '/startup',
      name: 'startup',
      component: () => import(/* webpackChunkName: "startup" */ './views/StartupView.vue'),
    },
    {
      path: '/login',
      name: 'login',
      component: () => import(/* webpackChunkName: "login" */ './views/LoginView.vue'),
    },
    {
      path: '/book/:bookId/read',
      name: 'read-book',
      component: () => import(/* webpackChunkName: "read-book" */ './views/DivinaReader.vue'),
      props: (route) => ({bookId: route.params.bookId}),
    },
    {
      path: '/read-v2/:bookId',
      name: 'read-book-v2',
      beforeEnter: shelfViewReaderGuard,
      component: () => import(/* webpackChunkName: "read-book-v2" */ './shelfview/views/ShelfViewReaderShell.vue'),
      props: (route) => ({bookId: route.params.bookId}),
    },
    {
      path: '/read-local',
      name: 'read-local',
      component: () => import(/* webpackChunkName: "read-local" */ './shelfview/views/ShelfViewLocalReader.vue'),
    },
    {
      path: '/book/:bookId/read-epub',
      name: 'read-epub',
      component: () => import(/* webpackChunkName: "read-epub" */ './views/EpubReader.vue'),
      props: (route) => ({bookId: route.params.bookId}),
    },
    {
      path: '*',
      name: 'notfound',
      component: () => import(/* webpackChunkName: "notfound" */ './views/PageNotFound.vue'),
    },
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      if (to.name !== from.name) {
        return {x: 0, y: 0}
      }
    }
  },
})

router.beforeEach((to, from, next) => {
  // avoid document.title flickering when changing route
  if (!['read-book', 'read-book-v2', 'read-local', 'read-epub', 'browse-book', 'browse-oneshot', 'browse-series', 'browse-libraries', 'browse-books',
  ].includes(<string>to.name)
  ) {
    document.title = 'Tales'
  }

  if (window.opener !== null &&
    window.name === 'oauth2Login' &&
    to.query.server_redirect === 'Y'
  ) {
    if (!to.query.error) {
      // authentication succeeded, we redirect the parent window so that it can login via cookie
      window.opener.location.href = urls.origin
    } else {
      // authentication failed, we cascade the error message to the parent
      window.opener.location.href = window.location
    }
    // we can close the popup
    window.close()
  }

  if (to.name !== 'startup' && to.name !== 'login' && !lStore.getters.authenticated) {
    const query = Object.assign({}, to.query, {redirect: to.fullPath})
    next({name: 'startup', query: query})
  } else next()
})

export default router
