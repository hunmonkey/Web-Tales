export type ShelfViewCanvasImageProcessingMode =
  | 'SHARPEN'
  | 'WEBGL_SHARPEN'
  | 'BACKGROUND_CLEANUP'
  | 'ADAPTIVE_MONO'

export const SHELFVIEW_CANVAS_IMAGE_MAX_PIXELS = 12000000

/**
 * The 2D filters run a per-pixel convolution on the main thread, so a page near
 * the overall budget freezes the tab for hundreds of milliseconds on every page
 * turn. Past this size the original image is shown untouched instead: a page
 * that arrives instantly beats a sharpened one that stutters. The GPU path keeps
 * the larger budget because it does not block the main thread.
 */
export const SHELFVIEW_CANVAS_CPU_MAX_PIXELS = 4000000

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

export function shelfViewBackgroundCleanupImageData(imageData: ImageData): ImageData {
  const {data} = imageData

  for (let index = 0; index < data.length; index += 4) {
    const red = data[index]
    const green = data[index + 1]
    const blue = data[index + 2]
    const luminance = weightedLuminance(red, green, blue)
    const channelSpread = Math.max(red, green, blue) - Math.min(red, green, blue)

    if (luminance > 214 && channelSpread < 34) {
      data[index] = 255
      data[index + 1] = 255
      data[index + 2] = 255
    } else if (luminance < 44) {
      data[index] = clampPixel(red * 0.72)
      data[index + 1] = clampPixel(green * 0.72)
      data[index + 2] = clampPixel(blue * 0.72)
    } else {
      data[index] = clampPixel((red - 128) * 1.16 + 132)
      data[index + 1] = clampPixel((green - 128) * 1.16 + 132)
      data[index + 2] = clampPixel((blue - 128) * 1.16 + 132)
    }
  }

  return imageData
}

export function shelfViewAdaptiveMonochromeImageData(imageData: ImageData): ImageData {
  const {width, height, data} = imageData
  const source = new Uint8ClampedArray(data)
  const tileSize = 24

  for (let tileY = 0; tileY < height; tileY += tileSize) {
    for (let tileX = 0; tileX < width; tileX += tileSize) {
      const maxY = Math.min(tileY + tileSize, height)
      const maxX = Math.min(tileX + tileSize, width)
      const threshold = tileThreshold(source, width, tileX, tileY, maxX, maxY)

      for (let y = tileY; y < maxY; y++) {
        for (let x = tileX; x < maxX; x++) {
          const index = (y * width + x) * 4
          const luminance = weightedLuminance(source[index], source[index + 1], source[index + 2])
          const value = luminance < threshold ? 0 : 255
          data[index] = value
          data[index + 1] = value
          data[index + 2] = value
          data[index + 3] = source[index + 3]
        }
      }
    }
  }

  return imageData
}

export function shelfViewRenderWebglSharpen(
  canvas: HTMLCanvasElement,
  image: TexImageSource,
  width: number,
  height: number,
): boolean {
  const gl = (canvas.getContext('webgl') || canvas.getContext('experimental-webgl')) as WebGLRenderingContext | null
  if (!gl) return false

  const vertexShader = compileShader(gl, gl.VERTEX_SHADER, `
    attribute vec2 a_position;
    attribute vec2 a_texCoord;
    varying vec2 v_texCoord;
    void main() {
      gl_Position = vec4(a_position, 0.0, 1.0);
      v_texCoord = a_texCoord;
    }
  `)
  const fragmentShader = compileShader(gl, gl.FRAGMENT_SHADER, `
    precision mediump float;
    uniform sampler2D u_image;
    uniform vec2 u_texel;
    varying vec2 v_texCoord;
    void main() {
      vec4 center = texture2D(u_image, v_texCoord) * 5.0;
      vec4 top = texture2D(u_image, v_texCoord + vec2(0.0, -u_texel.y));
      vec4 bottom = texture2D(u_image, v_texCoord + vec2(0.0, u_texel.y));
      vec4 left = texture2D(u_image, v_texCoord + vec2(-u_texel.x, 0.0));
      vec4 right = texture2D(u_image, v_texCoord + vec2(u_texel.x, 0.0));
      gl_FragColor = vec4(clamp((center - top - bottom - left - right).rgb, 0.0, 1.0), 1.0);
    }
  `)
  if (!vertexShader || !fragmentShader) return false

  const program = gl.createProgram()
  if (!program) return false
  gl.attachShader(program, vertexShader)
  gl.attachShader(program, fragmentShader)
  gl.linkProgram(program)
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) return false

  canvas.width = width
  canvas.height = height
  gl.viewport(0, 0, width, height)
  gl.useProgram(program)

  const positionLocation = gl.getAttribLocation(program, 'a_position')
  const texCoordLocation = gl.getAttribLocation(program, 'a_texCoord')
  const texelLocation = gl.getUniformLocation(program, 'u_texel')
  bindFloatBuffer(gl, positionLocation, [-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1])
  bindFloatBuffer(gl, texCoordLocation, [0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0])

  const texture = gl.createTexture()
  if (!texture) return false
  gl.bindTexture(gl.TEXTURE_2D, texture)
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE)
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE)
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR)
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR)
  gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image)
  gl.uniform2f(texelLocation, 1 / width, 1 / height)
  gl.drawArrays(gl.TRIANGLES, 0, 6)

  gl.deleteTexture(texture)
  gl.deleteProgram(program)
  gl.deleteShader(vertexShader)
  gl.deleteShader(fragmentShader)
  return true
}

function tileThreshold(
  source: Uint8ClampedArray,
  width: number,
  minX: number,
  minY: number,
  maxX: number,
  maxY: number,
): number {
  let sum = 0
  let count = 0

  for (let y = minY; y < maxY; y++) {
    for (let x = minX; x < maxX; x++) {
      const index = (y * width + x) * 4
      sum += weightedLuminance(source[index], source[index + 1], source[index + 2])
      count++
    }
  }

  return Math.max(48, Math.min(220, sum / Math.max(1, count) - 8))
}

function weightedLuminance(
  red: number,
  green: number,
  blue: number,
): number {
  return red * 0.299 + green * 0.587 + blue * 0.114
}

function compileShader(
  gl: WebGLRenderingContext,
  type: number,
  source: string,
): WebGLShader | undefined {
  const shader = gl.createShader(type)
  if (!shader) return undefined
  gl.shaderSource(shader, source)
  gl.compileShader(shader)
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    gl.deleteShader(shader)
    return undefined
  }
  return shader
}

function bindFloatBuffer(
  gl: WebGLRenderingContext,
  location: number,
  values: number[],
) {
  const buffer = gl.createBuffer()
  if (!buffer) return
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(values), gl.STATIC_DRAW)
  gl.enableVertexAttribArray(location)
  gl.vertexAttribPointer(location, 2, gl.FLOAT, false, 0, 0)
}

function clampPixel(value: number): number {
  return Math.max(0, Math.min(255, Math.round(value)))
}
