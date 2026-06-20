import {ShelfViewFixedPageBackground, ShelfViewFixedPageFit, ShelfViewFixedPageLayout, ShelfViewFixedPageReadingDirection, ShelfViewFixedPageRotation} from '@/shelfview/functions/fixed-page-view'
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

export const SHELFVIEW_HIGHLIGHT_SELECTOR_SCHEMA_VERSION = 1

export type ShelfViewHighlightColor =
  | 'YELLOW'
  | 'GREEN'
  | 'BLUE'
  | 'PINK'
  | 'UNDERLINE'

export interface ShelfViewTextRangeHighlightSelector {
  schemaVersion: typeof SHELFVIEW_HIGHLIGHT_SELECTOR_SCHEMA_VERSION,
  kind: 'TEXT_RANGE',
  startParagraph: number,
  startCharacterOffset: number,
  endParagraph: number,
  endCharacterOffset: number,
  normalizationVersion: number,
}

export interface ShelfViewMixedTextRangeHighlightSelector {
  schemaVersion: typeof SHELFVIEW_HIGHLIGHT_SELECTOR_SCHEMA_VERSION,
  kind: 'MIXED_TEXT_RANGE',
  itemId: string,
  itemIndex: number,
  itemSelector: ShelfViewTextRangeHighlightSelector,
}

export type ShelfViewHighlightSelector =
  | ShelfViewTextRangeHighlightSelector
  | ShelfViewMixedTextRangeHighlightSelector

export interface ShelfViewHighlightDto {
  id: string,
  bookId: string,
  locator: ShelfViewLocator,
  selector: ShelfViewHighlightSelector,
  contentFingerprint: string,
  color: ShelfViewHighlightColor,
  note?: string,
  textQuote?: string,
  createdAt: string,
  updatedAt: string,
}

export interface ShelfViewHighlightUpdateDto {
  locator: ShelfViewLocator,
  selector: ShelfViewHighlightSelector,
  color?: ShelfViewHighlightColor,
  note?: string,
  textQuote?: string,
}

export interface ShelfViewFixedPagePreferenceSettings {
  layout?: ShelfViewFixedPageLayout,
  readingDirection?: ShelfViewFixedPageReadingDirection,
  pageFit?: ShelfViewFixedPageFit,
  imageFilterPreset?: ShelfViewImageFilterPreset,
  fixedPageRotation?: ShelfViewFixedPageRotation,
  fixedPageCoverSingle?: boolean,
  fixedPageScale?: number,
  fixedPageGap?: number,
  fixedPageMargin?: number,
  fixedPageBackground?: ShelfViewFixedPageBackground,
  fixedPageBrightness?: number,
  fixedPageContrast?: number,
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
  textBrightness?: number,
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
