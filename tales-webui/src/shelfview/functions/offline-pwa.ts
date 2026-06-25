export function shelfViewServiceWorkerBaseUrl(resourceBaseUrl: string | undefined = window.resourceBaseUrl): string {
  const baseUrl = resourceBaseUrl?.trim() || '/'
  return baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`
}

export function registerShelfViewServiceWorker(
  win: Window = window,
  nodeEnv: string | undefined = process.env.NODE_ENV,
): void {
  if (nodeEnv !== 'production') return
  if (!('serviceWorker' in win.navigator)) return

  const register = () => {
    const baseUrl = shelfViewServiceWorkerBaseUrl(win.resourceBaseUrl)
    void win.navigator.serviceWorker
      .register(`${baseUrl}shelfview-service-worker.js`, {scope: baseUrl})
      .catch(() => undefined)
  }

  if (win.document.readyState === 'complete') {
    register()
  } else {
    win.addEventListener('load', register, {once: true})
  }
}
