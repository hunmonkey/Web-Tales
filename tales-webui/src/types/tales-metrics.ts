export interface Tag {
  key: string,
  value: string,
}

export interface MetricDto {
  name: string,
  description?: string,
  baseUnit?: string,
  measurements: MeasurementDto[],
  availableTags: TagDto[],
}

export interface MeasurementDto {
  statistic: string,
  value: number,
}

export interface TagDto {
  tag: string,
  values: string[],
}

export interface MetricsSummaryDto {
  totals: MetricsTotalsDto,
  perLibrary: MetricsPerLibraryDto,
  tasks: MetricsTasksDto,
  readProgressIncluded: boolean,
}

export interface MetricsTotalsDto {
  libraries: number,
  series: number,
  books: number,
  users: number,
  collections: number,
  readlists: number,
  sidecars: number,
  booksFileSize: number,
}

export interface MetricsPerLibraryDto {
  series: Record<string, number>,
  books: Record<string, number>,
  sidecars: Record<string, number>,
  booksFileSize: Record<string, number>,
}

export interface MetricsTasksDto {
  executed: Record<string, number>,
  totalTimeSeconds: Record<string, number>,
}
