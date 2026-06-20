import {AxiosInstance} from 'axios'
import {ShelfViewApiError, toShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {SHELFVIEW_TEXT_CONTENT_SCHEMA_VERSION, ShelfViewTextContent} from '@/shelfview/models/shelfview-text'

export default class ShelfViewTextService {
  private http: AxiosInstance

  constructor(http: AxiosInstance) {
    this.http = http
  }

  async getTextContent(
    manifest: ShelfViewManifest,
    encoding?: string,
  ): Promise<ShelfViewTextContent> {
    try {
      if (!manifest.text?.endpoint) {
        throw new ShelfViewApiError('ShelfView text endpoint is missing', 'BAD_REQUEST')
      }

      const response = await this.http.get(
        manifest.text.endpoint,
        encoding ? {params: {encoding}} : undefined,
      )
      const content = response.data as ShelfViewTextContent
      assertSupportedTextContent(content)
      return content
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to retrieve ShelfView text')
    }
  }
}

function assertSupportedTextContent(content: ShelfViewTextContent) {
  if (content.schemaVersion !== SHELFVIEW_TEXT_CONTENT_SCHEMA_VERSION) {
    throw new ShelfViewApiError(
      `Unsupported ShelfView text schema version: ${content.schemaVersion}`,
      'UNSUPPORTED_SCHEMA',
    )
  }
}
