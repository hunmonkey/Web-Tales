export const SHELFVIEW_MANIFEST_SCHEMA_VERSION = 1

export type ShelfViewPublicationKind =
  | 'FIXED_PAGE_SEQUENCE'
  | 'TEXT_REFLOW'
  | 'MIXED_CONTENT_SEQUENCE'

export type ShelfViewSourceFormat =
  | 'CBZ'
  | 'ZIP'
  | 'CBR'
  | 'PDF'
  | 'IMAGE_DIRECTORY'
  | 'TXT'
  | 'JPG'
  | 'PNG'
  | 'WEBP'
  | 'GIF'

export interface ShelfViewManifest {
  schemaVersion: typeof SHELFVIEW_MANIFEST_SCHEMA_VERSION,
  bookId: string,
  title: string,
  kind: ShelfViewPublicationKind,
  sourceFormat: ShelfViewSourceFormat,
  contentFingerprint: string,
  pageCount?: number,
  text?: ShelfViewText,
  pages?: ShelfViewPages,
  items?: ShelfViewManifestItem[],
  capabilities: ShelfViewCapabilities,
}

export interface ShelfViewManifestItem {
  itemId: string,
  index: number,
  kind: ShelfViewPublicationKind,
  label: string,
  contentFingerprint: string,
  sourceFormat?: ShelfViewSourceFormat,
  pageCount?: number,
  text?: ShelfViewText,
  pages?: ShelfViewPages,
}

export interface ShelfViewText {
  endpoint: string,
  detectedEncoding?: string,
  normalizationVersion: number,
}

export interface ShelfViewPages {
  endpointTemplate: string,
  thumbnailEndpointTemplate?: string,
}

export interface ShelfViewCapabilities {
  search: boolean,
  textSelection: boolean,
  doublePage: boolean,
  continuousScroll: boolean,
  rtl: boolean,
  imageFilters: boolean,
}
