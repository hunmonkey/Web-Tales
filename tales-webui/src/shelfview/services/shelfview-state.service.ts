import {AxiosInstance} from 'axios'
import urls from '@/functions/urls'
import {
  ShelfViewDictionaryLookupResult,
  ShelfViewDictionaryPack,
  ShelfViewDictionaryProviderDto,
} from '@/shelfview/functions/dictionary-lookup'
import {ShelfViewApiError, toShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {
  ShelfViewBookmarkDto,
  ShelfViewBookmarkUpdateDto,
  ShelfViewHighlightDto,
  ShelfViewHighlightUpdateDto,
  ShelfViewPositionDto,
  ShelfViewPositionUpdateDto,
  ShelfViewPreferenceQuery,
  ShelfViewPreferenceResponseDto,
  ShelfViewPreferenceUpdateDto,
} from '@/shelfview/models/shelfview-state'

const API_SHELFVIEW = '/api/shelfview/v1'
const API_SHELFVIEW_BOOKS = `${API_SHELFVIEW}/books`

export default class ShelfViewStateService {
  private http: AxiosInstance

  constructor(http: AxiosInstance) {
    this.http = http
  }

  async getPosition(bookId: string): Promise<ShelfViewPositionDto | undefined> {
    try {
      const response = await this.http.get(`${API_SHELFVIEW_BOOKS}/${bookId}/position`)
      return response.status === 204 ? undefined : response.data as ShelfViewPositionDto
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to retrieve ShelfView position')
    }
  }

  async savePosition(
    bookId: string,
    position: ShelfViewPositionUpdateDto,
  ): Promise<ShelfViewPositionDto> {
    try {
      return (await this.http.put(`${API_SHELFVIEW_BOOKS}/${bookId}/position`, position)).data as ShelfViewPositionDto
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to save ShelfView position')
    }
  }

  /**
   * Saves the position while the page is going away. The browser cancels a
   * normal XHR started during unload, which loses the last pages the reader
   * actually read, so this uses a keepalive fetch instead. `sendBeacon` is not
   * usable here because the endpoint is a PUT. Returns false when the browser
   * has no keepalive support, so the caller can fall back to [savePosition].
   */
  savePositionOnUnload(
    bookId: string,
    position: ShelfViewPositionUpdateDto,
  ): boolean {
    if (typeof fetch !== 'function') return false

    try {
      void fetch(`${urls.originNoSlash}${API_SHELFVIEW_BOOKS}/${bookId}/position`, {
        method: 'PUT',
        keepalive: true,
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-Requested-With': 'XMLHttpRequest',
        },
        body: JSON.stringify(position),
      }).catch(() => undefined)
      return true
    } catch (e) {
      return false
    }
  }

  async getBookmarks(bookId: string): Promise<ShelfViewBookmarkDto[]> {
    try {
      return (await this.http.get(`${API_SHELFVIEW_BOOKS}/${bookId}/bookmarks`)).data as ShelfViewBookmarkDto[]
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to retrieve ShelfView bookmarks')
    }
  }

  async createBookmark(
    bookId: string,
    bookmark: ShelfViewBookmarkUpdateDto,
  ): Promise<ShelfViewBookmarkDto> {
    try {
      return (await this.http.post(`${API_SHELFVIEW_BOOKS}/${bookId}/bookmarks`, bookmark)).data as ShelfViewBookmarkDto
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to create ShelfView bookmark')
    }
  }

  async updateBookmark(
    bookmarkId: string,
    bookmark: ShelfViewBookmarkUpdateDto,
  ): Promise<ShelfViewBookmarkDto> {
    try {
      return (await this.http.put(`${API_SHELFVIEW}/bookmarks/${bookmarkId}`, bookmark)).data as ShelfViewBookmarkDto
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to update ShelfView bookmark')
    }
  }

  async deleteBookmark(bookmarkId: string): Promise<void> {
    try {
      await this.http.delete(`${API_SHELFVIEW}/bookmarks/${bookmarkId}`)
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to delete ShelfView bookmark')
    }
  }

  async getHighlights(bookId: string): Promise<ShelfViewHighlightDto[]> {
    try {
      return (await this.http.get(`${API_SHELFVIEW_BOOKS}/${bookId}/highlights`)).data as ShelfViewHighlightDto[]
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to retrieve ShelfView highlights')
    }
  }

  async createHighlight(
    bookId: string,
    highlight: ShelfViewHighlightUpdateDto,
  ): Promise<ShelfViewHighlightDto> {
    try {
      return (await this.http.post(`${API_SHELFVIEW_BOOKS}/${bookId}/highlights`, highlight)).data as ShelfViewHighlightDto
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to create ShelfView highlight')
    }
  }

  async updateHighlight(
    highlightId: string,
    highlight: ShelfViewHighlightUpdateDto,
  ): Promise<ShelfViewHighlightDto> {
    try {
      return (await this.http.put(`${API_SHELFVIEW}/highlights/${highlightId}`, highlight)).data as ShelfViewHighlightDto
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to update ShelfView highlight')
    }
  }

  async deleteHighlight(highlightId: string): Promise<void> {
    try {
      await this.http.delete(`${API_SHELFVIEW}/highlights/${highlightId}`)
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to delete ShelfView highlight')
    }
  }

  async getPreference(query: ShelfViewPreferenceQuery): Promise<ShelfViewPreferenceResponseDto> {
    try {
      return (await this.http.get(`${API_SHELFVIEW}/preferences`, {params: query})).data as ShelfViewPreferenceResponseDto
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to retrieve ShelfView preference')
    }
  }

  async savePreference(preference: ShelfViewPreferenceUpdateDto): Promise<ShelfViewPreferenceResponseDto> {
    try {
      return (await this.http.put(`${API_SHELFVIEW}/preferences`, preference)).data as ShelfViewPreferenceResponseDto
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to save ShelfView preference')
    }
  }

  async getDictionaryProviders(): Promise<ShelfViewDictionaryProviderDto[]> {
    try {
      return (await this.http.get(`${API_SHELFVIEW}/dictionary/providers`)).data as ShelfViewDictionaryProviderDto[]
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to retrieve ShelfView dictionary providers')
    }
  }

  async importDictionaryLocalPack(pack: ShelfViewDictionaryPack): Promise<ShelfViewDictionaryProviderDto> {
    try {
      return (await this.http.post(`${API_SHELFVIEW}/dictionary/providers/local-pack`, pack)).data as ShelfViewDictionaryProviderDto
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to import ShelfView dictionary pack')
    }
  }

  async deleteDictionaryProvider(providerId: string): Promise<void> {
    try {
      await this.http.delete(`${API_SHELFVIEW}/dictionary/providers/${providerId}`)
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to delete ShelfView dictionary provider')
    }
  }

  async lookupDictionary(query: string): Promise<ShelfViewDictionaryLookupResult[]> {
    try {
      const response = await this.http.get(`${API_SHELFVIEW}/dictionary/lookup`, {params: {query}})
      return (response.data as ShelfViewDictionaryLookupResult[]).map(result => ({
        ...result,
        kind: 'LOCAL_PACK',
      }))
    } catch (e) {
      if (e instanceof ShelfViewApiError) throw e
      throw toShelfViewApiError(e, 'An error occurred while trying to lookup ShelfView dictionary')
    }
  }
}
