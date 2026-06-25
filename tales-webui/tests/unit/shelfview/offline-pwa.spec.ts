import {registerShelfViewServiceWorker, shelfViewServiceWorkerBaseUrl} from '@/shelfview/functions/offline-pwa'

describe('offline PWA service worker registration', () => {
  it('normalizes the service worker base URL', () => {
    expect(shelfViewServiceWorkerBaseUrl()).toBe('/')
    expect(shelfViewServiceWorkerBaseUrl('/tales')).toBe('/tales/')
    expect(shelfViewServiceWorkerBaseUrl('/tales/')).toBe('/tales/')
  })

  it('skips registration outside production', () => {
    const win = {
      navigator: {
        serviceWorker: {
          register: jest.fn(),
        },
      },
      document: {
        readyState: 'complete',
      },
    } as unknown as Window

    registerShelfViewServiceWorker(win, 'development')

    expect(win.navigator.serviceWorker.register).not.toHaveBeenCalled()
  })

  it('registers the service worker after window load in production', () => {
    const listeners: Record<string, EventListener> = {}
    const register = jest.fn().mockResolvedValue({})
    const addEventListener = jest.fn((event: string, listener: EventListener) => {
      listeners[event] = listener
    })
    const win = {
      resourceBaseUrl: '/tales/',
      navigator: {
        serviceWorker: {
          register,
        },
      },
      document: {
        readyState: 'loading',
      },
      addEventListener,
    } as unknown as Window

    registerShelfViewServiceWorker(win, 'production')

    expect(addEventListener).toHaveBeenCalledWith('load', expect.any(Function), {once: true})
    listeners.load(new Event('load'))

    expect(register).toHaveBeenCalledWith('/tales/shelfview-service-worker.js', {scope: '/tales/'})
  })

  it('registers immediately when the document is already loaded', () => {
    const register = jest.fn().mockResolvedValue({})
    const win = {
      resourceBaseUrl: '/',
      navigator: {
        serviceWorker: {
          register,
        },
      },
      document: {
        readyState: 'complete',
      },
      addEventListener: jest.fn(),
    } as unknown as Window

    registerShelfViewServiceWorker(win, 'production')

    expect(register).toHaveBeenCalledWith('/shelfview-service-worker.js', {scope: '/'})
    expect(win.addEventListener).not.toHaveBeenCalled()
  })
})
