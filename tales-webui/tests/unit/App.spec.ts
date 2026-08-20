import {createLocalVue, shallowMount} from '@vue/test-utils'
import Vuex, {Store} from 'vuex'
import App from '@/App.vue'
import {loadLocale} from '@/i18n'
import {persistedModule} from '@/plugins/persisted-state'

jest.mock('@/i18n', () => ({
  __esModule: true,
  loadLocale: jest.fn(),
  default: {
    locale: 'en',
  },
}))

const mockedLoadLocale = loadLocale as jest.MockedFunction<typeof loadLocale>

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>(promiseResolve => {
    resolve = promiseResolve
  })
  return {promise, resolve}
}

function mountApp(initialLocale: string) {
  const localVue = createLocalVue()
  localVue.use(Vuex)

  const store = new Store({
    modules: {
      persistedState: {
        ...persistedModule,
        state: {
          ...(persistedModule.state as Record<string, unknown>),
          locale: initialLocale,
        },
      },
    },
  })
  const i18n = {locale: 'en'}
  const vuetify = {
    rtl: false,
    theme: {dark: false},
  }

  const wrapper = shallowMount(App, {
    localVue,
    store,
    mocks: {
      $eventHub: {
        $on: jest.fn(),
        $off: jest.fn(),
      },
      $i18n: i18n,
      $router: {push: jest.fn()},
      $t: () => i18n.locale === 'ar' ? 'true' : 'false',
      $talesUsers: {logout: jest.fn()},
      $vuetify: vuetify,
    },
    stubs: {
      'router-view': true,
      'v-app': {
        template: '<div><slot /></div>',
      },
    },
  })

  return {i18n, store, vuetify, wrapper}
}

async function flushLocaleLoad() {
  await Promise.resolve()
  await Promise.resolve()
}

describe('App locale loading', () => {
  beforeEach(() => {
    mockedLoadLocale.mockReset()
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      value: jest.fn().mockReturnValue({
        matches: false,
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
      }),
    })
  })

  it('does not let an older lazy load replace a newer eager locale', async () => {
    const slowLoad = deferred<boolean>()
    mockedLoadLocale.mockImplementation(locale => locale === 'fr' ? slowLoad.promise : Promise.resolve(true))
    const {i18n, store, vuetify, wrapper} = mountApp('fr')

    store.commit('setLocale', 'en')
    await wrapper.vm.$nextTick()
    await flushLocaleLoad()

    expect(i18n.locale).toBe('en')
    expect(vuetify.rtl).toBe(false)

    slowLoad.resolve(true)
    await flushLocaleLoad()

    expect(i18n.locale).toBe('en')
    expect(vuetify.rtl).toBe(false)
    wrapper.destroy()
  })

  it('does not let an older lazy load replace a newer lazy locale', async () => {
    const slowLoad = deferred<boolean>()
    const latestLoad = deferred<boolean>()
    mockedLoadLocale.mockImplementation(locale => locale === 'fr' ? slowLoad.promise : latestLoad.promise)
    const {i18n, store, vuetify, wrapper} = mountApp('fr')

    store.commit('setLocale', 'ar')
    await wrapper.vm.$nextTick()
    latestLoad.resolve(true)
    await flushLocaleLoad()

    expect(i18n.locale).toBe('ar')
    expect(vuetify.rtl).toBe(true)

    slowLoad.resolve(true)
    await flushLocaleLoad()

    expect(i18n.locale).toBe('ar')
    expect(vuetify.rtl).toBe(true)
    wrapper.destroy()
  })

  it('does not apply an older success after the latest locale load fails', async () => {
    const slowLoad = deferred<boolean>()
    mockedLoadLocale.mockImplementation(locale => locale === 'fr' ? slowLoad.promise : Promise.resolve(false))
    const {i18n, store, vuetify, wrapper} = mountApp('fr')

    store.commit('setLocale', 'ar')
    await wrapper.vm.$nextTick()
    await flushLocaleLoad()

    expect(i18n.locale).toBe('en')
    expect(vuetify.rtl).toBe(false)

    slowLoad.resolve(true)
    await flushLocaleLoad()

    expect(i18n.locale).toBe('en')
    expect(vuetify.rtl).toBe(false)
    wrapper.destroy()
  })
})
