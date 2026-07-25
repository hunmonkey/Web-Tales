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
import org.tales.shelfview.domain.ShelfViewHighlight
import org.tales.shelfview.domain.persistence.ShelfViewHighlightRepository
import org.tales.shelfview.interfaces.api.dto.ShelfViewHighlightDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewHighlightUpdateDto
import org.tales.shelfview.interfaces.api.dto.toDto

@RestController
@RequestMapping("api/shelfview/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ShelfView")
class ShelfViewHighlightController(
  private val bookDtoRepository: BookDtoRepository,
  private val mediaRepository: MediaRepository,
  private val contentRestrictionChecker: ContentRestrictionChecker,
  private val highlightRepository: ShelfViewHighlightRepository,
  private val mapper: ObjectMapper,
) {
  @Operation(summary = "List ShelfView highlights")
  @GetMapping("books/{bookId}/highlights")
  fun getHighlights(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable bookId: String,
  ): Collection<ShelfViewHighlightDto> {
    val book = findAuthorizedBook(bookId, principal)

    return highlightRepository
      .findHighlights(book.id, principal.user.id)
      .map { it.toDto(mapper) }
  }

  @Operation(summary = "Create ShelfView highlight")
  @PostMapping("books/{bookId}/highlights", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  fun createHighlight(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable bookId: String,
    @RequestBody request: ShelfViewHighlightUpdateDto,
  ): ShelfViewHighlightDto {
    val book = findAuthorizedBook(bookId, principal)
    val locator = validateLocator(book, request)
    val highlightData =
      ShelfViewHighlightValidator.validate(
        locator = request.locator,
        selector = request.selector,
        color = request.color,
        note = request.note,
        textQuote = request.textQuote,
      )
    val highlight =
      ShelfViewHighlight(
        userId = principal.user.id,
        bookId = book.id,
        locatorJson = mapper.writeValueAsString(request.locator),
        selectorJson = mapper.writeValueAsString(request.selector),
        contentFingerprint = locator.contentFingerprint,
        color = highlightData.color,
        note = highlightData.note,
        textQuote = highlightData.textQuote,
      )

    highlightRepository.saveHighlight(highlight)

    return highlightRepository.findHighlightByIdAndUserIdOrNull(highlight.id, principal.user.id)?.toDto(mapper)
      ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SHELFVIEW_HIGHLIGHT_SAVE_FAILED")
  }

  @Operation(summary = "Update ShelfView highlight")
  @PutMapping("highlights/{highlightId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
  fun updateHighlight(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable highlightId: String,
    @RequestBody request: ShelfViewHighlightUpdateDto,
  ): ShelfViewHighlightDto {
    val existing = highlightRepository.findHighlightByIdAndUserIdOrNull(highlightId, principal.user.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val book = findAuthorizedBook(existing.bookId, principal)
    val locator = validateLocator(book, request)
    val highlightData =
      ShelfViewHighlightValidator.validate(
        locator = request.locator,
        selector = request.selector,
        color = request.color,
        note = request.note,
        textQuote = request.textQuote,
      )
    val highlight =
      existing.copy(
        locatorJson = mapper.writeValueAsString(request.locator),
        selectorJson = mapper.writeValueAsString(request.selector),
        contentFingerprint = locator.contentFingerprint,
        color = highlightData.color,
        note = highlightData.note,
        textQuote = highlightData.textQuote,
      )

    highlightRepository.saveHighlight(highlight)

    return highlightRepository.findHighlightByIdAndUserIdOrNull(highlight.id, principal.user.id)?.toDto(mapper)
      ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SHELFVIEW_HIGHLIGHT_SAVE_FAILED")
  }

  @Operation(summary = "Delete ShelfView highlight")
  @DeleteMapping("highlights/{highlightId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteHighlight(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable highlightId: String,
  ) {
    val existing = highlightRepository.findHighlightByIdAndUserIdOrNull(highlightId, principal.user.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    findAuthorizedBook(existing.bookId, principal)

    highlightRepository.deleteHighlight(highlightId, principal.user.id)
  }

  private fun validateLocator(
    book: BookDto,
    request: ShelfViewHighlightUpdateDto,
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
