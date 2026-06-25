import {shallowMount} from '@vue/test-utils'
import MetricsView from '@/views/MetricsView.vue'

const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))
const flushPromises = () => wait(0)

const summary = {
  totals: {
    libraries: 2,
    series: 5,
    books: 12,
    users: 3,
    collections: 4,
    readlists: 6,
    sidecars: 3,
    booksFileSize: 2048,
  },
  perLibrary: {
    series: {
      library1: 2,
      library2: 3,
    },
    books: {
      library1: 7,
      library2: 5,
    },
    sidecars: {
      library1: 1,
      library2: 2,
    },
    booksFileSize: {
      library1: 1024,
      library2: 1024,
    },
  },
  tasks: {
    executed: {
      scan: 2,
    },
    totalTimeSeconds: {
      scan: 10,
    },
  },
  readProgressIncluded: false,
}

const emptySummary = {
  totals: {
    libraries: 0,
    series: 0,
    books: 0,
    users: 0,
    collections: 0,
    readlists: 0,
    sidecars: 0,
    booksFileSize: 0,
  },
  perLibrary: {
    series: {},
    books: {},
    sidecars: {},
    booksFileSize: {},
  },
  tasks: {
    executed: {},
    totalTimeSeconds: {},
  },
  readProgressIncluded: false,
}

describe('MetricsView', () => {
  it('loads one metrics summary instead of many actuator metric requests', async () => {
    const getSummary = jest.fn().mockResolvedValue(summary)
    const getMetric = jest.fn()

    const wrapper = mountMetricsView({
      getSummary,
      getMetric,
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(getSummary).toHaveBeenCalledTimes(1)
    expect(getMetric).not.toHaveBeenCalled()
    expect((wrapper.vm as any).booksAllTags).toEqual({
      'Main library': 7,
      'Archive library': 5,
    })
    expect((wrapper.vm as any).tasksCount).toEqual({scan: 2})
    expect((wrapper.vm as any).loadError).toBe(false)
  })

  it('shows an empty state when every summary value is zero', async () => {
    const wrapper = mountMetricsView({
      getSummary: jest.fn().mockResolvedValue(emptySummary),
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    expect((wrapper.vm as any).showEmptyState).toBe(true)
    expect(wrapper.text()).toContain('metrics.no_data')
  })

  it('shows an error state when the summary cannot be loaded', async () => {
    const wrapper = mountMetricsView({
      getSummary: jest.fn().mockRejectedValue(new Error('failed')),
    })

    await flushPromises()
    await wrapper.vm.$nextTick()

    expect((wrapper.vm as any).summary).toBeUndefined()
    expect((wrapper.vm as any).loadError).toBe(true)
    expect(wrapper.text()).toContain('metrics.error_loading')
  })
})

function mountMetricsView(talesMetrics: {
  getSummary: jest.Mock,
  getMetric?: jest.Mock,
}) {
  return shallowMount(MetricsView, {
    mocks: {
      $store: {
        getters: {
          getLibraryById: (id: string) => ({
            library1: {name: 'Main library'},
            library2: {name: 'Archive library'},
          }[id]),
        },
      },
      $t: (key: string) => key,
      $tc: (key: string) => key,
      $talesMetrics: {
        getMetric: talesMetrics.getMetric ?? jest.fn(),
        getSummary: talesMetrics.getSummary,
      },
      $vuetify: {
        rtl: false,
      },
    },
    stubs: [
      'bar-chart',
      'pie-chart',
      'v-alert',
      'v-card',
      'v-card-text',
      'v-card-title',
      'v-col',
      'v-container',
      'v-icon',
      'v-progress-linear',
      'v-row',
    ],
  })
}
