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
