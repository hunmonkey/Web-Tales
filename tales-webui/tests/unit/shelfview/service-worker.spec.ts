import fs from 'fs'
import path from 'path'
import vm from 'vm'

// The service worker ships as a plain script in public/, so it is loaded into a
// sandbox with fake globals rather than imported. That keeps the assertions on
// the file that actually gets served.
const SERVICE_WORKER_PATH = path.join(__dirname, '..', '..', '..', 'public', 'shelfview-service-worker.js')

class FakeResponse {
  ok: boolean
  status: number
  body: string

  constructor(body: string, init: {status?: number} = {}) {
    this.status = init.status ?? 200
    this.ok = this.status >= 200 && this.status < 300
    this.body = body
  }

  clone(): FakeResponse {
    return new FakeResponse(this.body, {status: this.status})
  }
}

function loadServiceWorker(fetchImpl: (request: any) => Promise<any>) {
  const listeners: Record<string, (event: any) => void> = {}
  const cache = {
    addAll: jest.fn().mockResolvedValue(undefined),
    put: jest.fn().mockResolvedValue(undefined),
    match: jest.fn().mockResolvedValue(undefined),
  }
  const caches = {
    open: jest.fn().mockResolvedValue(cache),
    match: jest.fn().mockResolvedValue(undefined),
    keys: jest.fn().mockResolvedValue([]),
    delete: jest.fn().mockResolvedValue(true),
  }
  const self = {
    addEventListener: (type: string, handler: (event: any) => void) => {
      listeners[type] = handler
    },
    location: {origin: 'https://tales.test'},
    skipWaiting: jest.fn(),
    clients: {claim: jest.fn().mockResolvedValue(undefined)},
  }

  const context: any = {self, caches, fetch: fetchImpl, URL, Response: FakeResponse, Promise, console}
  vm.createContext(context)
  vm.runInContext(fs.readFileSync(SERVICE_WORKER_PATH, 'utf8'), context)

  return {listeners, caches, cache}
}

function navigateEvent(url: string) {
  let responded: Promise<any> | undefined
  return {
    event: {
      request: {url, method: 'GET', mode: 'navigate', destination: 'document'},
      respondWith: (value: Promise<any>) => {
        responded = value
      },
    },
    response: () => responded,
  }
}

describe('ShelfView offline service worker', () => {
  it('caches the app shell only when the navigation response is good', async () => {
    const {listeners, cache} = loadServiceWorker(async () => new FakeResponse('<html>fresh</html>'))
    const {event, response} = navigateEvent('https://tales.test/')

    listeners.fetch(event)
    await response()

    expect(cache.put).toHaveBeenCalledWith('./', expect.any(FakeResponse))
  })

  it('never lets an error page become the offline app shell', async () => {
    const {listeners, cache} = loadServiceWorker(async () => new FakeResponse('Bad Gateway', {status: 502}))
    const {event, response} = navigateEvent('https://tales.test/')

    listeners.fetch(event)
    const result = await response()

    // The bad response still reaches the browser, but it must not be stored, or
    // every later offline visit would be served that error instead of the app.
    expect(cache.put).not.toHaveBeenCalled()
    expect(result.status).toBe(502)
  })

  it('leaves API requests to the network', async () => {
    const fetchImpl = jest.fn()
    const {listeners} = loadServiceWorker(fetchImpl)
    let responded = false
    listeners.fetch({
      request: {url: 'https://tales.test/api/v1/books', method: 'GET', mode: 'cors', destination: ''},
      respondWith: () => {
        responded = true
      },
    })

    expect(responded).toBe(false)
    expect(fetchImpl).not.toHaveBeenCalled()
  })
})
