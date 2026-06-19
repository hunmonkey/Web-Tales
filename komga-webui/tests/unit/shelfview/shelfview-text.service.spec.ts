import {AxiosInstance} from 'axios'
import ShelfViewTextService from '@/shelfview/services/shelfview-text.service'
import {ShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {ShelfViewTextContent} from '@/shelfview/models/shelfview-text'

const manifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'Book One',
  kind: 'TEXT_REFLOW',
  sourceFormat: 'TXT',
  contentFingerprint: 'komga-file-state:book-1',
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
  paragraphs: ['First paragraph', 'Second paragraph'],
}

describe('ShelfViewTextService', () => {
  it('fetches text content from the manifest endpoint', async () => {
    const get = jest.fn().mockResolvedValue({data: content})
    const service = new ShelfViewTextService({get} as unknown as AxiosInstance)

    await expect(service.getTextContent(manifest)).resolves.toEqual(content)
    expect(get).toHaveBeenCalledWith('/api/shelfview/v1/books/book-1/text', undefined)
  })

  it('passes a manual encoding override when requested', async () => {
    const get = jest.fn().mockResolvedValue({data: content})
    const service = new ShelfViewTextService({get} as unknown as AxiosInstance)

    await expect(service.getTextContent(manifest, 'euc-kr')).resolves.toEqual(content)
    expect(get).toHaveBeenCalledWith('/api/shelfview/v1/books/book-1/text', {
      params: {encoding: 'euc-kr'},
    })
  })

  it('rejects manifests without a text endpoint', async () => {
    const get = jest.fn()
    const service = new ShelfViewTextService({get} as unknown as AxiosInstance)

    await expect(service.getTextContent({...manifest, text: undefined})).rejects.toEqual(
      new ShelfViewApiError('ShelfView text endpoint is missing', 'BAD_REQUEST'),
    )
    expect(get).not.toHaveBeenCalled()
  })

  it('rejects unsupported text schema versions before rendering', async () => {
    const get = jest.fn().mockResolvedValue({data: {...content, schemaVersion: 2}})
    const service = new ShelfViewTextService({get} as unknown as AxiosInstance)

    await expect(service.getTextContent(manifest)).rejects.toEqual(
      new ShelfViewApiError('Unsupported ShelfView text schema version: 2', 'UNSUPPORTED_SCHEMA'),
    )
  })

  it('maps text API failures to ShelfView errors', async () => {
    const get = jest.fn().mockRejectedValue({
      response: {
        status: 413,
        data: {message: 'Text file is too large'},
      },
    })
    const service = new ShelfViewTextService({get} as unknown as AxiosInstance)

    await expect(service.getTextContent(manifest)).rejects.toEqual(
      new ShelfViewApiError('An error occurred while trying to retrieve ShelfView text: Text file is too large', 'PAYLOAD_TOO_LARGE', 413),
    )
  })
})
