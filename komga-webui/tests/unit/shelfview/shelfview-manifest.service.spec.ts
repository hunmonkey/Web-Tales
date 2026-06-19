import {AxiosInstance} from 'axios'
import ShelfViewManifestService from '@/shelfview/services/shelfview-manifest.service'
import {ShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'

const manifest: ShelfViewManifest = {
  schemaVersion: 1,
  bookId: 'book-1',
  title: 'Book One',
  kind: 'FIXED_PAGE_SEQUENCE',
  sourceFormat: 'CBZ',
  contentFingerprint: 'komga-file-state:book-1',
  pageCount: 2,
  pages: {
    endpointTemplate: '/api/v1/books/book-1/pages/{page}?zero_based=true',
  },
  capabilities: {
    search: false,
    textSelection: false,
    doublePage: true,
    continuousScroll: true,
    rtl: true,
    imageFilters: true,
  },
}

describe('ShelfViewManifestService', () => {
  it('fetches a manifest by book id', async () => {
    const get = jest.fn().mockResolvedValue({data: manifest})
    const service = new ShelfViewManifestService({get} as unknown as AxiosInstance)

    await expect(service.getManifest('book-1')).resolves.toEqual(manifest)
    expect(get).toHaveBeenCalledWith('/api/shelfview/v1/books/book-1/manifest')
  })

  it('fetches a mixed ZIP manifest with ordered internal items', async () => {
    const mixedManifest: ShelfViewManifest = {
      ...manifest,
      kind: 'MIXED_CONTENT_SEQUENCE',
      sourceFormat: 'ZIP',
      pageCount: 2,
      pages: undefined,
      items: [
        {
          itemId: 'text-item',
          index: 0,
          kind: 'TEXT_REFLOW',
          label: '1.txt',
          contentFingerprint: 'komga-file-state:book-1:zip-item:text-item',
          sourceFormat: 'TXT',
          text: {
            endpoint: '/api/shelfview/v1/books/book-1/mixed/text-item/text',
            normalizationVersion: 1,
          },
        },
        {
          itemId: 'image-item',
          index: 1,
          kind: 'FIXED_PAGE_SEQUENCE',
          label: '2.jpg',
          contentFingerprint: 'komga-file-state:book-1:zip-item:image-item',
          sourceFormat: 'JPG',
          pageCount: 1,
          pages: {
            endpointTemplate: '/api/shelfview/v1/books/book-1/mixed/image-item/pages/{page}?zero_based=true',
          },
        },
      ],
      capabilities: {
        ...manifest.capabilities,
        textSelection: true,
      },
    }
    const get = jest.fn().mockResolvedValue({data: mixedManifest})
    const service = new ShelfViewManifestService({get} as unknown as AxiosInstance)

    await expect(service.getManifest('book-1')).resolves.toEqual(mixedManifest)
  })

  it('maps missing books to a ShelfView API error', async () => {
    const get = jest.fn().mockRejectedValue({
      response: {
        status: 404,
        data: {message: 'Not found'},
      },
    })
    const service = new ShelfViewManifestService({get} as unknown as AxiosInstance)

    await expect(service.getManifest('missing')).rejects.toMatchObject({
      name: 'ShelfViewApiError',
      code: 'NOT_FOUND',
      status: 404,
      message: 'An error occurred while trying to retrieve ShelfView manifest: Not found',
    })
  })

  it('rejects unsupported manifest schema versions before the reader opens them', async () => {
    const get = jest.fn().mockResolvedValue({data: {...manifest, schemaVersion: 2}})
    const service = new ShelfViewManifestService({get} as unknown as AxiosInstance)

    await expect(service.getManifest('book-1')).rejects.toEqual(
      new ShelfViewApiError('Unsupported ShelfView manifest schema version: 2', 'UNSUPPORTED_SCHEMA'),
    )
  })
})
