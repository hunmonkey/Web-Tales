package org.tales.shelfview.domain.persistence

import org.tales.shelfview.domain.ShelfViewBookmark

interface ShelfViewBookmarkRepository {
  fun findBookmarks(
    bookId: String,
    userId: String,
  ): Collection<ShelfViewBookmark>

  fun findBookmarkByIdAndUserIdOrNull(
    bookmarkId: String,
    userId: String,
  ): ShelfViewBookmark?

  fun saveBookmark(bookmark: ShelfViewBookmark)

  fun deleteBookmark(
    bookmarkId: String,
    userId: String,
  )

  fun deleteBookmarksByUserId(userId: String)

  fun deleteBookmarksByBookId(bookId: String)

  fun deleteAllBookmarks()
}
