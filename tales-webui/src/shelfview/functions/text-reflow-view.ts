export type ShelfViewTextTheme =
  | 'PAPER'
  | 'DARK'
  | 'OLED'

export type ShelfViewTextFontFamily =
  | 'PRETENDARD'
  | 'SYSTEM'
  | 'GOWUN_DODUM'
  | 'GOWUN_BATANG'
  | 'READABLE_KR'
  | 'SERIF'
  | 'SANS'
  | 'MONO'

export type ShelfViewTextAlign =
  | 'LEFT'
  | 'JUSTIFY'

export interface ShelfViewTextThemeDefinition {
  value: ShelfViewTextTheme,
  label: string,
  icon: string,
  className: string,
}

export interface ShelfViewTextFontFamilyDefinition {
  value: ShelfViewTextFontFamily,
  label: string,
  cssValue: string,
}

export interface ShelfViewTextAlignDefinition {
  value: ShelfViewTextAlign,
  label: string,
  cssValue: string,
}

export const SHELFVIEW_TEXT_THEMES: ShelfViewTextTheme[] = ['PAPER', 'DARK', 'OLED']
export const SHELFVIEW_TEXT_FONT_FAMILIES: ShelfViewTextFontFamily[] = ['PRETENDARD', 'SYSTEM', 'GOWUN_DODUM', 'GOWUN_BATANG', 'READABLE_KR', 'SERIF', 'SANS', 'MONO']
export const SHELFVIEW_TEXT_ALIGNS: ShelfViewTextAlign[] = ['LEFT', 'JUSTIFY']

const TEXT_THEME_DEFINITIONS: Record<ShelfViewTextTheme, ShelfViewTextThemeDefinition> = {
  PAPER: {
    value: 'PAPER',
    label: 'Paper text theme',
    icon: 'mdi-file-document-outline',
    className: 'shelfview-reader-shell__text--paper',
  },
  DARK: {
    value: 'DARK',
    label: 'Dark text theme',
    icon: 'mdi-theme-light-dark',
    className: 'shelfview-reader-shell__text--dark',
  },
  OLED: {
    value: 'OLED',
    label: 'OLED text theme',
    icon: 'mdi-invert-colors',
    className: 'shelfview-reader-shell__text--oled',
  },
}

const TEXT_FONT_FAMILY_DEFINITIONS: Record<ShelfViewTextFontFamily, ShelfViewTextFontFamilyDefinition> = {
  PRETENDARD: {
    value: 'PRETENDARD',
    label: 'Pretendard',
    cssValue: 'Pretendard, "Apple SD Gothic Neo", "Malgun Gothic", sans-serif',
  },
  SYSTEM: {
    value: 'SYSTEM',
    label: 'System',
    cssValue: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
  },
  GOWUN_DODUM: {
    value: 'GOWUN_DODUM',
    label: 'Gowun Dodum',
    cssValue: '"ShelfView Gowun Dodum", "Malgun Gothic", "Apple SD Gothic Neo", sans-serif',
  },
  GOWUN_BATANG: {
    value: 'GOWUN_BATANG',
    label: 'Gowun Batang',
    cssValue: '"ShelfView Gowun Batang", "Noto Serif KR", serif',
  },
  READABLE_KR: {
    value: 'READABLE_KR',
    label: 'Korean readable',
    cssValue: '"Malgun Gothic", "Apple SD Gothic Neo", "Noto Sans KR", sans-serif',
  },
  SERIF: {
    value: 'SERIF',
    label: 'Serif',
    cssValue: 'Georgia, "Times New Roman", "Noto Serif KR", serif',
  },
  SANS: {
    value: 'SANS',
    label: 'Sans',
    cssValue: 'Roboto, Arial, "Noto Sans KR", sans-serif',
  },
  MONO: {
    value: 'MONO',
    label: 'Mono',
    cssValue: '"SFMono-Regular", Consolas, "Liberation Mono", monospace',
  },
}

const TEXT_ALIGN_DEFINITIONS: Record<ShelfViewTextAlign, ShelfViewTextAlignDefinition> = {
  LEFT: {
    value: 'LEFT',
    label: 'Left',
    cssValue: 'left',
  },
  JUSTIFY: {
    value: 'JUSTIFY',
    label: 'Justify',
    cssValue: 'justify',
  },
}

export function shelfViewTextTheme(theme: ShelfViewTextTheme): ShelfViewTextThemeDefinition {
  return TEXT_THEME_DEFINITIONS[theme]
}

export function shelfViewNextTextTheme(theme: ShelfViewTextTheme): ShelfViewTextTheme {
  const currentIndex = SHELFVIEW_TEXT_THEMES.indexOf(theme)
  return SHELFVIEW_TEXT_THEMES[(currentIndex + 1) % SHELFVIEW_TEXT_THEMES.length]
}

export function shelfViewTextFontFamily(fontFamily: ShelfViewTextFontFamily): ShelfViewTextFontFamilyDefinition {
  return TEXT_FONT_FAMILY_DEFINITIONS[fontFamily]
}

export function shelfViewTextAlign(textAlign: ShelfViewTextAlign): ShelfViewTextAlignDefinition {
  return TEXT_ALIGN_DEFINITIONS[textAlign]
}
