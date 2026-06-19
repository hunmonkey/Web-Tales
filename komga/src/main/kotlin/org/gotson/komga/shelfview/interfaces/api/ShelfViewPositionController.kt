package org.gotson.komga.shelfview.interfaces.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.commons.io.FilenameUtils
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.MediaFile
import org.gotson.komga.domain.model.MediaProfile
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.domain.persistence.ReadProgressRepository
import org.gotson.komga.domain.service.BookLifecycle
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.interfaces.api.ContentRestrictionChecker
import org.gotson.komga.interfaces.api.persistence.BookDtoRepository
import org.gotson.komga.interfaces.api.rest.dto.BookDto
import org.gotson.komga.shelfview.interfaces.api.dto.ShelfViewPositionDto
import org.gotson.komga.shelfview.interfaces.api.dto.ShelfViewPositionUpdateDto
import org.gotson.komga.shelfview.interfaces.api.dto.toDto
import org.gotson.komga.shelfview.domain.ShelfViewReaderPosition
import org.gotson.komga.shelfview.domain.persistence.ShelfViewReaderPositionRepository
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
import org.gotson.komga.domain.model.MediaType as KomgaMediaType

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
  private val mapper: ObjectMapper,
) {
  @Operation(summary = "Get ShelfView reader position")
  @GetMapping("books/{bookId}/position")
  fun getPosition(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @PathVariable bookId: String,
  ): ResponseEntity<ShelfViewPositionDto> {
    val book = findAuthorizedBook(bookId, principal)
    val media = mediaRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val savedPosition = readerPositionRepository.findPositionOrNull(book.id, principal.user.id)
    val komgaPosition = komgaFixedPagePositionOrNull(book, media, principal.user.id)
    val position =
      when {
        savedPosition == null -> komgaPosition
        komgaPosition == null -> savedPosition
        komgaPosition.updatedAt.isAfter(savedPosition.updatedAt) -> komgaPosition
        else -> savedPosition
      }

    return position?.let {
      ResponseEntity.ok(it.toDto(mapper))
    } ?: ResponseEntity.noContent().build()
  }

  @Operation(summary = "Save ShelfView reader position")
  @PutMapping("books/{bookId}/position", consumes = [MediaType.APPLICATION_JSON_VALUE])
  fun savePosition(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @PathVariable bookId: String,
    @RequestBody request: ShelfViewPositionUpdateDto,
  ): ShelfViewPositionDto {
    val book = findAuthorizedBook(bookId, principal)
    val media = mediaRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val contentFingerprint = shelfViewContentFingerprint(book, media)
    val validatedLocator =
      ShelfViewLocatorValidator.validate(
        locator = request.locator,
        progression = request.progression,
        expectedContentFingerprint = contentFingerprint,
        pageCount = shelfViewPageCount(media),
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
    syncKomgaProgressIfFixedPage(book, media, request, principal)

    return readerPositionRepository.findPositionOrNull(book.id, principal.user.id)?.toDto(mapper)
      ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SHELFVIEW_POSITION_SAVE_FAILED")
  }

  private fun findAuthorizedBook(
    bookId: String,
    principal: KomgaPrincipal,
  ) = bookDtoRepository.findByIdOrNull(bookId, principal.user.id)?.also {
    contentRestrictionChecker.checkContentRestriction(principal.user, it)
  } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

  private fun komgaFixedPagePositionOrNull(
    book: BookDto,
    media: Media,
    userId: String,
  ): ShelfViewReaderPosition? {
    if (!supportsKomgaFixedPageProgress(book, media)) return null

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

  private fun syncKomgaProgressIfFixedPage(
    book: BookDto,
    media: Media,
    request: ShelfViewPositionUpdateDto,
    principal: KomgaPrincipal,
  ) {
    val locator = request.locator ?: return
    if (locator.get("kind")?.textValue() != "PAGE") return
    if (!supportsKomgaFixedPageProgress(book, media)) return

    val page =
      locator
        .get("page")
        ?.takeIf { it.isIntegralNumber }
        ?.intValue()
        ?.plus(1)
        ?: return
    if (page !in 1..media.pageCount) return

    val domainBook = bookRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    bookLifecycle.markReadProgress(domainBook, principal.user, page)
  }

  private fun supportsKomgaFixedPageProgress(
    book: BookDto,
    media: Media,
  ): Boolean {
    val mediaType = KomgaMediaType.fromMediaType(media.mediaType) ?: return false
    return when (mediaType.profile) {
      MediaProfile.DIVINA ->
        !(
          mediaType == KomgaMediaType.ZIP &&
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
