import {createLocalVue, shallowMount} from '@vue/test-utils'
import Vuelidate from 'vuelidate'
import UserAddDialog from '@/components/dialogs/UserAddDialog.vue'
import {UserRoles} from '@/types/enum-users'

const localVue = createLocalVue()
localVue.use(Vuelidate)

describe('UserAddDialog', () => {
  it('sends the password with the new-user payload', async () => {
    const dispatch = jest.fn().mockResolvedValue(undefined)
    const push = jest.fn()
    const wrapper = mountDialog(dispatch, push)
    const vm = wrapper.vm as any

    vm.form.email = 'newuser@example.org'
    vm.form.password = 'new-password'
    vm.form.roles = [UserRoles.PAGE_STREAMING, UserRoles.FILE_DOWNLOAD]
    await vm.$nextTick()

    await vm.addUser()

    expect(dispatch).toHaveBeenCalledWith('postUser', {
      email: 'newuser@example.org',
      password: 'new-password',
      roles: [UserRoles.PAGE_STREAMING, UserRoles.FILE_DOWNLOAD],
    })
    expect(push).toHaveBeenCalledWith({name: 'settings-users'})
  })

  it('does not submit when the email is invalid', async () => {
    const dispatch = jest.fn()
    const wrapper = mountDialog(dispatch)
    const vm = wrapper.vm as any

    vm.form.email = 'bad-email'
    vm.form.password = 'new-password'
    await vm.$nextTick()

    await vm.addUser()

    expect(dispatch).not.toHaveBeenCalled()
  })
})

function mountDialog(
  dispatch: jest.Mock,
  push: jest.Mock = jest.fn(),
) {
  return shallowMount(UserAddDialog, {
    localVue,
    mocks: {
      $eventHub: {
        $emit: jest.fn(),
      },
      $i18n: {
        t: (key: string) => key,
      },
      $router: {
        push,
      },
      $store: {
        dispatch,
      },
      $t: (key: string) => key,
      $vuetify: {
        breakpoint: {
          xsOnly: false,
        },
      },
    },
    stubs: [
      'v-btn',
      'v-card',
      'v-card-actions',
      'v-card-text',
      'v-card-title',
      'v-checkbox',
      'v-col',
      'v-container',
      'v-dialog',
      'v-icon',
      'v-row',
      'v-spacer',
      'v-text-field',
      'v-toolbar',
      'v-toolbar-items',
      'v-toolbar-title',
    ],
  })
}
