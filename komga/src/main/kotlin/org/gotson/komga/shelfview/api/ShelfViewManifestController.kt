package org.gotson.komga.shelfview.api

import org.apache.commons.io.FilenameUtils
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.MediaProfile
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.interfaces.api.ContentRestrictionChecker
import org.gotson.komga.interfaces.api.persistence.BookDtoRepository
import org.gotson.komga.interfaces.api.rest.dto.BookDto
import org.gotson.komga.shelfview.api.dto.ShelfViewCapabilitiesDto
import org.gotson.komga.shelfview.api.dto.ShelfViewManifestDto
import org.gotson.komga.shelfview.api.dto.ShelfViewPagesDto
import org.gotson.komga.shelfview.domain.ShelfViewPublicationKind
import org.gotson.komga.shelfview.domain.ShelfViewSourceFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.gotson.komga.domain.model.MediaType as KomgaMediaType

@RestController
@RequestMapping("api/shelfview/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
class ShelfViewManifestController(
  private val bookDtoRepository: BookDtoRepository,
  private val mediaRepository: MediaRepository,
  private val contentRestrictionChecker: ContentRestrictionChecker,
) {
  @GetMapping("books/{bookId}/manifest")
  fun getManifest(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @PathVariable bookId: String,
  ): ShelfViewManifestDto {
    val book = bookDtoRepository.findByIdOrNull(bookId, principal.user.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    contentRestrictionChecker.checkContentRestriction(principal.user, book)

    val media = mediaRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    ensureReady(media)

    val mediaType =
      KomgaMediaType.fromMediaType(media.mediaType)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Book analysis failed")

    if (mediaType.profile !in fixedPageProfiles) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Book media type '${media.mediaType}' is not supported by ShelfView fixed-page manifest yet")
    }

    return ShelfViewManifestDto(
      bookId = book.id,
      title = book.metadata.title.ifBlank { book.name },
      kind = ShelfViewPublicationKind.FIXED_PAGE_SEQUENCE,
      sourceFormat = sourceFormat(book, mediaType),
      contentFingerprint = shelfViewContentFingerprint(book, media),
      pageCount = shelfViewPageCount(media),
      pages = ShelfViewPagesDto(endpointTemplate = "/api/v1/books/${book.id}/pages/{page}?zero_based=true"),
      capabilities = fixedPageCapabilities,
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

  private fun sourceFormat(
    book: BookDto,
    mediaType: KomgaMediaType,
  ): ShelfViewSourceFormat =
    when (mediaType) {
      KomgaMediaType.ZIP -> if (FilenameUtils.getExtension(book.url).equals("zip", ignoreCase = true)) ShelfViewSourceFormat.ZIP else ShelfViewSourceFormat.CBZ
      KomgaMediaType.RAR_GENERIC,
      KomgaMediaType.RAR_4,
      KomgaMediaType.RAR_5,
      -> ShelfViewSourceFormat.CBR
      KomgaMediaType.PDF -> ShelfViewSourceFormat.PDF
      KomgaMediaType.EPUB -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "EPUB is not supported by ShelfView fixed-page manifest yet")
      KomgaMediaType.TXT -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "TXT is handled by the ShelfView text manifest, not the fixed-page manifest")
    }

  private companion object {
    val fixedPageProfiles = setOf(MediaProfile.DIVINA, MediaProfile.PDF)

    val fixedPageCapabilities =
      ShelfViewCapabilitiesDto(
        search = false,
        textSelection = false,
        doublePage = true,
        continuousScroll = true,
        rtl = true,
        imageFilters = true,
      )
  }
}
