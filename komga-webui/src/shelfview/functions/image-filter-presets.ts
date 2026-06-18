export type ShelfViewImageFilterPreset =
  | 'ORIGINAL'
  | 'EYE_COMFORT'
  | 'MONO_COMIC'
  | 'LOW_LIGHT'
  | 'NIGHT_INVERT'
  | 'OLED_TEXT'

export interface ShelfViewImageFilterValues {
  brightness: number,
  contrast: number,
  grayscale: number,
  sepia: number,
  invert: number,
}

export interface ShelfViewImageFilterPresetOption {
  id: ShelfViewImageFilterPreset,
  label: string,
  icon: string,
  values: ShelfViewImageFilterValues,
}

export const SHELFVIEW_IMAGE_FILTER_PRESETS: ShelfViewImageFilterPresetOption[] = [
  {
    id: 'ORIGINAL',
    label: 'Original',
    icon: 'mdi-image-outline',
    values: {
      brightness: 1,
      contrast: 1,
      grayscale: 0,
      sepia: 0,
      invert: 0,
    },
  },
  {
    id: 'EYE_COMFORT',
    label: 'Eye comfort',
    icon: 'mdi-eye-outline',
    values: {
      brightness: 0.92,
      contrast: 0.98,
      grayscale: 0,
      sepia: 0.16,
      invert: 0,
    },
  },
  {
    id: 'MONO_COMIC',
    label: 'Mono comic',
    icon: 'mdi-circle-half-full',
    values: {
      brightness: 1,
      contrast: 1.18,
      grayscale: 1,
      sepia: 0,
      invert: 0,
    },
  },
  {
    id: 'LOW_LIGHT',
    label: 'Low light',
    icon: 'mdi-brightness-4',
    values: {
      brightness: 0.78,
      contrast: 0.9,
      grayscale: 0,
      sepia: 0.08,
      invert: 0,
    },
  },
  {
    id: 'NIGHT_INVERT',
    label: 'Night invert',
    icon: 'mdi-theme-light-dark',
    values: {
      brightness: 0.82,
      contrast: 0.95,
      grayscale: 0,
      sepia: 0,
      invert: 1,
    },
  },
  {
    id: 'OLED_TEXT',
    label: 'OLED text',
    icon: 'mdi-television-ambient-light',
    values: {
      brightness: 0.9,
      contrast: 1.12,
      grayscale: 0,
      sepia: 0,
      invert: 1,
    },
  },
]

export function shelfViewImageFilterPreset(
  preset: ShelfViewImageFilterPreset,
): ShelfViewImageFilterPresetOption {
  return SHELFVIEW_IMAGE_FILTER_PRESETS.find(option => option.id === preset) || SHELFVIEW_IMAGE_FILTER_PRESETS[0]
}

export function shelfViewNextImageFilterPreset(
  preset: ShelfViewImageFilterPreset,
): ShelfViewImageFilterPreset {
  const currentIndex = SHELFVIEW_IMAGE_FILTER_PRESETS.findIndex(option => option.id === preset)
  return SHELFVIEW_IMAGE_FILTER_PRESETS[(currentIndex + 1) % SHELFVIEW_IMAGE_FILTER_PRESETS.length].id
}

export function shelfViewImageFilterStyle(
  preset: ShelfViewImageFilterPreset,
): Record<string, string> {
  const values = shelfViewImageFilterPreset(preset).values

  return {
    '--sv-brightness': values.brightness.toString(),
    '--sv-contrast': values.contrast.toString(),
    '--sv-grayscale': values.grayscale.toString(),
    '--sv-sepia': values.sepia.toString(),
    '--sv-invert': values.invert.toString(),
  }
}
