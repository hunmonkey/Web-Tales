import {
  shelfViewCanvasImageWithinPixelBudget,
  shelfViewSharpenImageData,
} from '@/shelfview/functions/canvas-image-processing'

describe('ShelfView canvas image processing', () => {
  it('rejects invalid or over-budget canvas sizes', () => {
    expect(shelfViewCanvasImageWithinPixelBudget(100, 100, 10000)).toBe(true)
    expect(shelfViewCanvasImageWithinPixelBudget(101, 100, 10000)).toBe(false)
    expect(shelfViewCanvasImageWithinPixelBudget(0, 100, 10000)).toBe(false)
    expect(shelfViewCanvasImageWithinPixelBudget(Number.NaN, 100, 10000)).toBe(false)
  })

  it('sharpens inner pixels and preserves image edges', () => {
    const imageData = {
      width: 3,
      height: 3,
      data: new Uint8ClampedArray([
        10, 10, 10, 255,
        80, 80, 80, 255,
        10, 10, 10, 255,
        80, 80, 80, 255,
        100, 100, 100, 128,
        80, 80, 80, 255,
        10, 10, 10, 255,
        80, 80, 80, 255,
        10, 10, 10, 255,
      ]),
    } as ImageData

    shelfViewSharpenImageData(imageData)

    expect(Array.from(imageData.data.slice(0, 4))).toEqual([10, 10, 10, 255])
    expect(Array.from(imageData.data.slice(16, 20))).toEqual([180, 180, 180, 128])
  })
})
