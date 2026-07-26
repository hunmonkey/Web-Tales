import {shallowMount} from '@vue/test-utils'
import ItemBrowser from '@/components/ItemBrowser.vue'

jest.mock('@/i18n', () => ({
  __esModule: true,
  default: {
    locale: 'en',
    t: (key: string) => key,
    tc: (key: string, count: number) => `${key}:${count}`,
  },
}))

function mountBrowser(propsData: Record<string, any>) {
  return shallowMount(ItemBrowser, {
    propsData,
    directives: {
      resize: {},
    },
    mocks: {
      $vuetify: {breakpoint: {name: 'lg'}},
      $_: {head: (a: any[]) => a[0]},
    },
    stubs: {
      'v-item-group': {template: '<div><slot /></div>'},
      'v-row': {template: '<div><slot /></div>'},
      draggable: {template: '<div><slot /></div>'},
    },
    slots: {
      empty: '<p data-testid="empty-slot">nothing here</p>',
    },
  })
}

describe('ItemBrowser', () => {
  it('shows card-shaped placeholders instead of the empty slot while loading', () => {
    const wrapper = mountBrowser({items: [], loading: true, fixedItemWidth: 150})

    const skeleton = wrapper.find('[data-testid="item-browser-skeleton"]')
    expect(skeleton.exists()).toBe(true)
    expect(skeleton.findAllComponents({name: 'ItemCardSkeleton'}).length).toBe(12)
    expect(wrapper.find('[data-testid="empty-slot"]').exists()).toBe(false)
  })

  it('draws fewer placeholders for a single horizontal row', () => {
    const wrapper = mountBrowser({items: [], loading: true, nowrap: true, fixedItemWidth: 150})

    expect(wrapper.findAllComponents({name: 'ItemCardSkeleton'}).length).toBe(8)
  })

  it('falls back to the empty slot once loading finished with no results', () => {
    const wrapper = mountBrowser({items: [], loading: false})

    expect(wrapper.find('[data-testid="item-browser-skeleton"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="empty-slot"]').exists()).toBe(true)
  })

  it('never shows placeholders once there are items to draw', () => {
    const wrapper = mountBrowser({items: [{id: 'a'}], loading: true})

    expect(wrapper.find('[data-testid="item-browser-skeleton"]').exists()).toBe(false)
    expect(wrapper.findAllComponents({name: 'ItemCardSkeleton'}).length).toBe(0)
  })
})
