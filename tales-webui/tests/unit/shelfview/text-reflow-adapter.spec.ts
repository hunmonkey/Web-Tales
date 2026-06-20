import {TextReflowAdapter} from '@/shelfview/adapters/text-reflow-adapter'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {SHELFVIEW_LOCATOR_SCHEMA_VERSION} from '@/shelfview/models/shelfview-locator'
import {ShelfViewTextContent} from '@/shelfview/models/shelfview-text'

const manifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'Book One',
  kind: 'TEXT_REFLOW',
  sourceFormat: 'TXT',
  contentFingerprint: 'tales-file-state:book-1',
  text: {
    endpoint: '/api/shelfview/v1/books/book-1/text',
    detectedEncoding: 'UTF-8',
    normalizationVersion: 1,
  },
  capabilities: {
    search: false,
    textSelection: true,
    doublePage: false,
    continuousScroll: true,
    rtl: false,
    imageFilters: false,
  },
}

const content: ShelfViewTextContent = {
  schemaVersion: 1,
  bookId: 'book-1',
  contentFingerprint: manifest.contentFingerprint,
  encoding: 'UTF-8',
  normalizationVersion: 1,
  paragraphs: ['First paragraph', 'Second paragraph', 'Third paragraph'],
}

describe('TextReflowAdapter', () => {
  it('opens a text manifest at the first paragraph', async () => {
    const adapter = new TextReflowAdapter(jest.fn().mockResolvedValue(content))

    await adapter.open(manifest)

    expect(adapter.getCurrentParagraphIndex()).toBe(0)
    expect(adapter.getParagraphsCount()).toBe(3)
    expect(adapter.getVisibleParagraphs('SINGLE_PAGE')).toEqual([
      {paragraphIndex: 0, text: 'First paragraph'},
    ])
    expect(adapter.getCurrentLocator()).toEqual({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'TEXT',
      paragraph: 0,
      characterOffset: 0,
      progression: 0,
      normalizationVersion: 1,
      contentFingerprint: manifest.contentFingerprint,
    })
  })

  it('moves next, previous and clamps paragraph locator boundaries', async () => {
    const adapter = new TextReflowAdapter(jest.fn().mockResolvedValue(content))

    await adapter.open(manifest)

    await adapter.next()
    expect(adapter.getCurrentParagraphIndex()).toBe(1)

    await adapter.next()
    await adapter.next()
    expect(adapter.getCurrentParagraphIndex()).toBe(2)
    expect(adapter.canGoNext()).toBe(false)

    await adapter.previous()
    expect(adapter.getCurrentParagraphIndex()).toBe(1)

    await expect(adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'TEXT',
      paragraph: -20,
      characterOffset: 0,
      progression: 0,
      normalizationVersion: 1,
      contentFingerprint: manifest.contentFingerprint,
    })).rejects.toThrow('paragraph is invalid')

    await adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'TEXT',
      paragraph: 20,
      characterOffset: 999,
      progression: 0,
      normalizationVersion: 1,
      contentFingerprint: manifest.contentFingerprint,
    })
    expect(adapter.getCurrentParagraphIndex()).toBe(2)
    expect(adapter.getCurrentCharacterOffset()).toBe('Third paragraph'.length)

    await adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'TEXT',
      paragraph: 1,
      characterOffset: 7,
      progression: 0.5,
      normalizationVersion: 1,
      contentFingerprint: manifest.contentFingerprint,
    })
    expect(adapter.getCurrentParagraphIndex()).toBe(1)
    expect(adapter.getCurrentCharacterOffset()).toBe(7)
    expect(adapter.getCurrentLocator()).toMatchObject({
      paragraph: 1,
      characterOffset: 7,
    })
    expect(adapter.getProgression()).toBeCloseTo((1 + (7 / 'Second paragraph'.length)) / 3)
  })

  it('returns every paragraph for continuous scroll mode', async () => {
    const adapter = new TextReflowAdapter(jest.fn().mockResolvedValue(content))

    await adapter.open(manifest)

    expect(adapter.getVisibleParagraphs('CONTINUOUS_SCROLL')).toEqual([
      {paragraphIndex: 0, text: 'First paragraph'},
      {paragraphIndex: 1, text: 'Second paragraph'},
      {paragraphIndex: 2, text: 'Third paragraph'},
    ])
  })

  it('rejects non-text manifests and stale text content', async () => {
    const adapter = new TextReflowAdapter(jest.fn().mockResolvedValue(content))

    await expect(adapter.open({...manifest, kind: 'FIXED_PAGE_SEQUENCE'})).rejects.toThrow('text manifests')

    const staleContent = {...content, contentFingerprint: 'tales-file-state:old'}
    const staleAdapter = new TextReflowAdapter(jest.fn().mockResolvedValue(staleContent))
    await expect(staleAdapter.open(manifest)).rejects.toThrow('different book version')
  })

  it('rejects non-text and stale locators', async () => {
    const adapter = new TextReflowAdapter(jest.fn().mockResolvedValue(content))

    await adapter.open(manifest)

    await expect(adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'PAGE',
      page: 1,
      offset: 0,
      contentFingerprint: manifest.contentFingerprint,
    })).rejects.toThrow('text locators')

    await expect(adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'TEXT',
      paragraph: 1,
      characterOffset: 0,
      progression: 0,
      normalizationVersion: 1,
      contentFingerprint: 'tales-file-state:old',
    })).rejects.toThrow('different book version')

    await expect(adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'TEXT',
      paragraph: 1,
      characterOffset: 0,
      progression: 2,
      normalizationVersion: 1,
      contentFingerprint: manifest.contentFingerprint,
    })).rejects.toThrow('progression is invalid')

    await expect(adapter.goTo({
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'TEXT',
      paragraph: 1.5,
      characterOffset: 0,
      progression: 0,
      normalizationVersion: 1,
      contentFingerprint: manifest.contentFingerprint,
    })).rejects.toThrow('paragraph is invalid')
  })

  it('handles empty text content without reporting progress', async () => {
    const adapter = new TextReflowAdapter(jest.fn().mockResolvedValue({...content, paragraphs: []}))

    await adapter.open(manifest)

    expect(adapter.getParagraphsCount()).toBe(0)
    expect(adapter.getVisibleParagraphs('SINGLE_PAGE')).toEqual([])
    expect(adapter.getProgression()).toBe(0)
    expect(adapter.canGoNext()).toBe(false)
    expect(adapter.canGoPrevious()).toBe(false)
  })
})
