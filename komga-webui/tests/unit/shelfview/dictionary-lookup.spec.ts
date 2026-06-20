import {
  SHELFVIEW_DICTIONARY_PACK_SCHEMA_VERSION,
  shelfViewDictionaryLookupResults,
  shelfViewNormalizeDictionaryQuery,
  shelfViewParseDictionaryPack,
  shelfViewWebDictionaryUrl,
} from '@/shelfview/functions/dictionary-lookup'

describe('dictionary lookup helpers', () => {
  it('normalizes query text and creates the web fallback URL', () => {
    expect(shelfViewNormalizeDictionaryQuery('  첫   번째  ')).toBe('첫 번째')
    expect(decodeURIComponent(shelfViewWebDictionaryUrl('첫 번째'))).toContain('첫 번째 dictionary')
  })

  it('returns matching local results before the web lookup fallback', () => {
    const results = shelfViewDictionaryLookupResults(' 첫 번째 ', [
      {
        term: '첫 번째',
        definition: 'first item',
        sourceName: 'Test pack',
      },
      {
        term: '두 번째',
        definition: 'second item',
      },
    ])

    expect(results).toHaveLength(2)
    expect(results[0]).toMatchObject({
      kind: 'LOCAL_PACK',
      term: '첫 번째',
      definition: 'first item',
      sourceName: 'Test pack',
    })
    expect(results[1]).toMatchObject({
      kind: 'WEB_LOOKUP',
      providerLabelKey: 'shelfview_reader.dictionary_provider_web',
    })
  })

  it('parses a local dictionary pack with required source and license metadata', () => {
    const pack = shelfViewParseDictionaryPack(JSON.stringify({
      schemaVersion: SHELFVIEW_DICTIONARY_PACK_SCHEMA_VERSION,
      metadata: {
        sourceName: 'Test Dictionary',
        sourceUrl: 'https://example.test/dictionary',
        licenseName: 'First-party test data',
        attribution: 'Generated for tests',
      },
      entries: [
        {
          term: ' 첫 번째 ',
          definition: ' 첫 번째 뜻 <b>plain text</b> ',
        },
      ],
    }))

    expect(pack.metadata).toMatchObject({
      sourceName: 'Test Dictionary',
      licenseName: 'First-party test data',
    })
    expect(pack.entries).toEqual([{
      term: '첫 번째',
      definition: '첫 번째 뜻 <b>plain text</b>',
      sourceName: undefined,
      sourceUrl: undefined,
      licenseName: undefined,
    }])
  })

  it('rejects packs without source/license metadata or valid entries', () => {
    expect(() => shelfViewParseDictionaryPack('{')).toThrow('Invalid dictionary pack JSON')
    expect(() => shelfViewParseDictionaryPack(JSON.stringify({
      schemaVersion: 999,
      metadata: {},
      entries: [],
    }))).toThrow('Unsupported dictionary pack schema')
    expect(() => shelfViewParseDictionaryPack(JSON.stringify({
      schemaVersion: SHELFVIEW_DICTIONARY_PACK_SCHEMA_VERSION,
      metadata: {
        sourceName: 'Missing license',
      },
      entries: [{term: 'word', definition: 'meaning'}],
    }))).toThrow('Dictionary pack license name is required')
    expect(() => shelfViewParseDictionaryPack(JSON.stringify({
      schemaVersion: SHELFVIEW_DICTIONARY_PACK_SCHEMA_VERSION,
      metadata: {
        sourceName: 'Test',
        licenseName: 'Test license',
      },
      entries: [],
    }))).toThrow('Dictionary pack entries are invalid')
  })
})
