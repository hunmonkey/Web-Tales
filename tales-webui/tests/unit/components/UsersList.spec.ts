import {shallowMount} from '@vue/test-utils'
import UsersList from '@/components/UsersList.vue'

describe('UsersList', () => {
  it('shows an explicit add-user button', () => {
    const wrapper = shallowMount(UsersList, {
      mocks: {
        $eventHub: {
          $emit: jest.fn(),
        },
        $store: {
          dispatch: jest.fn().mockResolvedValue(undefined),
          state: {
            talesUsers: {
              me: {id: 'me'},
              users: [],
            },
          },
        },
        $t: (key: string) => key,
        $talesUsers: {
          getLatestAuthenticationActivityForUser: jest.fn(),
        },
        $vuetify: {
          rtl: false,
        },
      },
      stubs: {
        'confirmation-dialog': true,
        'password-change-dialog': true,
        'router-view': true,
        'user-edit-dialog': true,
        'user-restrictions-edit-dialog': true,
        'v-btn': {
          name: 'v-btn',
          props: ['to'],
          template: '<button><slot /></button>',
        },
        'v-divider': true,
        'v-icon': true,
        'v-list': {
          template: '<div><slot /></div>',
        },
        'v-list-item': {
          template: '<div><slot /></div>',
        },
        'v-list-item-action': {
          template: '<div><slot /></div>',
        },
        'v-list-item-content': {
          template: '<div><slot /></div>',
        },
        'v-list-item-icon': {
          template: '<div><slot /></div>',
        },
        'v-list-item-subtitle': {
          template: '<div><slot /></div>',
        },
        'v-list-item-title': {
          template: '<div><slot /></div>',
        },
        'v-tooltip': {
          template: '<div><slot name="activator" :on="{}" /><slot /></div>',
        },
      },
    })

    const addButton = wrapper
      .findAllComponents({name: 'v-btn'})
      .wrappers
      .find(button => button.text().includes('dialog.add_user.dialog_title'))

    expect(addButton).toBeDefined()
    expect(addButton!.props('to')).toEqual({name: 'settings-users-add'})
  })
})
