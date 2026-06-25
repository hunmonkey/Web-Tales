import {shallowMount} from '@vue/test-utils'
import UsersList from '@/components/UsersList.vue'

const mountUsersList = (options: {
  users?: any[],
  talesUsers?: any,
} = {}) => {
  return shallowMount(UsersList, {
    mocks: {
      $eventHub: {
        $emit: jest.fn(),
      },
      $store: {
        dispatch: jest.fn().mockResolvedValue(undefined),
        state: {
          talesUsers: {
            me: {id: 'me'},
            users: options.users ?? [],
          },
        },
      },
      $t: (key: string) => key,
      $i18n: {
        locale: 'en',
      },
      $talesUsers: options.talesUsers ?? {
        getLatestAuthenticationActivitiesByUser: jest.fn().mockResolvedValue({}),
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
}

describe('UsersList', () => {
  it('shows an explicit add-user button', () => {
    const wrapper = mountUsersList()

    const addButton = wrapper
      .findAllComponents({name: 'v-btn'})
      .wrappers
      .find(button => button.text().includes('dialog.add_user.dialog_title'))

    expect(addButton).toBeDefined()
    expect(addButton!.props('to')).toEqual({name: 'settings-users-add'})
  })

  it('loads latest authentication activity with one batched request', async () => {
    const getLatestAuthenticationActivitiesByUser = jest.fn().mockResolvedValue({
      user1: {
        dateTime: '2026-06-25T11:00:00Z',
      },
    })

    const wrapper = mountUsersList({
      users: [
        {id: 'user1', email: 'user1@example.org', roles: []},
        {id: 'user2', email: 'user2@example.org', roles: []},
      ],
      talesUsers: {
        getLatestAuthenticationActivitiesByUser,
      },
    })

    await Promise.resolve()
    await wrapper.vm.$nextTick()

    expect(getLatestAuthenticationActivitiesByUser).toHaveBeenCalledTimes(1)
    expect((wrapper.vm as any).usersLastActivity.user1).toEqual(new Date('2026-06-25T11:00:00Z'))
    expect((wrapper.vm as any).usersLastActivity.user2).toBeUndefined()
  })
})
