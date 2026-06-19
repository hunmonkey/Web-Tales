export type ShelfViewTextTheme =
  | 'PAPER'
  | 'DARK'
  | 'OLED'

export interface ShelfViewTextThemeDefinition {
  value: ShelfViewTextTheme,
  label: string,
  icon: string,
  className: string,
}

export const SHELFVIEW_TEXT_THEMES: ShelfViewTextTheme[] = ['PAPER', 'DARK', 'OLED']

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

export function shelfViewTextTheme(theme: ShelfViewTextTheme): ShelfViewTextThemeDefinition {
  return TEXT_THEME_DEFINITIONS[theme]
}

export function shelfViewNextTextTheme(theme: ShelfViewTextTheme): ShelfViewTextTheme {
  const currentIndex = SHELFVIEW_TEXT_THEMES.indexOf(theme)
  return SHELFVIEW_TEXT_THEMES[(currentIndex + 1) % SHELFVIEW_TEXT_THEMES.length]
}
