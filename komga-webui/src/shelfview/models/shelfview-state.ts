import {ShelfViewFixedPageFit, ShelfViewFixedPageLayout, ShelfViewFixedPageReadingDirection} from '@/shelfview/functions/fixed-page-view'
import {ShelfViewImageFilterPreset} from '@/shelfview/functions/image-filter-presets'
import {ShelfViewLocator} from '@/shelfview/models/shelfview-locator'

export type ShelfViewPreferenceScopeType =
  | 'GLOBAL'
  | 'LIBRARY'
  | 'SERIES'
  | 'BOOK'

export type ShelfViewPreferenceFormat =
  | 'FIXED_PAGE_SEQUENCE'

export interface ShelfViewPositionDto {
  bookId: string,
  locator: ShelfViewLocator,
  progression: number,
  contentFingerprint: string,
  updatedAt: string,
}

export interface ShelfViewPositionUpdateDto {
  locator: ShelfViewLocator,
  progression: number,
}

export interface ShelfViewBookmarkDto {
  id: string,
  bookId: string,
  locator: ShelfViewLocator,
  label?: string,
  note?: string,
  createdAt: string,
  updatedAt: string,
}

export interface ShelfViewBookmarkUpdateDto {
  locator: ShelfViewLocator,
  label?: string,
  note?: string,
}

export interface ShelfViewFixedPagePreferenceSettings {
  layout?: ShelfViewFixedPageLayout,
  readingDirection?: ShelfViewFixedPageReadingDirection,
  pageFit?: ShelfViewFixedPageFit,
  imageFilterPreset?: ShelfViewImageFilterPreset,
}

export interface ShelfViewPreferenceDto {
  scopeType: ShelfViewPreferenceScopeType,
  scopeId: string,
  format: ShelfViewPreferenceFormat,
  settings: ShelfViewFixedPagePreferenceSettings,
  updatedAt: string,
}

export interface ShelfViewPreferenceResponseDto {
  scopeType: ShelfViewPreferenceScopeType,
  scopeId: string,
  format: ShelfViewPreferenceFormat,
  settings?: ShelfViewFixedPagePreferenceSettings,
  updatedAt?: string,
  effectiveSettings: ShelfViewFixedPagePreferenceSettings,
  sources: ShelfViewPreferenceDto[],
}

export interface ShelfViewPreferenceQuery {
  scopeType: ShelfViewPreferenceScopeType,
  scopeId: string,
  format: ShelfViewPreferenceFormat,
}

export interface ShelfViewPreferenceUpdateDto extends ShelfViewPreferenceQuery {
  settings: ShelfViewFixedPagePreferenceSettings,
}
