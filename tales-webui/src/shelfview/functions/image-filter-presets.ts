import {ShelfViewCanvasImageProcessingMode} from '@/shelfview/functions/canvas-image-processing'

export type ShelfViewImageFilterPreset =
  | 'ORIGINAL'
  | 'IMAGE_ENHANCE'
  | 'FADED_SCAN'
  | 'CANVAS_SHARPEN'
  | 'WEBGL_SHARPEN'
  | 'BACKGROUND_CLEANUP'
  | 'ADAPTIVE_MONO'
  | 'EYE_COMFORT'
  | 'MONO_COMIC'
  | 'LOW_LIGHT'
  | 'NIGHT_INVERT'
  | 'OLED_TEXT'

export interface ShelfViewImageFilterValues {
  brightness: number,
  contrast: number,
  saturate: number,
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
      saturate: 1,
      grayscale: 0,
      sepia: 0,
      invert: 0,
    },
  },
  {
    id: 'IMAGE_ENHANCE',
    label: 'Image enhance',
    icon: 'mdi-auto-fix',
    values: {
      brightness: 1.04,
      contrast: 1.28,
      saturate: 1.08,
      grayscale: 0,
      sepia: 0,
      invert: 0,
    },
  },
  {
    id: 'FADED_SCAN',
    label: 'Faded scan boost',
    icon: 'mdi-image-filter-center-focus-strong-outline',
    values: {
      brightness: 1.08,
      contrast: 1.42,
      saturate: 0.92,
      grayscale: 0,
      sepia: 0,
      invert: 0,
    },
  },
  {
    id: 'CANVAS_SHARPEN',
    label: 'Canvas sharpen prototype',
    icon: 'mdi-vector-square',
    values: {
      brightness: 1,
      contrast: 1,
      saturate: 1,
      grayscale: 0,
      sepia: 0,
      invert: 0,
    },
  },
  {
    id: 'WEBGL_SHARPEN',
    label: 'WebGL sharpen',
    icon: 'mdi-vector-polyline',
    values: {
      brightness: 1,
      contrast: 1,
      saturate: 1,
      grayscale: 0,
      sepia: 0,
      invert: 0,
    },
  },
  {
    id: 'BACKGROUND_CLEANUP',
    label: 'Background cleanup',
    icon: 'mdi-eraser',
    values: {
      brightness: 1,
      contrast: 1,
      saturate: 1,
      grayscale: 0,
      sepia: 0,
      invert: 0,
    },
  },
  {
    id: 'ADAPTIVE_MONO',
    label: 'Adaptive monochrome',
    icon: 'mdi-contrast-circle',
    values: {
      brightness: 1,
      contrast: 1,
      saturate: 1,
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
      saturate: 0.94,
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
      saturate: 1,
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
      saturate: 0.9,
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
      saturate: 0.86,
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
      saturate: 1,
      grayscale: 0,
      sepia: 0,
      invert: 1,
    },
  },
]

export const SHELFVIEW_IMAGE_FILTER_PRIMARY: ShelfViewImageFilterPreset[] = [
  'ORIGINAL',
  'IMAGE_ENHANCE',
  'MONO_COMIC',
  'NIGHT_INVERT',
]

export const SHELFVIEW_IMAGE_FILTER_PRIMARY_PRESETS: ShelfViewImageFilterPresetOption[] =
  SHELFVIEW_IMAGE_FILTER_PRESETS.filter(option => SHELFVIEW_IMAGE_FILTER_PRIMARY.includes(option.id))

export function shelfViewImageFilterPreset(
  preset: ShelfViewImageFilterPreset,
): ShelfViewImageFilterPresetOption {
  return SHELFVIEW_IMAGE_FILTER_PRESETS.find(option => option.id === preset) || SHELFVIEW_IMAGE_FILTER_PRESETS[0]
}

export function shelfViewImageFilterPrimaryPreset(
  preset: ShelfViewImageFilterPreset,
): ShelfViewImageFilterPreset {
  return SHELFVIEW_IMAGE_FILTER_PRIMARY.includes(preset) ? preset : 'ORIGINAL'
}

export function shelfViewImageFilterUsesCanvas(
  preset: ShelfViewImageFilterPreset,
): boolean {
  return shelfViewImageFilterProcessingMode(preset) !== undefined
}

export function shelfViewImageFilterProcessingMode(
  preset: ShelfViewImageFilterPreset,
): ShelfViewCanvasImageProcessingMode | undefined {
  switch (preset) {
    case 'CANVAS_SHARPEN':
      return 'SHARPEN'
    case 'WEBGL_SHARPEN':
      return 'WEBGL_SHARPEN'
    case 'BACKGROUND_CLEANUP':
      return 'BACKGROUND_CLEANUP'
    case 'ADAPTIVE_MONO':
      return 'ADAPTIVE_MONO'
    default:
      return undefined
  }
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
    '--sv-saturate': values.saturate.toString(),
    '--sv-grayscale': values.grayscale.toString(),
    '--sv-sepia': values.sepia.toString(),
    '--sv-invert': values.invert.toString(),
  }
}
