import {
  SHELFVIEW_FIXED_PAGE_BACKGROUNDS,
  ShelfViewFixedPageBackground,
  ShelfViewFixedPageRotation,
  normalizeFixedPageRotation,
} from '@/shelfview/functions/fixed-page-view'
import {
  SHELFVIEW_TEXT_ALIGNS,
  SHELFVIEW_TEXT_FONT_FAMILIES,
  SHELFVIEW_TEXT_THEMES,
  ShelfViewTextAlign,
  ShelfViewTextFontFamily,
  ShelfViewTextTheme,
} from '@/shelfview/functions/text-reflow-view'

export interface ShelfViewFixedPageDisplaySettings {
  fixedPageRotation: ShelfViewFixedPageRotation,
  fixedPageCoverSingle: boolean,
  fixedPageScale: number,
  fixedPageGap: number,
  fixedPageMargin: number,
  fixedPageBackground: ShelfViewFixedPageBackground,
  fixedPageBrightness: number,
  fixedPageContrast: number,
}

export interface ShelfViewTextDisplaySettings {
  textTheme: ShelfViewTextTheme,
  textFontFamily: ShelfViewTextFontFamily,
  textFontSize: number,
  textLineHeight: number,
  textLetterSpacing: number,
  textWordSpacing: number,
  textContentWidth: number,
  textPageMargin: number,
  textParagraphSpacing: number,
  textAlign: ShelfViewTextAlign,
  textFontWeight: number,
  textBrightness: number,
}

const FIXED_PAGE_SCALE_MIN = 50
const FIXED_PAGE_SCALE_MAX = 300

export const SHELFVIEW_TEXT_DISPLAY_DEFAULTS: ShelfViewTextDisplaySettings = {
  textTheme: 'PAPER',
  textFontFamily: 'SYSTEM',
  textFontSize: 20,
  textLineHeight: 1.75,
  textLetterSpacing: 0,
  textWordSpacing: 0,
  textContentWidth: 760,
  textPageMargin: 56,
  textParagraphSpacing: 1.35,
  textAlign: 'LEFT',
  textFontWeight: 400,
  textBrightness: 1,
}

export const SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS: ShelfViewFixedPageDisplaySettings = {
  fixedPageRotation: 0,
  fixedPageCoverSingle: false,
  fixedPageScale: 100,
  fixedPageGap: 12,
  fixedPageMargin: 16,
  fixedPageBackground: 'DARK',
  fixedPageBrightness: 1,
  fixedPageContrast: 1,
}

export function shelfViewNormalizeFixedPageDisplaySettings(
  settings: ShelfViewFixedPageDisplaySettings,
): ShelfViewFixedPageDisplaySettings {
  return {
    fixedPageRotation: normalizeFixedPageRotation(settings.fixedPageRotation),
    fixedPageCoverSingle: settings.fixedPageCoverSingle,
    fixedPageScale: shelfViewNormalizeFixedPageScale(settings.fixedPageScale),
    fixedPageGap: Math.round(clampNumber(settings.fixedPageGap, 0, 64) / 2) * 2,
    fixedPageMargin: Math.round(clampNumber(settings.fixedPageMargin, 0, 96) / 4) * 4,
    fixedPageBackground: SHELFVIEW_FIXED_PAGE_BACKGROUNDS.some(option => option.id === settings.fixedPageBackground)
      ? settings.fixedPageBackground
      : SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageBackground,
    fixedPageBrightness: roundNumber(clampNumber(settings.fixedPageBrightness, 0.8, 1.2), 2),
    fixedPageContrast: roundNumber(clampNumber(settings.fixedPageContrast, 0.85, 1.35), 2),
  }
}

export function shelfViewNormalizeFixedPageScale(value: number): number {
  return Math.round(clampNumber(value, FIXED_PAGE_SCALE_MIN, FIXED_PAGE_SCALE_MAX) / 5) * 5
}

export function shelfViewNormalizeTextDisplaySettings(
  settings: ShelfViewTextDisplaySettings,
): ShelfViewTextDisplaySettings {
  return {
    textTheme: SHELFVIEW_TEXT_THEMES.includes(settings.textTheme) ? settings.textTheme : SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textTheme,
    textFontFamily: SHELFVIEW_TEXT_FONT_FAMILIES.includes(settings.textFontFamily) ? settings.textFontFamily : SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textFontFamily,
    textFontSize: Math.round(clampNumber(settings.textFontSize, 14, 32)),
    textLineHeight: roundNumber(clampNumber(settings.textLineHeight, 1.35, 2.4), 2),
    textLetterSpacing: roundNumber(clampNumber(settings.textLetterSpacing, 0, 2.5), 1),
    textWordSpacing: roundNumber(clampNumber(settings.textWordSpacing, 0, 8), 1),
    textContentWidth: Math.round(clampNumber(settings.textContentWidth, 520, 1180)),
    textPageMargin: Math.round(clampNumber(settings.textPageMargin, 16, 120)),
    textParagraphSpacing: roundNumber(clampNumber(settings.textParagraphSpacing, 0.6, 2.6), 2),
    textAlign: SHELFVIEW_TEXT_ALIGNS.includes(settings.textAlign) ? settings.textAlign : SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textAlign,
    textFontWeight: Math.round(clampNumber(settings.textFontWeight, 300, 700) / 50) * 50,
    textBrightness: roundNumber(clampNumber(settings.textBrightness, 0.8, 1.2), 2),
  }
}

function clampNumber(
  value: number,
  min: number,
  max: number,
): number {
  if (!Number.isFinite(value)) return min
  return Math.min(Math.max(value, min), max)
}

function roundNumber(
  value: number,
  digits: number,
): number {
  const multiplier = 10 ** digits
  return Math.round(value * multiplier) / multiplier
}
