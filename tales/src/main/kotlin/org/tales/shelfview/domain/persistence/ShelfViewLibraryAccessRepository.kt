package org.tales.shelfview.domain.persistence

interface ShelfViewLibraryAccessRepository {
  /** Ids of every library protected by a PIN. */
  fun findLockedLibraryIds(): Set<String>

  fun findPinHashOrNull(libraryId: String): String?

  fun savePinHash(
    libraryId: String,
    pinHash: String,
  )

  fun deletePin(libraryId: String)
}
