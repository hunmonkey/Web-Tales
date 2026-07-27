package org.tales.shelfview.interfaces.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.commons.io.FilenameUtils
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.model.Media
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.infrastructure.security.TalesPrincipal
import org.tales.infrastructure.web.getMediaTypeOrDefault
import org.tales.interfaces.api.ContentRestrictionChecker
import org.tales.interfaces.api.getBookLastModified
import org.tales.interfaces.api.persistence.BookDtoRepository
import org.tales.interfaces.api.setImageLastModified
import java.nio.charset.StandardCharsets
import org.tales.domain.model.MediaType as TalesMediaType

@RestController
@RequestMapping("api/shelfview/v1")
@Tag(name = "ShelfView")
class ShelfViewMixedPageController(
  private val bookDtoRepository: BookDtoRepository,
  private val bookRepository: BookRepository,
  private val mediaRepository: MediaRepository,
  private val contentRestrictionChecker: ContentRestrictionChecker,
  private val zipMixedContentProvider: ShelfViewZipMixedContentProvider,
  private val contentDetector: ContentDetector,
) {
  @Operation(summary = "Get ShelfView page content from a mixed ZIP item")
  @GetMapping(
    value = ["books/{bookId}/mixed/{itemId}/pages/{pageNumber}"],
    produces = [MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE, "image/webp"],
  )
  fun getMixedPage(
    @AuthenticationPrincipal principal: TalesPrincipal,
    request: ServletWebRequest,
    @PathVariable bookId: String,
    @PathVariable itemId: String,
    @PathVariable pageNumber: Int,
    @RequestParam(name = "zero_based", defaultValue = "false") zeroBasedIndex: Boolean,
  ): ResponseEntity<ByteArray> {
    val bookDto = bookDtoRepository.findByIdOrNull(bookId, principal.user.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    contentRestrictionChecker.checkContentRestriction(principal.user, bookDto)

    val media = mediaRepository.findByIdOrNull(bookDto.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    ensureReady(media)
    if (TalesMediaType.fromMediaType(media.mediaType) != TalesMediaType.ZIP) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_MIXED_UNSUPPORTED_MEDIA")
    }
    if (request.checkNotModified(getBookLastModified(media))) {
      return ResponseEntity
        .status(HttpStatus.NOT_MODIFIED)
        .setImageLastModified(media)
        .body(ByteArray(0))
    }

    val book = bookRepository.findByIdOrNull(bookDto.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val bookContentFingerprint = shelfViewContentFingerprint(bookDto, media)
    val page =
      zipMixedContentProvider.readPage(
        path = book.path,
        bookContentFingerprint = bookContentFingerprint,
        itemId = itemId,
        pageNumber = if (zeroBasedIndex) pageNumber + 1 else pageNumber,
      )

    return ResponseEntity
      .ok()
      .headers(
        HttpHeaders().apply {
          val extension = contentDetector.mediaTypeToExtension(page.mediaType) ?: ".bin"
          val baseName = FilenameUtils.getBaseName(page.normalizedEntryName).ifBlank { "mixed-item" }
          contentDisposition =
            ContentDisposition
              .builder("inline")
              .filename("$baseName-${page.pageNumber}$extension", StandardCharsets.UTF_8)
              .build()
        },
      ).contentType(getMediaTypeOrDefault(page.mediaType))
      .setImageLastModified(media)
      .body(page.bytes)
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
