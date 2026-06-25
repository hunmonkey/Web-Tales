export const SHELFVIEW_DICTIONARY_QUERY_MAX_LENGTH = 80
export const SHELFVIEW_DICTIONARY_PACK_SCHEMA_VERSION = 1
const SHELFVIEW_DICTIONARY_PACK_MAX_ENTRIES = 500
const SHELFVIEW_DICTIONARY_DEFINITION_MAX_LENGTH = 1000
const SHELFVIEW_DICTIONARY_METADATA_MAX_LENGTH = 240
const SHELFVIEW_DICTIONARY_SOURCE_URL_MAX_LENGTH = 500

export type ShelfViewDictionaryProviderKind =
  | 'WEB_LOOKUP'
  | 'LOCAL_PACK'
  | 'CONFIGURED_API'

export interface ShelfViewDictionaryLocalEntry {
  term: string,
  definition: string,
  sourceName?: string,
  sourceUrl?: string,
  licenseName?: string,
}

export interface ShelfViewDictionaryPackMetadata {
  sourceName: string,
  sourceUrl?: string,
  retrievedAt?: string,
  licenseName: string,
  licenseUrl?: string,
  attribution?: string,
  redistributionNotes?: string,
}

export interface ShelfViewDictionaryPack {
  schemaVersion: typeof SHELFVIEW_DICTIONARY_PACK_SCHEMA_VERSION,
  metadata: ShelfViewDictionaryPackMetadata,
  entries: ShelfViewDictionaryLocalEntry[],
}

export interface ShelfViewDictionaryLookupResult {
  key: string,
  kind: ShelfViewDictionaryProviderKind,
  providerLabelKey?: string,
  providerLabel?: string,
  actionLabelKey?: string,
  term?: string,
  definition?: string,
  sourceName?: string,
  sourceUrl?: string,
  licenseName?: string,
  url?: string,
}

export interface ShelfViewDictionaryProviderDto {
  id: string,
  kind: ShelfViewDictionaryProviderKind,
  label: string,
  enabled: boolean,
  sourceName: string,
  sourceUrl?: string,
  retrievedAt?: string,
  licenseName: string,
  licenseUrl?: string,
  attribution?: string,
  redistributionNotes?: string,
  entryCount: number,
  createdAt: string,
  updatedAt: string,
}

export function shelfViewNormalizeDictionaryQuery(text: string): string {
  return text.replace(/\s+/g, ' ').trim().slice(0, SHELFVIEW_DICTIONARY_QUERY_MAX_LENGTH)
}

export function shelfViewParseDictionaryPack(rawJson: string): ShelfViewDictionaryPack {
  let parsed: unknown
  try {
    parsed = JSON.parse(rawJson)
  } catch (e) {
    throw new Error('Invalid dictionary pack JSON')
  }

  if (!isRecord(parsed) || parsed.schemaVersion !== SHELFVIEW_DICTIONARY_PACK_SCHEMA_VERSION) {
    throw new Error('Unsupported dictionary pack schema')
  }

  if (!isRecord(parsed.metadata)) {
    throw new Error('Dictionary pack metadata is required')
  }

  const metadata = parseDictionaryPackMetadata(parsed.metadata)
  if (!Array.isArray(parsed.entries) || parsed.entries.length === 0 || parsed.entries.length > SHELFVIEW_DICTIONARY_PACK_MAX_ENTRIES) {
    throw new Error('Dictionary pack entries are invalid')
  }

  const entries = parsed.entries.map(parseDictionaryPackEntry)

  return {
    schemaVersion: SHELFVIEW_DICTIONARY_PACK_SCHEMA_VERSION,
    metadata,
    entries,
  }
}

export function shelfViewWebDictionaryUrl(query: string): string {
  return `https://www.google.com/search?q=${encodeURIComponent(`${shelfViewNormalizeDictionaryQuery(query)} dictionary`)}`
}

export function shelfViewDictionaryLookupResults(
  query: string,
  localEntries: ShelfViewDictionaryLocalEntry[] = [],
  serverResults: ShelfViewDictionaryLookupResult[] = [],
): ShelfViewDictionaryLookupResult[] {
  const normalizedQuery = shelfViewNormalizeDictionaryQuery(query)
  if (!normalizedQuery) return []

  const loweredQuery = normalizedQuery.toLocaleLowerCase()
  const localResults = localEntries
    .filter(entry => shelfViewNormalizeDictionaryQuery(entry.term).toLocaleLowerCase() === loweredQuery)
    .map((entry, index) => ({
      key: `local-${index}-${entry.term}`,
      kind: 'LOCAL_PACK' as const,
      providerLabelKey: 'shelfview_reader.dictionary_provider_local',
      term: entry.term,
      definition: entry.definition,
      sourceName: entry.sourceName,
      sourceUrl: entry.sourceUrl,
      licenseName: entry.licenseName,
    }))

  return [
    ...serverResults,
    ...localResults,
    {
      key: 'web-lookup',
      kind: 'WEB_LOOKUP',
      providerLabelKey: 'shelfview_reader.dictionary_provider_web',
      actionLabelKey: 'shelfview_reader.dictionary_search_web',
      url: shelfViewWebDictionaryUrl(normalizedQuery),
    },
  ]
}

function parseDictionaryPackMetadata(metadata: Record<string, unknown>): ShelfViewDictionaryPackMetadata {
  return {
    sourceName: boundedRequiredString(metadata.sourceName, SHELFVIEW_DICTIONARY_METADATA_MAX_LENGTH, 'Dictionary pack source name is required'),
    sourceUrl: boundedOptionalString(metadata.sourceUrl, SHELFVIEW_DICTIONARY_SOURCE_URL_MAX_LENGTH),
    retrievedAt: boundedOptionalString(metadata.retrievedAt, SHELFVIEW_DICTIONARY_METADATA_MAX_LENGTH),
    licenseName: boundedRequiredString(metadata.licenseName, SHELFVIEW_DICTIONARY_METADATA_MAX_LENGTH, 'Dictionary pack license name is required'),
    licenseUrl: boundedOptionalString(metadata.licenseUrl, SHELFVIEW_DICTIONARY_SOURCE_URL_MAX_LENGTH),
    attribution: boundedOptionalString(metadata.attribution, SHELFVIEW_DICTIONARY_METADATA_MAX_LENGTH),
    redistributionNotes: boundedOptionalString(metadata.redistributionNotes, SHELFVIEW_DICTIONARY_METADATA_MAX_LENGTH),
  }
}

function parseDictionaryPackEntry(entry: unknown): ShelfViewDictionaryLocalEntry {
  if (!isRecord(entry)) throw new Error('Dictionary pack entry is invalid')

  return {
    term: boundedRequiredString(entry.term, SHELFVIEW_DICTIONARY_QUERY_MAX_LENGTH, 'Dictionary pack entry term is required'),
    definition: boundedRequiredString(entry.definition, SHELFVIEW_DICTIONARY_DEFINITION_MAX_LENGTH, 'Dictionary pack entry definition is required'),
    sourceName: boundedOptionalString(entry.sourceName, SHELFVIEW_DICTIONARY_METADATA_MAX_LENGTH),
    sourceUrl: boundedOptionalString(entry.sourceUrl, SHELFVIEW_DICTIONARY_SOURCE_URL_MAX_LENGTH),
    licenseName: boundedOptionalString(entry.licenseName, SHELFVIEW_DICTIONARY_METADATA_MAX_LENGTH),
  }
}

function boundedRequiredString(
  value: unknown,
  maxLength: number,
  errorMessage: string,
): string {
  const normalized = boundedOptionalString(value, maxLength)
  if (!normalized) throw new Error(errorMessage)
  return normalized
}

function boundedOptionalString(
  value: unknown,
  maxLength: number,
): string | undefined {
  if (value === undefined || value === null) return undefined
  if (typeof value !== 'string') throw new Error('Dictionary pack field is invalid')

  const normalized = value.replace(/\s+/g, ' ').trim()
  if (!normalized) return undefined
  if (normalized.length > maxLength) throw new Error('Dictionary pack field is too long')
  return normalized
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
