import {
  SHELFVIEW_IMAGE_FILTER_PRIMARY,
  SHELFVIEW_IMAGE_FILTER_PRIMARY_PRESETS,
  SHELFVIEW_IMAGE_FILTER_PRESETS,
  ShelfViewImageFilterPreset,
  shelfViewImageFilterPrimaryPreset,
  shelfViewImageFilterPreset,
  shelfViewImageFilterProcessingMode,
  shelfViewImageFilterStyle,
  shelfViewImageFilterUsesCanvas,
  shelfViewNextImageFilterPreset,
} from '@/shelfview/functions/image-filter-presets'

describe('ShelfView image filter presets', () => {
  it('keeps the original preset visually neutral', () => {
    expect(shelfViewImageFilterStyle('ORIGINAL')).toEqual({
      '--sv-brightness': '1',
      '--sv-contrast': '1',
      '--sv-saturate': '1',
      '--sv-grayscale': '0',
      '--sv-sepia': '0',
      '--sv-invert': '0',
    })
  })

  it('defines the planned filter controls in presets', () => {
    expect(shelfViewImageFilterStyle('IMAGE_ENHANCE')).toMatchObject({
      '--sv-contrast': '1.28',
      '--sv-saturate': '1.08',
    })
    expect(shelfViewImageFilterStyle('FADED_SCAN')).toMatchObject({
      '--sv-brightness': '1.08',
      '--sv-contrast': '1.42',
      '--sv-saturate': '0.92',
    })
    expect(shelfViewImageFilterStyle('CANVAS_SHARPEN')).toMatchObject({
      '--sv-brightness': '1',
      '--sv-contrast': '1',
      '--sv-saturate': '1',
    })
    expect(shelfViewImageFilterStyle('WEBGL_SHARPEN')).toMatchObject({
      '--sv-brightness': '1',
      '--sv-contrast': '1',
    })
    expect(shelfViewImageFilterStyle('BACKGROUND_CLEANUP')).toMatchObject({
      '--sv-brightness': '1',
      '--sv-contrast': '1',
    })
    expect(shelfViewImageFilterStyle('ADAPTIVE_MONO')).toMatchObject({
      '--sv-brightness': '1',
      '--sv-contrast': '1',
    })
    expect(shelfViewImageFilterStyle('MONO_COMIC')).toMatchObject({
      '--sv-contrast': '1.18',
      '--sv-grayscale': '1',
    })
    expect(shelfViewImageFilterStyle('NIGHT_INVERT')).toMatchObject({
      '--sv-brightness': '0.82',
      '--sv-invert': '1',
    })
  })

  it('cycles through all presets and wraps back to original', () => {
    let preset: ShelfViewImageFilterPreset = 'ORIGINAL'

    for (let i = 0; i < SHELFVIEW_IMAGE_FILTER_PRESETS.length; i++) {
      preset = shelfViewNextImageFilterPreset(preset)
    }

    expect(preset).toBe('ORIGINAL')
  })

  it('exposes only four primary presets for the simplified menu', () => {
    expect(SHELFVIEW_IMAGE_FILTER_PRIMARY).toEqual(['ORIGINAL', 'IMAGE_ENHANCE', 'MONO_COMIC', 'NIGHT_INVERT'])
    expect(SHELFVIEW_IMAGE_FILTER_PRIMARY_PRESETS.map(option => option.id)).toEqual(SHELFVIEW_IMAGE_FILTER_PRIMARY)
    expect(SHELFVIEW_IMAGE_FILTER_PRIMARY).not.toContain('CANVAS_SHARPEN')
    expect(SHELFVIEW_IMAGE_FILTER_PRIMARY).not.toContain('WEBGL_SHARPEN')
    expect(SHELFVIEW_IMAGE_FILTER_PRIMARY).not.toContain('BACKGROUND_CLEANUP')
    expect(SHELFVIEW_IMAGE_FILTER_PRIMARY).not.toContain('ADAPTIVE_MONO')
    expect(SHELFVIEW_IMAGE_FILTER_PRIMARY.length).toBeLessThanOrEqual(4)
  })

  it('falls back to original for unknown preset values', () => {
    expect(shelfViewImageFilterPreset('not-real' as any).id).toBe('ORIGINAL')
    expect(shelfViewImageFilterPrimaryPreset('CANVAS_SHARPEN')).toBe('ORIGINAL')
  })

  it('maps advanced image processing presets to canvas modes', () => {
    expect(shelfViewImageFilterUsesCanvas('CANVAS_SHARPEN')).toBe(true)
    expect(shelfViewImageFilterUsesCanvas('WEBGL_SHARPEN')).toBe(true)
    expect(shelfViewImageFilterUsesCanvas('BACKGROUND_CLEANUP')).toBe(true)
    expect(shelfViewImageFilterUsesCanvas('ADAPTIVE_MONO')).toBe(true)
    expect(shelfViewImageFilterUsesCanvas('IMAGE_ENHANCE')).toBe(false)
    expect(shelfViewImageFilterProcessingMode('CANVAS_SHARPEN')).toBe('SHARPEN')
    expect(shelfViewImageFilterProcessingMode('WEBGL_SHARPEN')).toBe('WEBGL_SHARPEN')
    expect(shelfViewImageFilterProcessingMode('BACKGROUND_CLEANUP')).toBe('BACKGROUND_CLEANUP')
    expect(shelfViewImageFilterProcessingMode('ADAPTIVE_MONO')).toBe('ADAPTIVE_MONO')
  })
})
