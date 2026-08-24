import ShelfViewCanvasImagePage from '@/shelfview/components/ShelfViewCanvasImagePage.vue'

describe('ShelfViewCanvasImagePage', () => {
  const originalImage = global.Image

  afterEach(() => {
    global.Image = originalImage
  })

  it('loads images with credentials-enabled CORS for readable canvas pixels when the server allows it', async () => {
    let image: MockImage | undefined
    global.Image = jest.fn(() => {
      image = new MockImage()
      return image as unknown as HTMLImageElement
    }) as unknown as typeof Image

    const loadImage = (ShelfViewCanvasImagePage as any).options.methods.loadImage
    await expect(loadImage('http://api.test/page.svg')).resolves.toBe(image)

    expect(image?.crossOrigin).toBe('use-credentials')
    expect(image?.decoding).toBe('async')
    expect(image?.src).toBe('http://api.test/page.svg')
  })

  it('loads a same-origin page without a CORS mode, so it is the image the prefetch already fetched', async () => {
    let image: MockImage | undefined
    global.Image = jest.fn(() => {
      image = new MockImage()
      return image as unknown as HTMLImageElement
    }) as unknown as typeof Image

    const loadImage = (ShelfViewCanvasImagePage as any).options.methods.loadImage
    // jsdom serves the test document from http://localhost; a same-origin image
    // cannot taint the canvas, so its pixels stay readable without CORS.
    await expect(loadImage('/api/v1/books/book-1/pages/0')).resolves.toBe(image)

    expect(image?.crossOrigin).toBeNull()
  })

  it('cancels active filtering when the page component is destroyed', () => {
    const abort = jest.fn()
    const vm = {
      processingToken: 3,
      filterAbortController: {abort},
    }

    const beforeDestroy = (ShelfViewCanvasImagePage as any).options.beforeDestroy
    const hook = Array.isArray(beforeDestroy) ? beforeDestroy[0] : beforeDestroy
    hook.call(vm)

    expect(vm.processingToken).toBe(4)
    expect(abort).toHaveBeenCalledTimes(1)
  })
})

class MockImage {
  onload: (() => void) | null = null
  onerror: (() => void) | null = null
  crossOrigin: string | null = null
  decoding: string | undefined
  private imageSrc = ''

  set src(value: string) {
    this.imageSrc = value
    setTimeout(() => this.onload?.(), 0)
  }

  get src(): string {
    return this.imageSrc
  }
}
