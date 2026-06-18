import urls from '@/functions/urls'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'

export function shelfViewPageUrl(
  manifest: ShelfViewManifest,
  pageIndex: number,
  originNoSlash: string = urls.originNoSlash,
): string | undefined {
  if (!manifest.pages?.endpointTemplate) return undefined

  const endpoint = manifest.pages.endpointTemplate.replace('{page}', pageIndex.toString())
  return shelfViewEndpointUrl(endpoint, originNoSlash)
}

export function shelfViewEndpointUrl(
  endpoint: string,
  originNoSlash: string = urls.originNoSlash,
): string {
  if (/^https?:\/\//i.test(endpoint)) return endpoint
  return `${originNoSlash}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`
}
