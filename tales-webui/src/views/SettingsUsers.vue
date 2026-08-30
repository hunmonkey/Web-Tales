<template>
  <v-container fluid class="pa-6">
    <v-row>
      <v-col class="text-h5">{{ $t('users.users') }}</v-col>
    </v-row>
    <v-row>
      <v-col cols="12" md="10" lg="8" xl="4">
        <users-list/>
      </v-col>
    </v-row>

    <v-row align="center">
      <v-col class="text-h5">{{ $t('users.authentication_activity') }}</v-col>
      <!-- A labelled button rather than an icon, and behind a confirmation:
           clearing the history is exactly the kind of hard-to-undo admin
           action CLAUDE.md requires both for. -->
      <v-col cols="auto">
        <v-btn color="error"
               outlined
               data-testid="delete-activity"
               @click="confirmDeleteActivity = true"
        >
          <v-icon left>mdi-delete-outline</v-icon>
          {{ $t('authentication_activity.clear_history') }}
        </v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <authentication-activity-table ref="activityTable"/>
      </v-col>
    </v-row>

    <confirmation-dialog
      v-model="confirmDeleteActivity"
      :title="$t('dialog.delete_authentication_activity.title')"
      :body="$t('dialog.delete_authentication_activity.body')"
      :button-confirm="$t('dialog.delete_authentication_activity.button_confirm')"
      button-confirm-color="error"
      @confirm="deleteActivity"
    />
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import AuthenticationActivityTable from '@/components/AuthenticationActivityTable.vue'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import UsersList from '@/components/UsersList.vue'
import {ERROR} from '@/types/events'

export default Vue.extend({
  name: 'SettingsUsers',
  components: {UsersList, AuthenticationActivityTable, ConfirmationDialog},
  data: () => {
    return {
      confirmDeleteActivity: false,
    }
  },
  methods: {
    async deleteActivity() {
      try {
        await this.$talesUsers.deleteAllAuthenticationActivity()
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
        return
      }
      (this.$refs.activityTable as any)?.loadData()
    },
  },
})
</script>

<style scoped>

</style>
