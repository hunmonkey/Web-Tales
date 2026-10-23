import {
  shelfViewAdaptiveMonochromeImageData,
  shelfViewBackgroundCleanupImageData,
  shelfViewRenderWebglSharpen,
  shelfViewSharpenImageData,
} from '@/shelfview/functions/canvas-image-processing'

describe('ShelfView canvas image processing', () => {
  it('draws the WebGL sharpened page upright', () => {
    const UNPACK_FLIP_Y_WEBGL = 0x9240
    const calls: Array<[string, unknown[]]> = []
    const gl = new Proxy({UNPACK_FLIP_Y_WEBGL} as Record<string, unknown>, {
      get: (target, name: string) => name in target
        ? target[name]
        : (...args: unknown[]) => {
          calls.push([name, args])
          return {}
        },
    })
    const canvas = {getContext: () => gl} as unknown as HTMLCanvasElement

    expect(shelfViewRenderWebglSharpen(canvas, {} as TexImageSource, 4, 2)).toBe(true)

    // The texture coordinates already put the image's first row (v = 0) at the
    // top of the screen (y = +1); flipping on upload as well turns it over.
    const [positions, texCoords] = calls.filter(([name]) => name === 'bufferData').map(([, args]) => Array.from(args[1] as Float32Array))
    const topLeft = [0, 1, 2, 3, 4, 5].find(vertex => positions[vertex * 2] === -1 && positions[vertex * 2 + 1] === 1)!
    expect(texCoords.slice(topLeft * 2, topLeft * 2 + 2)).toEqual([0, 0])
    expect(calls).not.toContainEqual(['pixelStorei', [UNPACK_FLIP_Y_WEBGL, true]])
  })

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
