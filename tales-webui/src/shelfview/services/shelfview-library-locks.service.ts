import {AxiosInstance} from 'axios'

const API_LIBRARY_LOCKS = '/api/shelfview/v1/library-locks'

// $eventHub event fired after any lock or unlock change, so the sidebar and
// every library actions menu refresh their lock state together.
export const LIBRARY_LOCKS_CHANGED = 'shelfview-library-locks-changed'

export interface ShelfViewLibraryLockDto {
  libraryId: string,
  name: string,
  unlocked: boolean,
}

/**
 * Library PIN locks (ADR-0022). A locked library is absent from every other
 * API until this session unlocks it, so this list is the only place the UI
 * can learn a locked library's name.
 */
export default class ShelfViewLibraryLocksService {
  private http: AxiosInstance

  constructor(http: AxiosInstance) {
    this.http = http
  }

  async getLockStates(): Promise<ShelfViewLibraryLockDto[]> {
    return (await this.http.get(API_LIBRARY_LOCKS)).data
  }

  async unlock(libraryId: string, pin: string): Promise<void> {
    await this.http.post(`${API_LIBRARY_LOCKS}/${libraryId}/unlock`, {pin})
  }

  async relock(libraryId: string): Promise<void> {
    await this.http.delete(`${API_LIBRARY_LOCKS}/${libraryId}/unlock`)
  }

  async setPin(libraryId: string, pin: string): Promise<void> {
    await this.http.put(`${API_LIBRARY_LOCKS}/${libraryId}`, {pin})
  }

  async removePin(libraryId: string): Promise<void> {
    await this.http.delete(`${API_LIBRARY_LOCKS}/${libraryId}`)
  }
}
