import {shallowMount} from '@vue/test-utils'
import SettingsUsers from '@/views/SettingsUsers.vue'

jest.mock('@/i18n', () => ({
  __esModule: true,
  default: {
    locale: 'en',
    t: (key: string) => key,
    tc: (key: string, count: number) => `${key}:${count}`,
  },
}))

// Both children drag in heavy component trees (UsersList reaches ESM-only
// dependencies Jest cannot parse); these tests only exercise the delete flow.
jest.mock('@/components/UsersList.vue', () => ({__esModule: true, default: {name: 'UsersList', render: (h: any) => h('div')}}))
jest.mock('@/components/AuthenticationActivityTable.vue', () => ({__esModule: true, default: {name: 'AuthenticationActivityTable', render: (h: any) => h('div')}}))

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0))

describe('SettingsUsers activity deletion', () => {
  it('asks for confirmation first: opening the dialog deletes nothing', async () => {
    const deleteAllAuthenticationActivity = jest.fn().mockResolvedValue(undefined)
    const wrapper = mountView({deleteAllAuthenticationActivity})

    expect(wrapper.find('[data-testid="delete-activity"]').exists()).toBe(true)

    // The button only opens the dialog; deletion happens on its confirm.
    ;(wrapper.vm as any).confirmDeleteActivity = true
    await wrapper.vm.$nextTick()

    expect(deleteAllAuthenticationActivity).not.toHaveBeenCalled()
  })

  it('deletes the activity and reloads the table once confirmed', async () => {
    const deleteAllAuthenticationActivity = jest.fn().mockResolvedValue(undefined)
    const loadData = jest.fn()
    const wrapper = mountView({deleteAllAuthenticationActivity})
    ;(wrapper.vm.$refs as any).activityTable = {loadData}

    await (wrapper.vm as any).deleteActivity()

    expect(deleteAllAuthenticationActivity).toHaveBeenCalled()
    expect(loadData).toHaveBeenCalled()
  })

  it('reports a failure instead of pretending the table is empty', async () => {
    const deleteAllAuthenticationActivity = jest.fn().mockRejectedValue(new Error('boom'))
    const loadData = jest.fn()
    const emit = jest.fn()
    const wrapper = mountView({deleteAllAuthenticationActivity, emit})
    ;(wrapper.vm.$refs as any).activityTable = {loadData}

    await (wrapper.vm as any).deleteActivity()
    await flushPromises()

    expect(emit).toHaveBeenCalled()
    expect(loadData).not.toHaveBeenCalled()
  })
})

function mountView(services: {
  deleteAllAuthenticationActivity: jest.Mock,
  emit?: jest.Mock,
}) {
  return shallowMount(SettingsUsers, {
    mocks: {
      $t: (key: string) => key,
      $eventHub: {$on: jest.fn(), $off: jest.fn(), $emit: services.emit ?? jest.fn()},
      $talesUsers: {
        deleteAllAuthenticationActivity: services.deleteAllAuthenticationActivity,
      },
    },
    stubs: [
      'confirmation-dialog',
      'v-btn',
      'v-col',
      'v-container',
      'v-icon',
      'v-row',
    ],
  })
}
