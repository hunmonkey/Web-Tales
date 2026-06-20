import urls from '@/functions/urls'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'

export function shelfViewPageUrl(
  manifest: ShelfViewManifest,
  pageIndex: number,
  originNoSlash: string = urls.originNoSlash,
): string | undefined {
  if (!manifest.pages?.endpointTemplate) return undefined

  const endpoint = shelfViewPageEndpoint(manifest.pages.endpointTemplate, pageIndex)
  return shelfViewEndpointUrl(endpoint, originNoSlash)
}

export function shelfViewThumbnailUrl(
  manifest: ShelfViewManifest,
  pageIndex: number,
  originNoSlash: string = urls.originNoSlash,
): string | undefined {
  const template = manifest.pages?.thumbnailEndpointTemplate ?? manifest.pages?.endpointTemplate
  if (!template) return undefined

  const endpoint = shelfViewPageEndpoint(template, pageIndex)
  return shelfViewEndpointUrl(endpoint, originNoSlash)
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
