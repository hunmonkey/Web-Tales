package org.tales.shelfview.interfaces.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.tales.domain.model.Media
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.infrastructure.security.TalesPrincipal
import org.tales.interfaces.api.ContentRestrictionChecker
import org.tales.interfaces.api.persistence.BookDtoRepository
import org.tales.shelfview.interfaces.api.dto.ShelfViewTextContentDto
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.model.MediaType as TalesMediaType

@RestController
@RequestMapping("api/shelfview/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ShelfView")
class ShelfViewTextController(
  private val bookDtoRepository: BookDtoRepository,
  private val bookRepository: BookRepository,
  private val mediaRepository: MediaRepository,
  private val contentRestrictionChecker: ContentRestrictionChecker,
  private val textContentProvider: ShelfViewTextContentProvider,
) {
  @Operation(summary = "Get ShelfView text content")
  @GetMapping("books/{bookId}/text")
  fun getText(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable bookId: String,
    @RequestParam(required = false) encoding: String? = null,
  ): ShelfViewTextContentDto {
    val bookDto = bookDtoRepository.findByIdOrNull(bookId, principal.user.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    contentRestrictionChecker.checkContentRestriction(principal.user, bookDto)

    val media = mediaRepository.findByIdOrNull(bookDto.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    ensureReady(media)
    if (TalesMediaType.fromMediaType(media.mediaType) != TalesMediaType.TXT) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_TEXT_UNSUPPORTED_MEDIA")
    }

    val book = bookRepository.findByIdOrNull(bookDto.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    return textContentProvider.read(book, shelfViewContentFingerprint(bookDto, media), encoding)
  }

  private fun ensureReady(media: Media) {
    when (media.status) {
      Media.Status.UNKNOWN -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "Book has not been analyzed yet")
      Media.Status.OUTDATED -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "Book is outdated and must be re-analyzed")
      Media.Status.ERROR -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "Book analysis failed")
      Media.Status.UNSUPPORTED -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "Book format is not supported")
      Media.Status.READY -> Unit
    }
  }
}
