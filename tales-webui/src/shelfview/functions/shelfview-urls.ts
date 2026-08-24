import urls from '@/functions/urls'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'

export function shelfViewPageUrl(
  manifest: ShelfViewManifest,
  pageIndex: number,
  originNoSlash: string = urls.originNoSlash,
): string | undefined {
  if (!manifest.pages?.endpointTemplate) return undefined

  const endpoint = shelfViewPageEndpoint(manifest.pages.endpointTemplate, pageIndex)
  return shelfViewEndpointUrl(shelfViewVersionedEndpoint(endpoint, manifest.contentFingerprint), originNoSlash)
}

export function shelfViewThumbnailUrl(
  manifest: ShelfViewManifest,
  pageIndex: number,
  originNoSlash: string = urls.originNoSlash,
): string | undefined {
  const template = manifest.pages?.thumbnailEndpointTemplate ?? manifest.pages?.endpointTemplate
  if (!template) return undefined

  const endpoint = shelfViewPageEndpoint(template, pageIndex)
  return shelfViewEndpointUrl(shelfViewVersionedEndpoint(endpoint, manifest.contentFingerprint), originNoSlash)
}

/**
 * Splits the browser cache by book version. Page images are served with a
 * one-day private cache on the assumption that a replaced book hands out new
 * URLs, but the endpoint template only ever varies by page number, so replacing
 * a CBZ used to leave the old pages on screen for up to a day. The server
 * ignores this parameter; it exists purely to change the cache key.
 */
export function shelfViewVersionedEndpoint(
  endpoint: string,
  contentFingerprint: string | undefined,
): string {
  if (!contentFingerprint) return endpoint

  const separator = endpoint.includes('?') ? '&' : '?'
  return `${endpoint}${separator}v=${encodeURIComponent(contentFingerprint)}`
}

export function shelfViewEndpointUrl(
  endpoint: string,
  originNoSlash: string = urls.originNoSlash,
): string {
  if (/^https?:\/\//i.test(endpoint)) return endpoint
  return `${originNoSlash}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`
}

function shelfViewPageEndpoint(
  template: string,
  pageIndex: number,
): string {
  return template
    .replace('{pageOneBased}', (pageIndex + 1).toString())
    .replace('{page}', pageIndex.toString())
}

/**
 * The fetch mode a page image has to be requested with, or `undefined` for none.
 *
 * A same-origin image cannot taint a canvas, so it needs no CORS request, and
 * asking for one anyway is not free: the browser treats the same URL fetched in
 * two different modes as two different resources. A page prefetched as CORS is
 * then not the image the plain `<img>` displays, so it is downloaded again at
 * the moment of the page turn - exactly when the reader is waiting. Only a page
 * served from another origin, which is the development setup, still needs the
 * CORS mode so the canvas filters can read its pixels.
 */
export function shelfViewPageImageCrossOrigin(
  url: string,
  pageOrigin: string = window.location.origin,
): 'use-credentials' | undefined {
  try {
    return new URL(url, pageOrigin).origin === pageOrigin ? undefined : 'use-credentials'
  } catch (_error) {
    return 'use-credentials'
  }
}
