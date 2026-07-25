package org.tales.domain.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.tales.domain.model.BookPageNumbered
import org.tales.domain.model.PageHashKnown
import org.tales.domain.model.PageHashMatch
import org.tales.domain.model.PageHashUnknown

interface PageHashRepository {
  fun findKnown(pageHash: String): PageHashKnown?

  fun findAllKnown(
    actions: List<PageHashKnown.Action>?,
    pageable: Pageable,
  ): Page<PageHashKnown>

  fun findAllUnknown(pageable: Pageable): Page<PageHashUnknown>

  fun findMatchesByHash(
    pageHash: String,
    pageable: Pageable,
  ): Page<PageHashMatch>

  fun findMatchesByKnownHashAction(
    actions: List<PageHashKnown.Action>?,
    libraryId: String?,
  ): Map<String, Collection<BookPageNumbered>>

  fun getKnownThumbnail(pageHash: String): ByteArray?

  fun insert(
    pageHash: PageHashKnown,
    thumbnail: ByteArray?,
  )

  fun update(pageHash: PageHashKnown)
}
