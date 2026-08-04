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
