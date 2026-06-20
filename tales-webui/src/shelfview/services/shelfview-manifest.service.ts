import {AxiosInstance} from 'axios'
import {ShelfViewApiError, toShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {SHELFVIEW_MANIFEST_SCHEMA_VERSION, ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'

const API_SHELFVIEW_BOOKS = '/api/shelfview/v1/books'

export default class ShelfViewManifestService {
  private http: AxiosInstance

  constructor(http: AxiosInstance) {
    this.http = http
  }

  async getManifest(bookId: string): Promise<ShelfViewManifest> {
    try {
      const manifest = (await this.http.get(`${API_SHELFVIEW_BOOKS}/${bookId}/manifest`)).data as ShelfViewManifest
      assertSupportedManifest(manifest)
      return manifest
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e

      throw toShelfViewApiError(e, 'An error occurred while trying to retrieve ShelfView manifest')
    }
  }
}

function assertSupportedManifest(manifest: ShelfViewManifest) {
  if (manifest.schemaVersion !== SHELFVIEW_MANIFEST_SCHEMA_VERSION) {
    throw new ShelfViewApiError(
      `Unsupported ShelfView manifest schema version: ${manifest.schemaVersion}`,
      'UNSUPPORTED_SCHEMA',
    )
  }
}
