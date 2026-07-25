package org.tales.interfaces.api

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.model.Book
import org.tales.domain.model.TalesUser
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.SeriesMetadataRepository
import org.tales.interfaces.api.rest.dto.BookDto
import org.tales.interfaces.api.rest.dto.SeriesDto

@Component
class ContentRestrictionChecker(
  private val seriesMetadataRepository: SeriesMetadataRepository,
  private val bookRepository: BookRepository,
) {
  /**
   * Convenience function to check for content restriction.
   * This will retrieve data from repositories if needed.
   *
   * @throws[ResponseStatusException] if the user cannot access the content
   */
  fun checkContentRestriction(
    talesUser: TalesUser,
    book: BookDto,
  ) {
    if (!talesUser.canAccessLibrary(book.libraryId)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
    if (talesUser.restrictions.isRestricted)
      seriesMetadataRepository.findById(book.seriesId).let {
        if (!talesUser.isContentAllowed(it.ageRating, it.sharingLabels)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
      }
  }

  /**
   * Convenience function to check for content restriction.
   * This will retrieve data from repositories if needed.
   *
   * @throws[ResponseStatusException] if the user cannot access the content
   */
  fun checkContentRestriction(
    talesUser: TalesUser,
    book: Book,
  ) {
    if (!talesUser.canAccessLibrary(book.libraryId)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
    if (talesUser.restrictions.isRestricted)
      seriesMetadataRepository.findById(book.seriesId).let {
        if (!talesUser.isContentAllowed(it.ageRating, it.sharingLabels)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
      }
  }

  /**
   * Convenience function to check for content restriction.
   * This will retrieve data from repositories if needed.
   *
   * @throws[ResponseStatusException] if the user cannot access the content
   */
  fun checkContentRestriction(
    talesUser: TalesUser,
    bookId: String,
  ) {
    if (!talesUser.canAccessAllLibraries()) {
      bookRepository.getLibraryIdOrNull(bookId)?.let {
        if (!talesUser.canAccessLibrary(it)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
      } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
    if (talesUser.restrictions.isRestricted)
      bookRepository.getSeriesIdOrNull(bookId)?.let { seriesId ->
        seriesMetadataRepository.findById(seriesId).let {
          if (!talesUser.isContentAllowed(it.ageRating, it.sharingLabels)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
      } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
  }

  /**
   * Convenience function to check for content restriction.
   *
   * @throws[ResponseStatusException] if the user cannot access the content
   */
  fun checkContentRestriction(
    talesUser: TalesUser,
    series: SeriesDto,
  ) {
    if (!talesUser.canAccessLibrary(series.libraryId)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
    if (!talesUser.isContentAllowed(series.metadata.ageRating, series.metadata.sharingLabels)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
  }
}
