import {
  ShelfViewCanvasImageProcessingMode,
  shelfViewAdaptiveMonochromeImageData,
  shelfViewBackgroundCleanupImageData,
  shelfViewSharpenImageData,
} from '@/shelfview/functions/canvas-image-processing'

export interface ShelfViewCanvasFilterRequest {
  token: number
  mode: ShelfViewCanvasImageProcessingMode
  width: number
  height: number
  pixels: ArrayBuffer
}

export interface ShelfViewCanvasFilterResponse {
  token: number
  pixels?: ArrayBuffer
  error?: string
}

const worker = self as unknown as Worker

worker.addEventListener('message', (event: MessageEvent<ShelfViewCanvasFilterRequest>) => {
  const {token, mode, width, height, pixels} = event.data

  try {
    const imageData = new ImageData(new Uint8ClampedArray(pixels), width, height)

    switch (mode) {
      case 'BACKGROUND_CLEANUP':
        shelfViewBackgroundCleanupImageData(imageData)
        break
      case 'ADAPTIVE_MONO':
        shelfViewAdaptiveMonochromeImageData(imageData)
        break
      // WEBGL_SHARPEN only reaches here when the GPU path was unavailable, and
      // its CPU equivalent is the same convolution SHARPEN runs.
      case 'WEBGL_SHARPEN':
      case 'SHARPEN':
        shelfViewSharpenImageData(imageData)
        break
    }

    const processed = imageData.data.buffer
    worker.postMessage({token, pixels: processed} as ShelfViewCanvasFilterResponse, [processed])
  } catch (error) {
    worker.postMessage({token, error: String(error)} as ShelfViewCanvasFilterResponse)
  }
})
