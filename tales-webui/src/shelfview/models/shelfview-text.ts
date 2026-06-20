export const SHELFVIEW_TEXT_CONTENT_SCHEMA_VERSION = 1

export interface ShelfViewTextContent {
  schemaVersion: typeof SHELFVIEW_TEXT_CONTENT_SCHEMA_VERSION,
  bookId: string,
  contentFingerprint: string,
  encoding: string,
  normalizationVersion: number,
  paragraphs: string[],
}

export interface ShelfViewTextParagraph {
  paragraphIndex: number,
  text: string,
}
