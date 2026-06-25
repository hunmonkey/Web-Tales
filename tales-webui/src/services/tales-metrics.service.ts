import {AxiosInstance} from 'axios'
import {MetricDto, MetricsSummaryDto, Tag} from '@/types/tales-metrics'

const qs = require('qs')

const API_METRICS = '/actuator/metrics'
const API_METRICS_SUMMARY = '/api/v2/metrics/summary'

export default class TalesMetricsService {
  private http: AxiosInstance

  constructor(http: AxiosInstance) {
    this.http = http
  }

  async getMetric(metric: string, tags?: Tag[]): Promise<MetricDto> {
    try {
      const params = {} as any
      if (tags) params.tag = TalesMetricsService.tagQuery(tags)

      return (await this.http.get(`${API_METRICS}/${metric}`, {
        params: params,
        paramsSerializer: params => qs.stringify(params, {indices: false}),
      })).data
    } catch (e) {
      let msg = `An error occurred while trying to retrieve metric: ${metric}, tags: ${tags}`
      if (e.response.data.message) {
        msg += `: ${e.response.data.message}`
      }
      throw new Error(msg)
    }
  }

  async getSummary(): Promise<MetricsSummaryDto> {
    return (await this.http.get(API_METRICS_SUMMARY)).data
  }

  private static tagQuery(tags: Tag[]): string[] {
    return tags.map(t => `${t.key}:${t.value}`)
  }
}
