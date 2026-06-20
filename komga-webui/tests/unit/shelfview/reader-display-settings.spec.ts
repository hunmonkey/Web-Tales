import {
  SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS,
  SHELFVIEW_TEXT_DISPLAY_DEFAULTS,
  shelfViewNormalizeFixedPageDisplaySettings,
  shelfViewNormalizeFixedPageScale,
  shelfViewNormalizeTextDisplaySettings,
} from '@/shelfview/functions/reader-display-settings'

describe('ShelfView reader display settings', () => {
  it('normalizes fixed-page display settings to supported ranges', () => {
    expect(shelfViewNormalizeFixedPageDisplaySettings({
      fixedPageRotation: 45 as any,
      fixedPageCoverSingle: true,
      fixedPageScale: 347,
      fixedPageGap: 17,
      fixedPageMargin: 43,
      fixedPageBackground: 'BLUE' as any,
      fixedPageBrightness: 1.333,
      fixedPageContrast: 0.7,
    })).toEqual({
      fixedPageRotation: 0,
      fixedPageCoverSingle: true,
      fixedPageScale: 300,
      fixedPageGap: 18,
      fixedPageMargin: 44,
      fixedPageBackground: SHELFVIEW_FIXED_PAGE_DISPLAY_DEFAULTS.fixedPageBackground,
      fixedPageBrightness: 1.2,
      fixedPageContrast: 0.85,
    })
  })

  it('rounds fixed-page scale to the nearest safe step', () => {
    expect(shelfViewNormalizeFixedPageScale(127)).toBe(125)
    expect(shelfViewNormalizeFixedPageScale(Number.NaN)).toBe(50)
  })

  it('normalizes text display settings to supported ranges', () => {
    expect(shelfViewNormalizeTextDisplaySettings({
      textTheme: 'NEON' as any,
      textFontFamily: 'UNREADABLE' as any,
      textFontSize: 40,
      textLineHeight: 1.2,
      textLetterSpacing: 2.56,
      textWordSpacing: 9,
      textContentWidth: 500,
      textPageMargin: 4,
      textParagraphSpacing: 3,
      textAlign: 'CENTER' as any,
      textFontWeight: 376,
      textBrightness: 0.42,
    })).toEqual({
      textTheme: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textTheme,
      textFontFamily: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textFontFamily,
      textFontSize: 32,
      textLineHeight: 1.35,
      textLetterSpacing: 2.5,
      textWordSpacing: 8,
      textContentWidth: 520,
      textPageMargin: 16,
      textParagraphSpacing: 2.6,
      textAlign: SHELFVIEW_TEXT_DISPLAY_DEFAULTS.textAlign,
      textFontWeight: 400,
      textBrightness: 0.8,
    })
  })
})
