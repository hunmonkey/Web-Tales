import {
  SHELFVIEW_IMAGE_FILTER_PRESETS,
  ShelfViewImageFilterPreset,
  shelfViewImageFilterPreset,
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

  it('falls back to original for unknown preset values', () => {
    expect(shelfViewImageFilterPreset('not-real' as any).id).toBe('ORIGINAL')
  })

  it('marks only the canvas sharpen preset as canvas-backed', () => {
    expect(shelfViewImageFilterUsesCanvas('CANVAS_SHARPEN')).toBe(true)
    expect(shelfViewImageFilterUsesCanvas('IMAGE_ENHANCE')).toBe(false)
  })
})
