<template>
  <v-container fluid class="pa-6">
    <v-row>
      <v-col cols="auto">
        <span class="font-weight-black text-h6">{{ $t('ui_settings.general') }}</span>

        <v-radio-group
          v-model="form.seriesGroups"
          @change="$v.form.seriesGroups.$touch()"
          :label="$t('ui_settings.label_series_groups')"
          hide-details
        >
          <v-radio value="alpha" :label="$t('ui_settings.series_groups.alpha')"/>
        </v-radio-group>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="auto">
        <v-btn @click="refreshSettings"
               :disabled="discardDisabled"
        >{{ $t('common.discard') }}
        </v-btn>
      </v-col>
      <v-col cols="auto">
        <v-btn color="primary"
               :disabled="saveDisabled"
               @click="saveSettings"
        >{{ $t('common.save_changes') }}
        </v-btn>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import {
  CLIENT_SETTING,
  ClientSettingGlobalUpdateDto,
  ClientSettingsSeriesGroup,
  SERIES_GROUP_ALPHA,
  SERIES_GROUP_JAPANESE,
} from '@/types/tales-clientsettings'

export default Vue.extend({
  name: 'UISettings',
  data: () => ({
    form: {
      seriesGroups: 'alpha',
    },
  }),
  validations: {
    form: {
      seriesGroups: {},
    },
  },
  mounted() {
    this.refreshSettings()
  },
  computed: {
    saveDisabled(): boolean {
      return this.$v.form.$invalid || !this.$v.form.$anyDirty
    },
    discardDisabled(): boolean {
      return !this.$v.form.$anyDirty
    },
  },
  methods: {
    async refreshSettings() {
      await this.$store.dispatch('getClientSettingsGlobal')
      try {
        this.form.seriesGroups = (JSON.parse(this.$store.state.talesSettings.clientSettingsGlobal[CLIENT_SETTING.WEBUI_SERIES_GROUPS]?.value) as ClientSettingsSeriesGroup)?.name
      } catch (_) {
      }
      this.$v.form.$reset()
    },
    async saveSettings() {
      let newSettings = {} as Record<string, ClientSettingGlobalUpdateDto>
      if (this.$v.form?.seriesGroups?.$dirty)
        newSettings[CLIENT_SETTING.WEBUI_SERIES_GROUPS] = {
          value: JSON.stringify(this.form.seriesGroups === 'alpha' ? SERIES_GROUP_ALPHA : SERIES_GROUP_JAPANESE),
          allowUnauthorized: false,
        }

      await this.$talesSettings.updateClientSettingGlobal(newSettings)

      await this.refreshSettings()
    },
  },
})
</script>

<style scoped>

</style>
