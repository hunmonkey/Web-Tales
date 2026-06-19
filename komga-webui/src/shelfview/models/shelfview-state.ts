import {ShelfViewFixedPageFit, ShelfViewFixedPageLayout, ShelfViewFixedPageReadingDirection} from '@/shelfview/functions/fixed-page-view'
import {ShelfViewImageFilterPreset} from '@/shelfview/functions/image-filter-presets'
import {ShelfViewTextAlign, ShelfViewTextFontFamily, ShelfViewTextTheme} from '@/shelfview/functions/text-reflow-view'
import {ShelfViewLocator} from '@/shelfview/models/shelfview-locator'

export type ShelfViewPreferenceScopeType =
  | 'GLOBAL'
  | 'LIBRARY'
  | 'SERIES'
  | 'BOOK'

export type ShelfViewPreferenceFormat =
  | 'FIXED_PAGE_SEQUENCE'
  | 'TEXT_REFLOW'

export interface ShelfViewPositionDto {
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
  textTheme?: ShelfViewTextTheme,
  textFontFamily?: ShelfViewTextFontFamily,
  textFontSize?: number,
  textLineHeight?: number,
  textLetterSpacing?: number,
  textWordSpacing?: number,
  textContentWidth?: number,
  textPageMargin?: number,
  textParagraphSpacing?: number,
  textAlign?: ShelfViewTextAlign,
  textFontWeight?: number,
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
