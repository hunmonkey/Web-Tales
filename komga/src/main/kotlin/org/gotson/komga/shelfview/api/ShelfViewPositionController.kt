package org.gotson.komga.shelfview.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.interfaces.api.ContentRestrictionChecker
import org.gotson.komga.interfaces.api.persistence.BookDtoRepository
import org.gotson.komga.shelfview.api.dto.ShelfViewPositionDto
import org.gotson.komga.shelfview.api.dto.ShelfViewPositionUpdateDto
import org.gotson.komga.shelfview.api.dto.toDto
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

@RestController
@RequestMapping("api/shelfview/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ShelfView")
class ShelfViewPositionController(
  private val bookDtoRepository: BookDtoRepository,
  private val mediaRepository: MediaRepository,
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

    return readerPositionRepository.findPositionOrNull(book.id, principal.user.id)?.let {
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

    return readerPositionRepository.findPositionOrNull(book.id, principal.user.id)?.toDto(mapper)
      ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SHELFVIEW_POSITION_SAVE_FAILED")
  }

  private fun findAuthorizedBook(
    bookId: String,
    principal: KomgaPrincipal,
  ) = bookDtoRepository.findByIdOrNull(bookId, principal.user.id)?.also {
    contentRestrictionChecker.checkContentRestriction(principal.user, it)
  } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
}
