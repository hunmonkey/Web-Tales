<template>
  <div style="position: relative">
    <div class="d-flex justify-end mb-3">
      <v-btn color="primary"
             :to="{name: 'settings-users-add'}"
      >
        <v-icon left>mdi-account-plus</v-icon>
        {{ $t('dialog.add_user.dialog_title') }}
      </v-btn>
    </div>

    <v-list
      elevation="3"
      two-line
    >
      <div v-for="(u, index) in users" :key="index">
        <v-list-item
        >
          <v-tooltip bottom>
            <template v-slot:activator="{ on }">
              <v-list-item-icon v-on="on">
                <v-icon v-if="u.roles.includes(UserRoles.ADMIN)" color="red">mdi-account-star</v-icon>
                <v-icon v-else>mdi-account</v-icon>
              </v-list-item-icon>
            </template>
            <span>{{
                u.roles.includes(UserRoles.ADMIN) ? $t('settings_user.role_administrator') : $t('settings_user.role_user')
              }}</span>
          </v-tooltip>

          <v-list-item-content>
            <v-list-item-title>
              {{ u.email }}
            </v-list-item-title>
            <v-list-item-subtitle v-if="usersLastActivity[u.id] !== undefined">
              {{
                $t('settings_user.latest_activity', {
                  date:
                    new Intl.DateTimeFormat($i18n.locale, {
                      dateStyle: 'medium',
                      timeStyle: 'short'
                    }).format(usersLastActivity[u.id])
                })
              }}
            </v-list-item-subtitle>
            <v-list-item-subtitle v-else>{{ $t('settings_user.no_recent_activity') }}</v-list-item-subtitle>
          </v-list-item-content>

          <v-list-item-action>
            <v-tooltip bottom>
              <template v-slot:activator="{ on }">
                <v-btn icon @click="editRestrictions(u)" :disabled="u.roles.includes(UserRoles.ADMIN)"
                       v-on="on" :aria-label="$t('settings_user.edit_restrictions')">
                  <v-icon>mdi-book-lock</v-icon>
                </v-btn>
              </template>
              <span>{{ $t('settings_user.edit_restrictions') }}</span>
            </v-tooltip>
          </v-list-item-action>

          <v-list-item-action>
            <v-tooltip bottom>
              <template v-slot:activator="{ on }">
                <v-btn icon @click="changeUserPassword(u)" v-on="on" :aria-label="$t('settings_user.change_password')">
                  <v-icon>mdi-lock-reset</v-icon>
                </v-btn>
              </template>
              <span>{{ $t('settings_user.change_password') }}</span>
            </v-tooltip>
          </v-list-item-action>

          <v-list-item-action>
            <v-tooltip bottom>
              <template v-slot:activator="{ on }">
                <v-btn icon @click="editUser(u)" :disabled="u.id === me.id" v-on="on" :aria-label="$t('settings_user.edit_user')">
                  <v-icon>mdi-pencil</v-icon>
                </v-btn>
              </template>
              <span>{{ $t('settings_user.edit_user') }}</span>
            </v-tooltip>
          </v-list-item-action>

          <v-list-item-action>
            <v-btn icon @click="promptDeleteUser(u)"
                   :disabled="u.id === me.id"
                   :aria-label="$t('common.delete')"
            >
              <v-icon>mdi-delete-outline</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>

        <v-divider v-if="index !== users.length-1"/>
      </div>
    </v-list>

    <user-restrictions-edit-dialog v-model="modalEditRestrictions"
                                       :user="userToEditRestrictions"
    />

    <password-change-dialog v-model="modalChangePassword"
                            :user="userToChangePassword"
    />

    <user-edit-dialog v-model="modalEditUser"
                      :user="userToEdit"
    />

    <confirmation-dialog
      v-model="modalDeleteUser"
      :title="$t('dialog.delete_user.dialog_title')"
      :body-html="$t('dialog.delete_user.warning_html', {name: userToDelete.email})"
      :confirm-text=" $t('dialog.delete_user.confirm_delete', {name: userToDelete.email})"
      :button-confirm="$t('dialog.delete_user.button_confirm')"
      button-confirm-color="error"
      @confirm="deleteUser"
    />

    <router-view/>
  </div>
</template>

<script lang="ts">
import UserEditDialog from '@/components/dialogs/UserEditDialog.vue'
import UserRestrictionsEditDialog from '@/components/dialogs/UserRestrictionsEditDialog.vue'
import {UserRoles} from '@/types/enum-users'
import Vue from 'vue'
import PasswordChangeDialog from '@/components/dialogs/PasswordChangeDialog.vue'
import {ERROR} from '@/types/events'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import { UserDto } from '@/types/tales-users'

export default Vue.extend({
  name: 'UsersList',
  components: {
    ConfirmationDialog,
    UserRestrictionsEditDialog,
    UserEditDialog,
    PasswordChangeDialog,
  },
  data: () => ({
    UserRoles,
    modalDeleteUser: false,
    userToDelete: {} as UserDto,
    modalEditRestrictions: false,
    userToEditRestrictions: {} as UserDto,
    modalEditUser: false,
    userToEdit: {} as UserDto,
    modalChangePassword: false,
    userToChangePassword: {} as UserDto,
    usersLastActivity: {} as { [userId: string]: Date },
    latestActivitiesLoadId: 0,
  }),
  computed: {
    users(): UserDto[] {
      return this.$store.state.talesUsers.users
    },
    me(): UserDto {
      return this.$store.state.talesUsers.me
    },
  },
  watch: {
    users: {
      immediate: true,
      handler() {
        this.loadLatestAuthenticationActivities()
      },
    },
  },
  async mounted() {
    await this.$store.dispatch('getAllUsers')
  },
  methods: {
    promptDeleteUser(user: UserDto) {
      this.userToDelete = user
      this.modalDeleteUser = true
    },
    editRestrictions(user: UserDto) {
      this.userToEditRestrictions = user
      this.modalEditRestrictions = true
    },
    editUser(user: UserDto) {
      this.userToEdit = user
      this.modalEditUser = true
    },
    changeUserPassword(user: UserDto) {
      this.userToChangePassword = user
      this.modalChangePassword = true
    },
    async loadLatestAuthenticationActivities() {
      if (this.users.length === 0) {
        this.usersLastActivity = {}
        return
      }

      const loadId = ++this.latestActivitiesLoadId

      try {
        const activities = await this.$talesUsers.getLatestAuthenticationActivitiesByUser()
        if (loadId !== this.latestActivitiesLoadId) return

        const next = {} as { [userId: string]: Date }
        this.users.forEach((user: UserDto) => {
          const activity = activities[user.id]
          if (activity?.dateTime) next[user.id] = new Date(activity.dateTime)
        })
        this.usersLastActivity = next
      } catch (e) {
      }
    },
    async deleteUser() {
      try {
        await this.$store.dispatch('deleteUser', this.userToDelete)
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
    },
  },
})
</script>

<style scoped>

</style>
