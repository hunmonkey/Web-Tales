package org.gotson.komga.shelfview.domain.persistence

import org.gotson.komga.shelfview.domain.ShelfViewHighlight

interface ShelfViewHighlightRepository {
  fun findHighlights(
    bookId: String,
    userId: String,
  ): Collection<ShelfViewHighlight>

  fun findHighlightByIdAndUserIdOrNull(
    highlightId: String,
    userId: String,
  ): ShelfViewHighlight?

  fun saveHighlight(highlight: ShelfViewHighlight)

  fun deleteHighlight(
    highlightId: String,
    userId: String,
  )

  fun deleteHighlightsByUserId(userId: String)

  fun deleteHighlightsByBookId(bookId: String)

  fun deleteAllHighlights()
}
