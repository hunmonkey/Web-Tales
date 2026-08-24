import {shallowMount} from '@vue/test-utils'
import LibrariesActionsMenu from '@/components/menus/LibrariesActionsMenu.vue'
import {CLIENT_SETTING} from '@/types/tales-clientsettings'

function mountMenu(librarySort = 'MANUAL') {
  const dispatch = jest.fn()
  const wrapper = shallowMount(LibrariesActionsMenu, {
    mocks: {
      $t: (key: string) => key,
      $store: {
        dispatch,
        getters: {
          meAdmin: false,
          getClientSettingsLibrariesSort: librarySort,
        },
        state: {talesLibraries: {libraries: []}},
      },
    },
    stubs: {
      'v-menu': {template: '<div><slot name="activator" v-bind="{on: {}}" /><slot /></div>'},
      'v-list': {template: '<div><slot /></div>'},
      'v-list-item': {template: '<div v-bind="$attrs" @click="$emit(\'click\')"><slot /></div>'},
      'v-list-item-title': {template: '<div><slot /></div>'},
      'v-list-item-icon': {template: '<div><slot /></div>'},
      'v-subheader': {template: '<div><slot /></div>'},
      'v-divider': true,
      'v-btn': {template: '<button><slot /></button>'},
      'v-icon': {template: '<i><slot /></i>'},
      'confirmation-dialog': true,
    },
  })
  return {wrapper, dispatch}
}

describe('LibrariesActionsMenu', () => {
  it('offers the manual, name and creation-date orders', () => {
    const {wrapper} = mountMenu()

    expect(wrapper.find('[data-testid="libraries-sort-manual"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="libraries-sort-name"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="libraries-sort-created"]').exists()).toBe(true)
  })

  it('saves the chosen order on the account', async () => {
    const {wrapper, dispatch} = mountMenu()

    await wrapper.find('[data-testid="libraries-sort-created"]').trigger('click')

    expect(dispatch).toHaveBeenCalledWith('saveUserPreference', {
      key: CLIENT_SETTING.WEBUI_LIBRARIES_SORT,
      value: 'CREATED',
    })
  })

  it('does not write the order that is already in use', async () => {
    const {wrapper, dispatch} = mountMenu('NAME')

    await wrapper.find('[data-testid="libraries-sort-name"]').trigger('click')

    expect(dispatch).not.toHaveBeenCalled()
  })

  it('still asks the drawer to open the reorder panel', async () => {
    const {wrapper} = mountMenu()

    const reorderItem = wrapper.findAll('div').wrappers
      .find(w => w.text() === 'common.reorder')
    await reorderItem!.trigger('click')

    expect(wrapper.emitted('reorder')).toBeTruthy()
  })
})
