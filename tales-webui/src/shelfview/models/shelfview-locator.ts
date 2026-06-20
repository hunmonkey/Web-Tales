export const SHELFVIEW_LOCATOR_SCHEMA_VERSION = 1

export type ShelfViewLocatorKind =
  | 'PAGE'
  | 'TEXT'
  | 'MIXED'
  | 'EPUB'

export interface ShelfViewPageLocator {
  schemaVersion: typeof SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  kind: 'PAGE',
  page: number,
  offset: number,
  contentFingerprint: string,
}

export interface ShelfViewTextLocator {
  schemaVersion: typeof SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  kind: 'TEXT',
  paragraph: number,
  characterOffset: number,
  progression: number,
  normalizationVersion: number,
  contentFingerprint: string,
}

export interface ShelfViewMixedLocator {
  schemaVersion: typeof SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  kind: 'MIXED',
  itemId: string,
  itemIndex: number,
  itemLocator: ShelfViewPageLocator | ShelfViewTextLocator,
  contentFingerprint: string,
}

export interface ShelfViewEpubLocator {
  schemaVersion: typeof SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  kind: 'EPUB',
  href: string,
  title?: string,
  contentFingerprint: string,
}

export type ShelfViewLocator = ShelfViewPageLocator | ShelfViewTextLocator | ShelfViewMixedLocator | ShelfViewEpubLocator
