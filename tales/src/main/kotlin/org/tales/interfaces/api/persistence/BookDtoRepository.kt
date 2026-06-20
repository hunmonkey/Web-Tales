package org.tales.interfaces.api.persistence

import org.tales.domain.model.BookSearch
import org.tales.domain.model.ContentRestrictions
import org.tales.domain.model.ReadList
import org.tales.domain.model.SearchContext
import org.tales.interfaces.api.rest.dto.BookDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BookDtoRepository {
  fun findAll(pageable: Pageable): Page<BookDto>

  fun findAll(
    context: SearchContext,
    pageable: Pageable,
  ): Page<BookDto>

  fun findAll(
    search: BookSearch,
    context: SearchContext,
    pageable: Pageable,
  ): Page<BookDto>

  fun findByIdOrNull(
    bookId: String,
    userId: String,
  ): BookDto?

  fun findPreviousInSeriesOrNull(
    bookId: String,
    userId: String,
  ): BookDto?

  fun findNextInSeriesOrNull(
    bookId: String,
    userId: String,
  ): BookDto?

  fun findPreviousInReadListOrNull(
    readList: ReadList,
    bookId: String,
    userId: String,
    filterOnLibraryIds: Collection<String>?,
    restrictions: ContentRestrictions = ContentRestrictions(),
  ): BookDto?

  fun findNextInReadListOrNull(
    readList: ReadList,
    bookId: String,
    userId: String,
    filterOnLibraryIds: Collection<String>?,
    restrictions: ContentRestrictions = ContentRestrictions(),
  ): BookDto?

  fun findAllOnDeck(
    userId: String,
    filterOnLibraryIds: Collection<String>?,
    pageable: Pageable,
    restrictions: ContentRestrictions = ContentRestrictions(),
  ): Page<BookDto>

  fun findAllDuplicates(
    userId: String,
    pageable: Pageable,
  ): Page<BookDto>
}
