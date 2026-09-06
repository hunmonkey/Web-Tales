package org.tales.shelfview.interfaces.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.commons.io.FilenameUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.model.Media
import org.tales.domain.model.MediaFile
import org.tales.domain.model.MediaProfile
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.domain.persistence.ReadProgressRepository
import org.tales.domain.service.BookLifecycle
import org.tales.infrastructure.security.TalesPrincipal
import org.tales.interfaces.api.ContentRestrictionChecker
import org.tales.interfaces.api.persistence.BookDtoRepository
import org.tales.interfaces.api.rest.dto.BookDto
import org.tales.shelfview.domain.ShelfViewReaderPosition
import org.tales.shelfview.domain.persistence.ShelfViewReaderPositionRepository
import org.tales.shelfview.interfaces.api.dto.ShelfViewPositionDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewPositionUpdateDto
import org.tales.shelfview.interfaces.api.dto.toDto
import org.tales.domain.model.MediaType as TalesMediaType

@RestController
@RequestMapping("api/shelfview/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ShelfView")
class ShelfViewPositionController(
  private val bookDtoRepository: BookDtoRepository,
  private val bookRepository: BookRepository,
  private val mediaRepository: MediaRepository,
  private val readProgressRepository: ReadProgressRepository,
  private val bookLifecycle: BookLifecycle,
  private val contentRestrictionChecker: ContentRestrictionChecker,
  private val readerPositionRepository: ShelfViewReaderPositionRepository,
  private val locatorValidator: ShelfViewLocatorValidator,
  private val mapper: ObjectMapper,
) {
  @Operation(summary = "Get ShelfView reader position")
  @GetMapping("books/{bookId}/position")
  fun getPosition(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable bookId: String,
  ): ResponseEntity<ShelfViewPositionDto> {
    val book = findAuthorizedBook(bookId, principal)
    val media = mediaRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val savedPosition = readerPositionRepository.findPositionOrNull(book.id, principal.user.id)
    val talesPosition = talesFixedPagePositionOrNull(book, media, principal.user.id)
    val position =
      when {
        savedPosition == null -> talesPosition
        talesPosition == null -> savedPosition
        talesPosition.updatedAt.isAfter(savedPosition.updatedAt) -> talesPosition
        else -> savedPosition
      }

    return position?.let {
      ResponseEntity.ok(it.toDto(mapper))
    } ?: ResponseEntity.noContent().build()
  }

  @Operation(summary = "Save ShelfView reader position")
  @PutMapping("books/{bookId}/position", consumes = [MediaType.APPLICATION_JSON_VALUE])
  fun savePosition(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable bookId: String,
    @RequestBody request: ShelfViewPositionUpdateDto,
  ): ShelfViewPositionDto {
    val book = findAuthorizedBook(bookId, principal)
    val media = mediaRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val validatedLocator =
      locatorValidator.validate(
        locator = request.locator,
        progression = request.progression,
        book = book,
        media = media,
      )

    val position =
      ShelfViewReaderPosition(
        userId = principal.user.id,
        bookId = book.id,
        locatorJson = mapper.writeValueAsString(request.locator),
        progression = request.progression!!,
        contentFingerprint = validatedLocator.contentFingerprint,
      )

    readerPositionRepository.savePosition(position)
    syncTalesProgressIfFixedPage(book, media, request, principal)

    return readerPositionRepository.findPositionOrNull(book.id, principal.user.id)?.toDto(mapper)
      ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SHELFVIEW_POSITION_SAVE_FAILED")
  }

  private fun findAuthorizedBook(
    bookId: String,
    principal: TalesPrincipal,
  ) = bookDtoRepository.findByIdOrNull(bookId, principal.user.id)?.also {
    contentRestrictionChecker.checkContentRestriction(principal.user, it)
  } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

  private fun talesFixedPagePositionOrNull(
    book: BookDto,
    media: Media,
    userId: String,
  ): ShelfViewReaderPosition? {
    if (!supportsTalesFixedPageProgress(book, media)) return null

    val pageCount = shelfViewPageCount(media)
    val readProgress = readProgressRepository.findByBookIdAndUserIdOrNull(book.id, userId) ?: return null
    if (pageCount < 1 || readProgress.page !in 1..pageCount) return null

    val contentFingerprint = shelfViewContentFingerprint(book, media)
    val locator =
      mapper
        .createObjectNode()
        .put("schemaVersion", 1)
        .put("kind", "PAGE")
        .put("page", readProgress.page - 1)
        .put("offset", 0.0)
        .put("contentFingerprint", contentFingerprint)

    return ShelfViewReaderPosition(
      userId = userId,
      bookId = book.id,
      locatorJson = mapper.writeValueAsString(locator),
      progression = (readProgress.page.toFloat() / pageCount.toFloat()).coerceIn(0F, 1F),
      contentFingerprint = contentFingerprint,
      updatedAt = readProgress.readDate,
    )
  }

  private fun syncTalesProgressIfFixedPage(
    book: BookDto,
    media: Media,
    request: ShelfViewPositionUpdateDto,
    principal: TalesPrincipal,
  ) {
    val locator = request.locator ?: return
    if (locator.get("kind")?.textValue() != "PAGE") return
    if (!supportsTalesFixedPageProgress(book, media)) return

    val page =
      locator
        .get("page")
        ?.takeIf { it.isIntegralNumber }
        ?.intValue()
        ?.plus(1)
        ?: return
    if (page !in 1..shelfViewPageCount(media)) return

    val domainBook = bookRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    bookLifecycle.markReadProgress(domainBook, principal.user, page)
  }

  private fun supportsTalesFixedPageProgress(
    book: BookDto,
    media: Media,
  ): Boolean {
    val mediaType = TalesMediaType.fromMediaType(media.mediaType) ?: return false
    return when (mediaType.profile) {
      MediaProfile.DIVINA ->
        !(
          mediaType == TalesMediaType.ZIP &&
            FilenameUtils.getExtension(book.url).equals("zip", ignoreCase = true) &&
            hasMixedZipEntries(media.files)
        )
      MediaProfile.PDF,
      MediaProfile.IMAGE_DIRECTORY,
      -> true
      MediaProfile.EPUB,
      MediaProfile.TXT,
      -> false
    }
  }

  private fun hasMixedZipEntries(files: List<MediaFile>): Boolean =
    files.any { file ->
      val extension = FilenameUtils.getExtension(file.fileName).lowercase()
      extension == "txt" || extension == "pdf"
    }
}
