package org.gotson.komga.shelfview.interfaces.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.commons.io.FilenameUtils
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.MediaProfile
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.infrastructure.mediacontainer.ContentDetector
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.infrastructure.web.getMediaTypeOrDefault
import org.gotson.komga.interfaces.api.ContentRestrictionChecker
import org.gotson.komga.interfaces.api.getBookLastModified
import org.gotson.komga.interfaces.api.persistence.BookDtoRepository
import org.gotson.komga.interfaces.api.setNotModified
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
import java.nio.charset.StandardCharsets
import org.gotson.komga.domain.model.MediaType as KomgaMediaType

@RestController
@RequestMapping("api/shelfview/v1")
@Tag(name = "ShelfView")
class ShelfViewImageDirectoryPageController(
  private val bookDtoRepository: BookDtoRepository,
  private val bookRepository: BookRepository,
  private val mediaRepository: MediaRepository,
  private val contentRestrictionChecker: ContentRestrictionChecker,
  private val imageDirectoryContentProvider: ShelfViewImageDirectoryContentProvider,
  private val contentDetector: ContentDetector,
) {
  @Operation(summary = "Get ShelfView page content from an image-directory book")
  @GetMapping(
    value = ["books/{bookId}/image-directory/pages/{pageNumber}"],
    produces = [MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE, "image/webp"],
  )
  fun getImageDirectoryPage(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    request: ServletWebRequest,
    @PathVariable bookId: String,
    @PathVariable pageNumber: Int,
    @RequestParam(name = "zero_based", defaultValue = "false") zeroBasedIndex: Boolean,
  ): ResponseEntity<ByteArray> {
    val bookDto = bookDtoRepository.findByIdOrNull(bookId, principal.user.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    contentRestrictionChecker.checkContentRestriction(principal.user, bookDto)

    val media = mediaRepository.findByIdOrNull(bookDto.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    ensureReadyImageDirectory(media)
    if (request.checkNotModified(getBookLastModified(media))) {
      return ResponseEntity
        .status(HttpStatus.NOT_MODIFIED)
        .setNotModified(media)
        .body(ByteArray(0))
    }

    val book = bookRepository.findByIdOrNull(bookDto.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val page =
      imageDirectoryContentProvider.readPage(
        directory = book.path,
        media = media,
        pageNumber = if (zeroBasedIndex) pageNumber + 1 else pageNumber,
      )

    return ResponseEntity
      .ok()
      .headers(
        HttpHeaders().apply {
          val extension = contentDetector.mediaTypeToExtension(page.mediaType) ?: ".bin"
          val baseName = FilenameUtils.getBaseName(page.fileName).ifBlank { "page" }
          contentDisposition =
            ContentDisposition
              .builder("inline")
              .filename("$baseName$extension", StandardCharsets.UTF_8)
              .build()
        },
      ).contentType(getMediaTypeOrDefault(page.mediaType))
      .setNotModified(media)
      .body(page.bytes)
  }

  private fun ensureReadyImageDirectory(media: Media) {
    when (media.status) {
      Media.Status.UNKNOWN -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "Book has not been analyzed yet")
      Media.Status.OUTDATED -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "Book is outdated and must be re-analyzed")
      Media.Status.ERROR -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "Book analysis failed")
      Media.Status.UNSUPPORTED -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "Book format is not supported")
      Media.Status.READY -> Unit
    }

    if (KomgaMediaType.fromMediaType(media.mediaType)?.profile != MediaProfile.IMAGE_DIRECTORY) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_IMAGE_DIRECTORY_UNSUPPORTED_MEDIA")
    }
  }
}
