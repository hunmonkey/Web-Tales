<template>
  <v-container fluid class="pa-6">
    <v-row>
      <v-col class="d-flex justify-end py-0">
        <v-btn color="error" outlined @click="confirmDialog = true">
          <v-icon left>mdi-delete-sweep-outline</v-icon>
          {{ $t('authentication_activity.clear_history') }}
        </v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-col>
        <authentication-activity-table for-me ref="table"/>
      </v-col>
    </v-row>

    <confirmation-dialog
      v-model="confirmDialog"
      :title="$t('dialog.clear_authentication_activity.dialog_title')"
      :body="$t('dialog.clear_authentication_activity.body')"
      :confirm-text="$t('dialog.clear_authentication_activity.confirm')"
      :button-confirm="$t('dialog.clear_authentication_activity.button_confirm')"
      button-confirm-color="error"
      @confirm="clearActivity"
    />
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import AuthenticationActivityTable from '@/components/AuthenticationActivityTable.vue'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import {ERROR} from '@/types/events'

export default Vue.extend({
  name: 'SelfAuthenticationActivity',
  components: {AuthenticationActivityTable, ConfirmationDialog},
  data: () => ({
    confirmDialog: false,
  }),
  methods: {
    async clearActivity() {
      try {
        await this.$talesUsers.deleteMyAuthenticationActivity()
        ;(this.$refs.table as any).loadData()
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
    },
  },
})
</script>

<style scoped>

</style>
