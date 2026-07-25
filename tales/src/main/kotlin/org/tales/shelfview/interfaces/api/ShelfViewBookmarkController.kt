package org.tales.shelfview.interfaces.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.persistence.MediaRepository
import org.tales.infrastructure.security.TalesPrincipal
import org.tales.interfaces.api.ContentRestrictionChecker
import org.tales.interfaces.api.persistence.BookDtoRepository
import org.tales.interfaces.api.rest.dto.BookDto
import org.tales.shelfview.domain.ShelfViewBookmark
import org.tales.shelfview.domain.persistence.ShelfViewBookmarkRepository
import org.tales.shelfview.interfaces.api.dto.ShelfViewBookmarkDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewBookmarkUpdateDto
import org.tales.shelfview.interfaces.api.dto.toDto

@RestController
@RequestMapping("api/shelfview/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ShelfView")
class ShelfViewBookmarkController(
  private val bookDtoRepository: BookDtoRepository,
  private val mediaRepository: MediaRepository,
  private val contentRestrictionChecker: ContentRestrictionChecker,
  private val bookmarkRepository: ShelfViewBookmarkRepository,
  private val mapper: ObjectMapper,
) {
  @Operation(summary = "List ShelfView bookmarks")
  @GetMapping("books/{bookId}/bookmarks")
  fun getBookmarks(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable bookId: String,
  ): Collection<ShelfViewBookmarkDto> {
    val book = findAuthorizedBook(bookId, principal)

    return bookmarkRepository
      .findBookmarks(book.id, principal.user.id)
      .map { it.toDto(mapper) }
  }

  @Operation(summary = "Create ShelfView bookmark")
  @PostMapping("books/{bookId}/bookmarks", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  fun createBookmark(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable bookId: String,
    @RequestBody request: ShelfViewBookmarkUpdateDto,
  ): ShelfViewBookmarkDto {
    val book = findAuthorizedBook(bookId, principal)
    validateLocator(book, request)
    val bookmark =
      ShelfViewBookmark(
        userId = principal.user.id,
        bookId = book.id,
        locatorJson = mapper.writeValueAsString(request.locator),
        label = request.label,
        note = request.note,
      )

    bookmarkRepository.saveBookmark(bookmark)

    return bookmarkRepository.findBookmarkByIdAndUserIdOrNull(bookmark.id, principal.user.id)?.toDto(mapper)
      ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SHELFVIEW_BOOKMARK_SAVE_FAILED")
  }

  @Operation(summary = "Update ShelfView bookmark")
  @PutMapping("bookmarks/{bookmarkId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
  fun updateBookmark(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable bookmarkId: String,
    @RequestBody request: ShelfViewBookmarkUpdateDto,
  ): ShelfViewBookmarkDto {
    val existing = bookmarkRepository.findBookmarkByIdAndUserIdOrNull(bookmarkId, principal.user.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val book = findAuthorizedBook(existing.bookId, principal)
    validateLocator(book, request)

    val bookmark =
      existing.copy(
        locatorJson = mapper.writeValueAsString(request.locator),
        label = request.label,
        note = request.note,
      )

    bookmarkRepository.saveBookmark(bookmark)

    return bookmarkRepository.findBookmarkByIdAndUserIdOrNull(bookmark.id, principal.user.id)?.toDto(mapper)
      ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SHELFVIEW_BOOKMARK_SAVE_FAILED")
  }

  @Operation(summary = "Delete ShelfView bookmark")
  @DeleteMapping("bookmarks/{bookmarkId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteBookmark(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable bookmarkId: String,
  ) {
    val existing = bookmarkRepository.findBookmarkByIdAndUserIdOrNull(bookmarkId, principal.user.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    findAuthorizedBook(existing.bookId, principal)

    bookmarkRepository.deleteBookmark(bookmarkId, principal.user.id)
  }

  private fun validateLocator(
    book: BookDto,
    request: ShelfViewBookmarkUpdateDto,
  ): ShelfViewValidatedLocator {
    val media = mediaRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    return ShelfViewLocatorValidator.validate(
      locator = request.locator,
      expectedContentFingerprint = shelfViewContentFingerprint(book, media),
      pageCount = shelfViewPageCount(media),
    )
  }

  private fun findAuthorizedBook(
    bookId: String,
    principal: TalesPrincipal,
  ) = bookDtoRepository.findByIdOrNull(bookId, principal.user.id)?.also {
    contentRestrictionChecker.checkContentRestriction(principal.user, it)
  } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
}
