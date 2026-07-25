const CACHE_NAME = 'tales-offline-v2'
const APP_SHELL = [
  './',
  'manifest.json',
  'tales-icon.svg',
  'tales-icon-square.png',
]

self.addEventListener('install', event => {
  event.waitUntil(
    caches
      .open(CACHE_NAME)
      .then(cache => cache.addAll(APP_SHELL))
      .catch(() => undefined),
  )
  self.skipWaiting()
})

self.addEventListener('activate', event => {
  event.waitUntil(
    caches
      .keys()
      .then(keys => Promise.all(keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', event => {
  const request = event.request
  if (request.method !== 'GET') return

  const url = new URL(request.url)
  if (url.origin !== self.location.origin || isApiRequest(url)) return

  if (request.mode === 'navigate') {
    event.respondWith(networkFirstAppShell(request))
    return
  }

  if (isCacheableStaticRequest(request, url)) {
    event.respondWith(cacheFirst(request))
  }
})

function isApiRequest(url) {
  return url.pathname.includes('/api/')
}

function isCacheableStaticRequest(request, url) {
  if (request.cache === 'only-if-cached' && request.mode !== 'same-origin') return false
  if (['script', 'style', 'font', 'image', 'worker'].includes(request.destination)) return true
  return /\.(?:js|css|woff2?|ttf|png|svg|ico)$/i.test(url.pathname)
}

async function networkFirstAppShell(request) {
  try {
    const response = await fetch(request)
    const cache = await caches.open(CACHE_NAME)
    await cache.put('./', response.clone())
    return response
  } catch (e) {
    return await caches.match('./') || new Response('Tales is offline.', {
      status: 503,
      statusText: 'Offline',
      headers: {'Content-Type': 'text/plain; charset=utf-8'},
    })
  }
}

async function cacheFirst(request) {
  const cached = await caches.match(request)
  if (cached) return cached

  const response = await fetch(request)
  if (response.ok) {
    const cache = await caches.open(CACHE_NAME)
    await cache.put(request, response.clone())
  }
  return response
}
