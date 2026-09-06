package org.tales.shelfview.domain.persistence

/**
 * URLs of the books and series an admin removed from a library (ADR-0023).
 * The scanner skips them, so a removal outlives the next scan.
 */
interface ShelfViewRemovedEntryRepository {
  fun findUrls(libraryId: String): Set<String>

  fun add(
    libraryId: String,
    url: String,
  )

  fun deleteAll(libraryId: String)
}
