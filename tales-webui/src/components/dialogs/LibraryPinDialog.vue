<template>
  <v-dialog v-model="modal"
            max-width="450"
  >
    <v-card>
      <v-card-title>{{ title }}</v-card-title>

      <v-card-text>
        <v-container fluid>
          <v-row v-if="body">
            <v-col>{{ body }}</v-col>
          </v-row>

          <v-row>
            <v-col>
              <v-text-field
                v-model="pin"
                :label="$t('dialog.library_pin.pin_label')"
                type="password"
                autocomplete="off"
                autofocus
                :error-messages="errorMessage"
                @input="$emit('clear-error')"
                @keydown.enter="dialogConfirm"
              />
            </v-col>
          </v-row>
        </v-container>
      </v-card-text>

      <v-card-actions>
        <v-spacer/>
        <v-btn text @click="dialogCancel">{{ $t('common.cancel') }}</v-btn>
        <v-btn color="primary"
               :disabled="pin.length < 4"
               @click="dialogConfirm"
        >{{ buttonConfirm }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import Vue from 'vue'

// Unlike ConfirmationDialog this does not close itself on confirm: a wrong
// PIN must keep the dialog open with the error shown, so the caller owns the
// v-model and closes it only on success.
export default Vue.extend({
  name: 'LibraryPinDialog',
  data: () => {
    return {
      modal: false,
      pin: '',
    }
  },
  props: {
    value: Boolean,
    title: {
      type: String,
      required: true,
    },
    body: {
      type: String,
      required: false,
    },
    buttonConfirm: {
      type: String,
      required: true,
    },
    errorMessage: {
      type: String,
      required: false,
      default: '',
    },
  },
  watch: {
    value(val) {
      this.modal = val
    },
    modal(val) {
      if (!val) this.dialogCancel()
    },
  },
  methods: {
    dialogCancel() {
      this.pin = ''
      this.$emit('input', false)
    },
    dialogConfirm() {
      if (this.pin.length < 4) return
      this.$emit('confirm', this.pin)
    },
  },
})
</script>
