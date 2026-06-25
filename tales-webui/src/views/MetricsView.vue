<template>
  <v-container fluid class="pa-6">
    <v-progress-linear
      v-if="loading"
      indeterminate
      color="primary"
      class="mb-4"
    />

    <v-alert
      v-if="loadError"
      type="error"
      text
      class="mb-4"
    >
      {{ $t('metrics.error_loading') }}
    </v-alert>

    <v-alert
      v-else-if="showEmptyState"
      type="info"
      text
      class="mb-4"
    >
      {{ $t('metrics.no_data') }}
    </v-alert>

    <!--   KPI SUMMARY     -->
    <v-row dense>
      <v-col v-for="kpi in kpis" :key="kpi.key"
             cols="6" sm="4" md="2"
      >
        <v-card :color="kpi.color" dark height="100%" class="d-flex">
          <v-card-text class="d-flex align-center">
            <v-icon size="40" class="me-3">{{ kpi.icon }}</v-icon>
            <div class="overflow-hidden">
              <div class="text-h5 font-weight-bold text-truncate">{{ kpi.value }}</div>
              <div class="text-caption text-uppercase">{{ kpi.label }}</div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!--   TASK CHARTS     -->
    <v-row>
      <v-col cols="12" md="6">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">
            <v-icon left small>mdi-cog-play</v-icon>
            {{ $t('metrics.tasks_executed') }}
          </v-card-title>
          <v-card-text>
            <bar-chart v-if="hasChartData(tasksCount)" :data="tasksCount"/>
            <div v-else class="text-body-2 text--secondary">{{ $t('metrics.no_data') }}</div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="6">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">
            <v-icon left small>mdi-timer-outline</v-icon>
            {{ $t('metrics.tasks_total_time') }}
          </v-card-title>
          <v-card-text>
            <bar-chart v-if="hasChartData(tasksTotalTime)" :data="tasksTotalTime" suffix="s" :round="0"/>
            <div v-else class="text-body-2 text--secondary">{{ $t('metrics.no_data') }}</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!--   LIBRARY DISTRIBUTION     -->
    <v-row>
      <v-col cols="12" sm="6" md="3">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">{{ $t('metrics.library_disk_space') }}</v-card-title>
          <v-card-text>
            <pie-chart v-if="hasChartData(fileSizeAllTags)" :data="fileSizeAllTags" :legend="false" :bytes="true"/>
            <div v-else class="text-body-2 text--secondary">{{ $t('metrics.no_data') }}</div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">{{ $t('metrics.library_books') }}</v-card-title>
          <v-card-text>
            <pie-chart v-if="hasChartData(booksAllTags)" :data="booksAllTags" :legend="false"/>
            <div v-else class="text-body-2 text--secondary">{{ $t('metrics.no_data') }}</div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">{{ $t('metrics.library_series') }}</v-card-title>
          <v-card-text>
            <pie-chart v-if="hasChartData(seriesAllTags)" :data="seriesAllTags" :legend="false"/>
            <div v-else class="text-body-2 text--secondary">{{ $t('metrics.no_data') }}</div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">{{ $t('metrics.library_sidecars') }}</v-card-title>
          <v-card-text>
            <pie-chart v-if="hasChartData(sidecarsAllTags)" :data="sidecarsAllTags" :legend="false"/>
            <div v-else class="text-body-2 text--secondary">{{ $t('metrics.no_data') }}</div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import {MetricsSummaryDto} from '@/types/tales-metrics'
import {getFileSize} from '@/functions/file'

export default Vue.extend({
  name: 'MetricsView',
  data: () => ({
    getFileSize,
    loading: false,
    loadError: false,
    summary: undefined as unknown as MetricsSummaryDto | undefined,
  }),
  computed: {
    kpis(): { key: string, icon: string, color: string, label: string, value: string }[] {
      return [
        {key: 'books', icon: 'mdi-book-multiple', color: 'blue darken-2', label: this.$t('common.books').toString(), value: this.numberValue(this.summary?.totals.books)},
        {key: 'series', icon: 'mdi-bookshelf', color: 'deep-purple darken-1', label: this.$tc('common.series', 2), value: this.numberValue(this.summary?.totals.series)},
        {key: 'users', icon: 'mdi-account-multiple', color: 'teal darken-1', label: this.$t('users.users').toString(), value: this.numberValue(this.summary?.totals.users)},
        {key: 'collections', icon: 'mdi-layers-triple', color: 'orange darken-3', label: this.$t('common.collections').toString(), value: this.numberValue(this.summary?.totals.collections)},
        {key: 'readlists', icon: 'mdi-format-list-bulleted-square', color: 'pink darken-1', label: this.$t('common.readlists').toString(), value: this.numberValue(this.summary?.totals.readlists)},
        {key: 'disk', icon: 'mdi-harddisk', color: 'green darken-2', label: this.$t('common.disk_space').toString(), value: this.fileSizeValue(this.summary?.totals.booksFileSize)},
      ]
    },
    tasksCount(): Record<string, number> | undefined {
      return this.summary?.tasks.executed
    },
    tasksTotalTime(): Record<string, number> | undefined {
      return this.summary?.tasks.totalTimeSeconds
    },
    seriesAllTags(): Record<string, number> | undefined {
      return this.mapLibraryValues(this.summary?.perLibrary.series)
    },
    booksAllTags(): Record<string, number> | undefined {
      return this.mapLibraryValues(this.summary?.perLibrary.books)
    },
    sidecarsAllTags(): Record<string, number> | undefined {
      return this.mapLibraryValues(this.summary?.perLibrary.sidecars)
    },
    fileSizeAllTags(): Record<string, number> | undefined {
      return this.mapLibraryValues(this.summary?.perLibrary.booksFileSize)
    },
    showEmptyState(): boolean {
      const totals = this.summary?.totals
      const noTotals = !!totals && Object.values(totals).every(value => value === 0)

      return !this.loading &&
        !this.loadError &&
        !!this.summary &&
        noTotals &&
        !this.hasChartData(this.tasksCount) &&
        !this.hasChartData(this.tasksTotalTime) &&
        !this.hasChartData(this.seriesAllTags) &&
        !this.hasChartData(this.booksAllTags) &&
        !this.hasChartData(this.sidecarsAllTags) &&
        !this.hasChartData(this.fileSizeAllTags)
    },
  },
  mounted() {
    this.loadData()
  },
  methods: {
    numberValue(value: number | undefined): string {
      if (value === undefined) return '—'
      return Math.round(value).toLocaleString()
    },
    fileSizeValue(value: number | undefined): string {
      if (value === undefined) return '—'
      return getFileSize(value) ?? '—'
    },
    getLibraryNameById(id: string): string {
      return this.$store.getters.getLibraryById(id)?.name ?? id
    },
    async loadData() {
      this.loading = true
      this.loadError = false

      try {
        this.summary = await this.$talesMetrics.getSummary()
      } catch (_) {
        this.summary = undefined
        this.loadError = true
      } finally {
        this.loading = false
      }
    },
    mapLibraryValues(values: Record<string, number> | undefined): Record<string, number> | undefined {
      if (!values) return undefined
      return Object.entries(values).reduce((acc, [libraryId, value]) => {
        acc[this.getLibraryNameById(libraryId)] = value
        return acc
      }, {} as Record<string, number>)
    },
    hasChartData(values: Record<string, number> | undefined): boolean {
      return !!values && Object.values(values).some(value => value > 0)
    },
  },
})
</script>

<style scoped>

</style>
