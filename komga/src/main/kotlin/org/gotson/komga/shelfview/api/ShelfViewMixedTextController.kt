package org.gotson.komga.shelfview.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.interfaces.api.ContentRestrictionChecker
import org.gotson.komga.interfaces.api.persistence.BookDtoRepository
import org.gotson.komga.shelfview.api.dto.ShelfViewTextContentDto
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.gotson.komga.domain.model.MediaType as KomgaMediaType

@RestController
@RequestMapping("api/shelfview/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ShelfView")
class ShelfViewMixedTextController(
  private val bookDtoRepository: BookDtoRepository,
  private val bookRepository: BookRepository,
  private val mediaRepository: MediaRepository,
  private val contentRestrictionChecker: ContentRestrictionChecker,
  private val zipMixedContentProvider: ShelfViewZipMixedContentProvider,
  private val textContentProvider: ShelfViewTextContentProvider,
) {
  @Operation(summary = "Get ShelfView text content from a mixed ZIP item")
  @GetMapping("books/{bookId}/mixed/{itemId}/text")
  fun getMixedText(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @PathVariable bookId: String,
    @PathVariable itemId: String,
    @RequestParam(required = false) encoding: String? = null,
  ): ShelfViewTextContentDto {
    val bookDto = bookDtoRepository.findByIdOrNull(bookId, principal.user.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    contentRestrictionChecker.checkContentRestriction(principal.user, bookDto)

    val media = mediaRepository.findByIdOrNull(bookDto.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    ensureReady(media)
    if (KomgaMediaType.fromMediaType(media.mediaType) != KomgaMediaType.ZIP) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_MIXED_UNSUPPORTED_MEDIA")
    }

    val book = bookRepository.findByIdOrNull(bookDto.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val bookContentFingerprint = shelfViewContentFingerprint(bookDto, media)
    val entry =
      zipMixedContentProvider.readTextEntry(
        path = book.path,
        bookContentFingerprint = bookContentFingerprint,
        itemId = itemId,
        maxBytes = textContentProvider.maxBytes(),
      )

    return textContentProvider.readBytes(
      bookId = book.id,
      contentFingerprint = ShelfViewZipMixedItemIds.itemContentFingerprint(bookContentFingerprint, entry.itemId),
      bytes = entry.bytes,
      requestedEncoding = encoding,
    )
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
