import {
  shelfViewAdaptiveMonochromeImageData,
  shelfViewBackgroundCleanupImageData,
  shelfViewSharpenImageData,
} from '@/shelfview/functions/canvas-image-processing'

describe('ShelfView canvas image processing', () => {
  it('keeps the sharpen filter inside the valid pixel range', () => {
    const imageData = makeImageData(3, 3, [
      80, 80, 80, 255, 80, 80, 80, 255, 80, 80, 80, 255,
      80, 80, 80, 255, 180, 180, 180, 255, 80, 80, 80, 255,
      80, 80, 80, 255, 80, 80, 80, 255, 80, 80, 80, 255,
    ])

    const processed = shelfViewSharpenImageData(imageData)

    expect(processed.data[16]).toBe(255)
    expect(processed.data[19]).toBe(255)
  })

  it('cleans near-white paper background while preserving alpha', () => {
    const imageData = makeImageData(2, 1, [
      226, 224, 222, 128,
      24, 24, 24, 255,
    ])

    const processed = shelfViewBackgroundCleanupImageData(imageData)

    expect(Array.from(processed.data.slice(0, 4))).toEqual([255, 255, 255, 128])
    expect(processed.data[4]).toBeLessThan(24)
    expect(processed.data[7]).toBe(255)
  })

  it('converts local tiles to adaptive monochrome pixels', () => {
    const imageData = makeImageData(2, 1, [
      30, 30, 30, 255,
      230, 230, 230, 255,
    ])

    const processed = shelfViewAdaptiveMonochromeImageData(imageData)

    expect(Array.from(processed.data.slice(0, 4))).toEqual([0, 0, 0, 255])
    expect(Array.from(processed.data.slice(4, 8))).toEqual([255, 255, 255, 255])
  })
})

function makeImageData(
  width: number,
  height: number,
  values: number[],
): ImageData {
  return {
    width,
    height,
    data: new Uint8ClampedArray(values),
  } as ImageData
}
