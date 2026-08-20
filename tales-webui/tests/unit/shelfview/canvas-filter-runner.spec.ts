// The factory is mocked rather than stubbed at runtime so Jest never loads the
// real file: it contains `import.meta.url`, which is a syntax error once the
// transform rewrites the module to CommonJS.
jest.mock('@/shelfview/functions/canvas-filter-worker-factory', () => ({
  createCanvasFilterWorker: () => mockCreateCanvasFilterWorker(),
}))

let mockWorker: FakeWorker
let mockCreateCanvasFilterWorker: jest.Mock

describe('ShelfView canvas filter runner', () => {
  const originalWorker = (global as any).Worker
  const originalImageData = (global as any).ImageData

  beforeEach(() => {
    jest.resetModules()
    mockWorker = new FakeWorker()
    mockCreateCanvasFilterWorker = jest.fn(() => mockWorker)
    ;(global as any).ImageData = FakeImageData
  })

  afterEach(() => {
    ;(global as any).Worker = originalWorker
    ;(global as any).ImageData = originalImageData
  })

  it('filters on the main thread when the browser has no workers', async () => {
    delete (global as any).Worker
    const {shelfViewFilterImageData} = require('@/shelfview/functions/canvas-filter-runner')

    const imageData = makeImageData(2, 1, [226, 224, 222, 128, 24, 24, 24, 255])
    const filtered = await shelfViewFilterImageData('BACKGROUND_CLEANUP', imageData)

    expect(Array.from(filtered.data.slice(0, 4))).toEqual([255, 255, 255, 128])
  })

  it('refuses an image over the CPU budget when it has no worker to run it on', async () => {
    delete (global as any).Worker
    const {shelfViewFilterImageData} = require('@/shelfview/functions/canvas-filter-runner')

    // 2500 x 2000 is 5 MP, past SHELFVIEW_CANVAS_CPU_MAX_PIXELS.
    const oversized = {width: 2500, height: 2000, data: new Uint8ClampedArray(8)} as ImageData

    await expect(shelfViewFilterImageData('SHARPEN', oversized)).rejects.toThrow('CPU filter budget')
  })

  it('sends the work to the worker and returns what it sends back', async () => {
    ;(global as any).Worker = FakeWorker
    const {shelfViewFilterImageData} = require('@/shelfview/functions/canvas-filter-runner')

    const imageData = makeImageData(2, 1, [1, 2, 3, 4, 5, 6, 7, 8])
    const filtered = await shelfViewFilterImageData('ADAPTIVE_MONO', imageData)

    expect(mockWorker.received?.mode).toBe('ADAPTIVE_MONO')
    expect(mockWorker.received?.width).toBe(2)
    expect(mockWorker.transferred).toBe(true)
    expect(Array.from(filtered.data)).toEqual([9, 9, 9, 9, 9, 9, 9, 9])
  })

  it('filters an image over the CPU budget when a worker is available', async () => {
    ;(global as any).Worker = FakeWorker
    const {shelfViewFilterImageData} = require('@/shelfview/functions/canvas-filter-runner')

    // The same 5 MP image the synchronous path refuses above.
    const oversized = {width: 2500, height: 2000, data: new Uint8ClampedArray(8)} as ImageData

    await expect(shelfViewFilterImageData('SHARPEN', oversized)).resolves.toBeDefined()
    expect(mockWorker.received?.width).toBe(2500)
  })

  it('shares one worker creation across concurrent first requests', async () => {
    ;(global as any).Worker = FakeWorker
    const {shelfViewFilterImageData} = require('@/shelfview/functions/canvas-filter-runner')

    await Promise.all([
      shelfViewFilterImageData('SHARPEN', makeImageData(2, 1, [1, 2, 3, 4, 5, 6, 7, 8])),
      shelfViewFilterImageData('BACKGROUND_CLEANUP', makeImageData(2, 1, [8, 7, 6, 5, 4, 3, 2, 1])),
    ])

    expect(mockCreateCanvasFilterWorker).toHaveBeenCalledTimes(1)
  })

  it.each(['error', 'messageerror'])('rejects active and queued work on worker %s', async eventType => {
    ;(global as any).Worker = FakeWorker
    mockWorker.autoRespond = false
    const {shelfViewFilterImageData} = require('@/shelfview/functions/canvas-filter-runner')

    const first = shelfViewFilterImageData('SHARPEN', makeImageData(2, 1, [1, 2, 3, 4, 5, 6, 7, 8]))
    const second = shelfViewFilterImageData('SHARPEN', makeImageData(2, 1, [8, 7, 6, 5, 4, 3, 2, 1]))
    const firstRejection = expect(first).rejects.toThrow('Canvas filter worker')
    const secondRejection = expect(second).rejects.toThrow('Canvas filter worker')
    await waitForMessages(mockWorker, 1)

    mockWorker.emit(eventType)

    await firstRejection
    await secondRejection
    expect(mockWorker.terminateCount).toBe(1)
  })

  it('rejects work when posting to the worker fails', async () => {
    ;(global as any).Worker = FakeWorker
    mockWorker.postError = new Error('post failed')
    const {shelfViewFilterImageData} = require('@/shelfview/functions/canvas-filter-runner')

    await expect(shelfViewFilterImageData(
      'SHARPEN',
      makeImageData(2, 1, [1, 2, 3, 4, 5, 6, 7, 8]),
    )).rejects.toThrow('post failed')
    expect(mockWorker.terminateCount).toBe(1)
  })

  it('terminates cancelled active work and resumes queued work on a fresh worker', async () => {
    ;(global as any).Worker = FakeWorker
    const firstWorker = new FakeWorker(false)
    const restartedWorker = new FakeWorker(false)
    mockCreateCanvasFilterWorker
      .mockReturnValueOnce(firstWorker)
      .mockReturnValueOnce(restartedWorker)
    const {shelfViewFilterImageData} = require('@/shelfview/functions/canvas-filter-runner')
    const abortController = new AbortController()

    const cancelled = shelfViewFilterImageData(
      'SHARPEN',
      makeImageData(2, 1, [1, 2, 3, 4, 5, 6, 7, 8]),
      abortController.signal,
    )
    const cancelledRejection = expect(cancelled).rejects.toMatchObject({name: 'AbortError'})
    await waitForMessages(firstWorker, 1)

    const retained = shelfViewFilterImageData('ADAPTIVE_MONO', makeImageData(2, 1, [8, 7, 6, 5, 4, 3, 2, 1]))
    await Promise.resolve()
    expect(firstWorker.receivedMessages).toHaveLength(1)

    abortController.abort()

    await cancelledRejection
    await waitForMessages(restartedWorker, 1)
    restartedWorker.respond()
    await expect(retained).resolves.toBeDefined()
    expect(firstWorker.terminateCount).toBe(1)
    expect(mockCreateCanvasFilterWorker).toHaveBeenCalledTimes(2)
  })
})

class FakeWorker {
  received: { mode: string, width: number, height: number } | undefined
  receivedMessages: any[] = []
  transferred = false
  terminateCount = 0
  postError: Error | undefined
  private listeners: Record<string, Array<(event: any) => void>> = {}

  constructor(public autoRespond = true) {
  }

  addEventListener(type: string, listener: (event: any) => void) {
    this.listeners[type] = [...this.listeners[type] ?? [], listener]
  }

  removeEventListener(type: string, listener: (event: any) => void) {
    this.listeners[type] = (this.listeners[type] ?? []).filter(l => l !== listener)
  }

  postMessage(message: any, transfer?: unknown[]) {
    if (this.postError) throw this.postError
    this.received = message
    this.receivedMessages.push(message)
    this.transferred = Array.isArray(transfer) && transfer.length === 1
    if (this.autoRespond) this.respond()
  }

  respond() {
    const message = this.receivedMessages[this.receivedMessages.length - 1]
    if (!message) throw new Error('No worker message to answer')
    const pixels = new Uint8ClampedArray(8).fill(9).buffer
    ;(this.listeners.message ?? [])
      .slice()
      .forEach(listener => listener({data: {token: message.token, pixels}} as MessageEvent<unknown>))
  }

  emit(type: string) {
    ;(this.listeners[type] ?? []).slice().forEach(listener => listener({type}))
  }

  terminate() {
    this.terminateCount++
  }
}

class FakeImageData {
  constructor(public data: Uint8ClampedArray, public width: number, public height: number) {
  }
}

function makeImageData(width: number, height: number, values: number[]): ImageData {
  return {width, height, data: new Uint8ClampedArray(values)} as ImageData
}

async function waitForMessages(worker: FakeWorker, count: number) {
  for (let attempt = 0; attempt < 20; attempt++) {
    if (worker.receivedMessages.length >= count) return
    await new Promise(resolve => setTimeout(resolve, 0))
  }
  throw new Error(`Worker received ${worker.receivedMessages.length} messages; expected ${count}`)
}
