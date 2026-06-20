import {AxiosInstance} from 'axios'
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
}
