import {shallowMount} from '@vue/test-utils'
import ShelfViewControlMenu from '@/shelfview/components/ShelfViewControlMenu.vue'

describe('ShelfViewControlMenu', () => {
  function mountMenu() {
    return shallowMount(ShelfViewControlMenu, {
      propsData: {
        menu: 'fit',
        title: 'Page fit',
        left: 32,
        options: [
          {
            value: 'SCREEN',
            icon: 'mdi-fit-to-screen',
            label: 'Screen',
            active: true,
          },
          {
            value: 'WIDTH',
            icon: 'mdi-arrow-expand-horizontal',
            label: 'Width',
            active: false,
          },
        ],
      },
      stubs: {
        'v-icon': {template: '<i><slot /></i>'},
      },
    })
  }

  it('renders the title, position and options', () => {
    const wrapper = mountMenu()

    expect(wrapper.find('[data-testid="shelfview-reader-control-menu"]').attributes('aria-label')).toBe('Page fit')
    expect(wrapper.find('[data-testid="shelfview-reader-control-menu"]').attributes('style')).toContain('left: 32px')
    expect(wrapper.find('[data-testid="shelfview-reader-fit-option-SCREEN"]').attributes('aria-checked')).toBe('true')
    expect(wrapper.find('[data-testid="shelfview-reader-fit-option-WIDTH"]').attributes('aria-checked')).toBe('false')
    expect(wrapper.text()).toContain('Page fit')
    expect(wrapper.text()).toContain('Screen')
    expect(wrapper.text()).toContain('mdi-check')
  })

  it('emits close from the scrim', async () => {
    const wrapper = mountMenu()

    await wrapper.find('[data-testid="shelfview-reader-control-scrim"]').trigger('click')

    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('emits the selected option value', async () => {
    const wrapper = mountMenu()

    await wrapper.find('[data-testid="shelfview-reader-fit-option-WIDTH"]').trigger('click')

    expect(wrapper.emitted('select')).toEqual([['WIDTH']])
  })
})
