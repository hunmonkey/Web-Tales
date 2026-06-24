<template>
  <v-container fluid class="pa-6">
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
      <v-col v-if="tasksCount" cols="12" md="6">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">
            <v-icon left small>mdi-cog-play</v-icon>
            {{ $t('metrics.tasks_executed') }}
          </v-card-title>
          <v-card-text>
            <bar-chart :data="tasksCount"/>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col v-if="tasksTotalTime" cols="12" md="6">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">
            <v-icon left small>mdi-timer-outline</v-icon>
            {{ $t('metrics.tasks_total_time') }}
          </v-card-title>
          <v-card-text>
            <bar-chart :data="tasksTotalTime" suffix="s" :round="0"/>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!--   LIBRARY DISTRIBUTION     -->
    <v-row>
      <v-col v-if="fileSizeAllTags" cols="12" sm="6" md="3">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">{{ $t('metrics.library_disk_space') }}</v-card-title>
          <v-card-text>
            <pie-chart :data="fileSizeAllTags" :legend="false" :bytes="true"/>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col v-if="booksAllTags" cols="12" sm="6" md="3">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">{{ $t('metrics.library_books') }}</v-card-title>
          <v-card-text>
            <pie-chart :data="booksAllTags" :legend="false"/>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col v-if="seriesAllTags" cols="12" sm="6" md="3">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">{{ $t('metrics.library_series') }}</v-card-title>
          <v-card-text>
            <pie-chart :data="seriesAllTags" :legend="false"/>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col v-if="sidecarsAllTags" cols="12" sm="6" md="3">
        <v-card height="100%">
          <v-card-title class="text-subtitle-1">{{ $t('metrics.library_sidecars') }}</v-card-title>
          <v-card-text>
            <pie-chart :data="sidecarsAllTags" :legend="false"/>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import {MetricDto} from '@/types/tales-metrics'
import {getFileSize} from '@/functions/file'

export default Vue.extend({
  name: 'MetricsView',
  data: () => ({
    getFileSize,
    tasks: undefined as unknown as MetricDto,
    tasksCount: undefined as unknown as { [key: string]: number | undefined } | undefined,
    tasksTotalTime: undefined as unknown as { [key: string]: number | undefined } | undefined,
    series: undefined as unknown as MetricDto,
    seriesAllTags: undefined as unknown as { [key: string]: number | undefined } | undefined,
    books: undefined as unknown as MetricDto,
    booksAllTags: undefined as unknown as { [key: string]: number | undefined } | undefined,
    sidecars: undefined as unknown as MetricDto,
    sidecarsAllTags: undefined as unknown as { [key: string]: number | undefined } | undefined,
    booksFileSize: undefined as unknown as MetricDto,
    fileSizeAllTags: undefined as unknown as { [key: string]: number | undefined } | undefined,
    collections: undefined as unknown as MetricDto,
    readlists: undefined as unknown as MetricDto,
    users: undefined as unknown as MetricDto,
  }),
  computed: {
    kpis(): { key: string, icon: string, color: string, label: string, value: string }[] {
      return [
        {key: 'books', icon: 'mdi-book-multiple', color: 'blue darken-2', label: this.$t('common.books').toString(), value: this.metricValue(this.books)},
        {key: 'series', icon: 'mdi-bookshelf', color: 'deep-purple darken-1', label: this.$tc('common.series', 2), value: this.metricValue(this.series)},
        {key: 'users', icon: 'mdi-account-multiple', color: 'teal darken-1', label: this.$t('users.users').toString(), value: this.metricValue(this.users)},
        {key: 'collections', icon: 'mdi-layers-triple', color: 'orange darken-3', label: this.$t('common.collections').toString(), value: this.metricValue(this.collections)},
        {key: 'readlists', icon: 'mdi-format-list-bulleted-square', color: 'pink darken-1', label: this.$t('common.readlists').toString(), value: this.metricValue(this.readlists)},
        {key: 'disk', icon: 'mdi-harddisk', color: 'green darken-2', label: this.$t('common.disk_space').toString(), value: getFileSize(this.booksFileSize?.measurements[0].value) ?? '—'},
      ]
    },
  },
  mounted() {
    this.loadData()
  },
  methods: {
    metricValue(metric: MetricDto | undefined): string {
      if (!metric) return '—'
      return Math.round(metric.measurements[0].value).toLocaleString()
    },
    getLibraryNameById(id: string): string {
      return this.$store.getters.getLibraryById(id).name
    },
    async loadData() {
      this.$talesMetrics.getMetric('tales.tasks.execution')
        .then(m => {
          this.tasks = m
          this.getStatisticForEachTagValue(m, 'type', 'COUNT')
            .then(m => this.tasksCount = m)
            .catch(() => {
            })
          this.getStatisticForEachTagValue(m, 'type', 'TOTAL_TIME')
            .then(m => this.tasksTotalTime = m)
            .catch(() => {
            })
        })
        .catch(() => {
        })

      this.$talesMetrics.getMetric('tales.series')
        .then(m => {
            this.series = m
            this.getStatisticForEachTagValue(m, 'library', 'VALUE', this.getLibraryNameById)
              .then(v => this.seriesAllTags = v)
          },
        )
        .catch(() => {
        })

      this.$talesMetrics.getMetric('tales.books')
        .then(m => {
            this.books = m
            this.getStatisticForEachTagValue(m, 'library', 'VALUE', this.getLibraryNameById)
              .then(v => this.booksAllTags = v)
          },
        )
        .catch(() => {
        })

      this.$talesMetrics.getMetric('tales.books.filesize')
        .then(m => {
            this.booksFileSize = m
            this.getStatisticForEachTagValue(m, 'library', 'VALUE', this.getLibraryNameById)
              .then(v => this.fileSizeAllTags = v)
          },
        )
        .catch(() => {
        })

      this.$talesMetrics.getMetric('tales.sidecars')
        .then(m => {
            this.sidecars = m
            this.getStatisticForEachTagValue(m, 'library', 'VALUE', this.getLibraryNameById)
              .then(v => this.sidecarsAllTags = v)
          },
        )
        .catch(() => {
        })

      this.$talesMetrics.getMetric('tales.collections')
        .then(m => this.collections = m)
        .catch(() => {
        })

      this.$talesMetrics.getMetric('tales.readlists')
        .then(m => this.readlists = m)
        .catch(() => {
        })

      this.$talesMetrics.getMetric('tales.users')
        .then(m => this.users = m)
        .catch(() => {
        })
    },
    async getStatisticForEachTagValue(metric: MetricDto, tag: string, statistic: string = 'VALUE', tagTransform: (t: string) => string = x => x): Promise<{
      [key: string]: number | undefined
    } | undefined> {
      const tagDto = metric.availableTags.find(x => x.tag === tag)
      if (tagDto) {
        const tagToStatistic = tagDto.values.reduce((a, b) => {
          a[b] = 0
          return a
        }, {} as { [key: string]: number | undefined })

        for (let tagKey in tagToStatistic) {
          tagToStatistic[tagTransform(tagKey)] = (await this.$talesMetrics.getMetric(metric.name, [{
            key: tag,
            value: tagKey,
          }])).measurements.find(x => x.statistic === statistic)?.value
        }
        return this.$_.cloneDeep(tagToStatistic)
      }
      return undefined
    },
  },
})
</script>

<style scoped>

</style>
