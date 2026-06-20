package org.tales.shelfview.domain.persistence

import org.tales.shelfview.domain.ShelfViewReaderPosition

interface ShelfViewReaderPositionRepository {
  fun findPositionOrNull(
    bookId: String,
    userId: String,
  ): ShelfViewReaderPosition?

  fun savePosition(position: ShelfViewReaderPosition)

  fun deletePosition(
    bookId: String,
    userId: String,
  )

  fun deletePositionsByUserId(userId: String)

  fun deletePositionsByBookId(bookId: String)

  fun deleteAllPositions()
}
