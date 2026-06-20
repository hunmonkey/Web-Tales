export type ShelfViewCanvasImageProcessingMode = 'SHARPEN'

export const SHELFVIEW_CANVAS_IMAGE_MAX_PIXELS = 12000000

export function shelfViewCanvasImageWithinPixelBudget(
  width: number,
  height: number,
  maxPixels = SHELFVIEW_CANVAS_IMAGE_MAX_PIXELS,
): boolean {
  if (!Number.isFinite(width) || !Number.isFinite(height)) return false
  if (width < 1 || height < 1) return false
  return Math.floor(width) * Math.floor(height) <= maxPixels
}

export function shelfViewSharpenImageData(imageData: ImageData): ImageData {
  const source = new Uint8ClampedArray(imageData.data)
  const {width, height, data} = imageData

  if (width < 3 || height < 3) return imageData

  for (let y = 1; y < height - 1; y++) {
    for (let x = 1; x < width - 1; x++) {
      const index = (y * width + x) * 4
      const top = index - width * 4
      const bottom = index + width * 4
      const left = index - 4
      const right = index + 4

      data[index] = clampPixel(source[index] * 5 - source[top] - source[bottom] - source[left] - source[right])
      data[index + 1] = clampPixel(source[index + 1] * 5 - source[top + 1] - source[bottom + 1] - source[left + 1] - source[right + 1])
      data[index + 2] = clampPixel(source[index + 2] * 5 - source[top + 2] - source[bottom + 2] - source[left + 2] - source[right + 2])
      data[index + 3] = source[index + 3]
    }
  }

  return imageData
}

function clampPixel(value: number): number {
  return Math.max(0, Math.min(255, Math.round(value)))
}
