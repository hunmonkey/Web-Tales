package org.tales.shelfview.interfaces.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.io.FilenameUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.model.Media
import org.tales.domain.model.MediaFile
import org.tales.domain.model.MediaProfile
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.infrastructure.security.TalesPrincipal
import org.tales.interfaces.api.ContentRestrictionChecker
import org.tales.interfaces.api.persistence.BookDtoRepository
import org.tales.interfaces.api.rest.dto.BookDto
import org.tales.shelfview.domain.ShelfViewPublicationKind
import org.tales.shelfview.domain.ShelfViewSourceFormat
import org.tales.shelfview.interfaces.api.dto.ShelfViewCapabilitiesDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewEpubDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewManifestContentsItemDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewManifestDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewManifestItemDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewPagesDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewTextDto
import org.tales.domain.model.MediaType as TalesMediaType

@RestController
@RequestMapping("api/shelfview/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
class ShelfViewManifestController(
  private val bookDtoRepository: BookDtoRepository,
  private val bookRepository: BookRepository,
  private val mediaRepository: MediaRepository,
  private val contentRestrictionChecker: ContentRestrictionChecker,
  private val zipMixedContentProvider: ShelfViewZipMixedContentProvider,
  private val pdfContentsProvider: ShelfViewPdfContentsProvider,
  private val epubContentsProvider: ShelfViewEpubContentsProvider,
  private val mapper: ObjectMapper,
) {
  @GetMapping("books/{bookId}/manifest")
  fun getManifest(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable bookId: String,
  ): ShelfViewManifestDto {
    val book = bookDtoRepository.findByIdOrNull(bookId, principal.user.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    contentRestrictionChecker.checkContentRestriction(principal.user, book)

    val media = mediaRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    ensureReady(media)

    val mediaType =
      TalesMediaType.fromMediaType(media.mediaType)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Book analysis failed")
    val contentFingerprint = shelfViewContentFingerprint(book, media)

    if (mediaType.profile == MediaProfile.EPUB) {
      val epubExtension = mediaRepository.findExtensionByIdOrNull(book.id)
      return ShelfViewManifestDto(
        bookId = book.id,
        title = book.metadata.title.ifBlank { book.name },
        kind = ShelfViewPublicationKind.EPUB_WEBPUB,
        sourceFormat = ShelfViewSourceFormat.EPUB,
        contentFingerprint = contentFingerprint,
        epub =
          ShelfViewEpubDto(
            webpubManifestEndpoint = "/api/v1/books/${book.id}/manifest/epub",
            positionsEndpoint = "/api/v1/books/${book.id}/positions",
          ),
        contents = epubContentsProvider.listContents(epubExtension, contentFingerprint),
        capabilities = epubCapabilities,
      )
    }

    if (mediaType.profile == MediaProfile.TXT) {
      return ShelfViewManifestDto(
        bookId = book.id,
        title = book.metadata.title.ifBlank { book.name },
        kind = ShelfViewPublicationKind.TEXT_REFLOW,
        sourceFormat = ShelfViewSourceFormat.TXT,
        contentFingerprint = contentFingerprint,
        text =
          ShelfViewTextDto(
            endpoint = "/api/shelfview/v1/books/${book.id}/text",
            normalizationVersion = SHELFVIEW_TEXT_NORMALIZATION_VERSION,
          ),
        capabilities = textCapabilities,
      )
    }

    val sourceFormat = sourceFormat(book, mediaType)

    if (mediaType == TalesMediaType.ZIP && sourceFormat == ShelfViewSourceFormat.ZIP && hasMixedZipEntries(media.files)) {
      val sourceBook = bookRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
      val items =
        zipMixedContentProvider
          .listManifestItems(sourceBook.path, contentFingerprint)
          .mapIndexed { index, item ->
            item.toManifestItemDto(
              bookId = book.id,
              bookContentFingerprint = contentFingerprint,
              index = index,
            )
          }

      if (items.any { it.kind == ShelfViewPublicationKind.TEXT_REFLOW || it.sourceFormat == ShelfViewSourceFormat.PDF }) {
        return ShelfViewManifestDto(
          bookId = book.id,
          title = book.metadata.title.ifBlank { book.name },
          kind = ShelfViewPublicationKind.MIXED_CONTENT_SEQUENCE,
          sourceFormat = ShelfViewSourceFormat.ZIP,
          contentFingerprint = contentFingerprint,
          pageCount = items.sumOf { it.pageCount ?: 1 },
          items = items,
          contents = items.map { it.toContentsItemDto(contentFingerprint) },
          capabilities = mixedContentCapabilities,
        )
      }
    }

    if (mediaType.profile !in fixedPageProfiles) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Book media type '${media.mediaType}' is not supported by ShelfView fixed-page manifest yet")
    }

    val pageCount = shelfViewPageCount(media)
    val contents =
      if (mediaType == TalesMediaType.PDF) {
        val sourceBook = bookRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        pdfContentsProvider.listContents(sourceBook.path, contentFingerprint, pageCount)
      } else {
        emptyList()
      }

    return ShelfViewManifestDto(
      bookId = book.id,
      title = book.metadata.title.ifBlank { book.name },
      kind = ShelfViewPublicationKind.FIXED_PAGE_SEQUENCE,
      sourceFormat = sourceFormat,
      contentFingerprint = contentFingerprint,
      pageCount = pageCount,
      pages =
        ShelfViewPagesDto(
          endpointTemplate = fixedPageEndpointTemplate(book.id, mediaType),
          thumbnailEndpointTemplate = fixedPageThumbnailEndpointTemplate(book.id, mediaType),
        ),
      contents = contents,
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
    mediaType: TalesMediaType,
  ): ShelfViewSourceFormat =
    when (mediaType) {
      TalesMediaType.ZIP -> if (FilenameUtils.getExtension(book.url).equals("zip", ignoreCase = true)) ShelfViewSourceFormat.ZIP else ShelfViewSourceFormat.CBZ
      TalesMediaType.RAR_GENERIC,
      TalesMediaType.RAR_4,
      TalesMediaType.RAR_5,
      -> ShelfViewSourceFormat.CBR
      TalesMediaType.PDF -> ShelfViewSourceFormat.PDF
      TalesMediaType.EPUB -> ShelfViewSourceFormat.EPUB
      TalesMediaType.TXT -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "TXT is handled by the ShelfView text manifest, not the fixed-page manifest")
      TalesMediaType.IMAGE_DIRECTORY -> ShelfViewSourceFormat.IMAGE_DIRECTORY
    }

  private fun fixedPageEndpointTemplate(
    bookId: String,
    mediaType: TalesMediaType,
  ): String =
    when (mediaType) {
      TalesMediaType.IMAGE_DIRECTORY -> "/api/shelfview/v1/books/$bookId/image-directory/pages/{page}?zero_based=true"
      else -> "/api/v1/books/$bookId/pages/{page}?zero_based=true"
    }

  private fun fixedPageThumbnailEndpointTemplate(
    bookId: String,
    mediaType: TalesMediaType,
  ): String? =
    when (mediaType) {
      TalesMediaType.IMAGE_DIRECTORY -> null
      else -> "/api/v1/books/$bookId/pages/{pageOneBased}/thumbnail"
    }

  private fun hasMixedZipEntries(files: List<MediaFile>): Boolean =
    files.any { file ->
      val extension = FilenameUtils.getExtension(file.fileName).lowercase()
      extension == "txt" || extension == "pdf"
    }

  private fun ShelfViewZipMixedManifestItem.toManifestItemDto(
    bookId: String,
    bookContentFingerprint: String,
    index: Int,
  ): ShelfViewManifestItemDto {
    val itemContentFingerprint = ShelfViewZipMixedItemIds.itemContentFingerprint(bookContentFingerprint, itemId)

    return ShelfViewManifestItemDto(
      itemId = itemId,
      index = index,
      kind = kind,
      label = label,
      contentFingerprint = itemContentFingerprint,
      sourceFormat = sourceFormat,
      pageCount = pageCount,
      text =
        if (kind == ShelfViewPublicationKind.TEXT_REFLOW) {
          ShelfViewTextDto(
            endpoint = "/api/shelfview/v1/books/$bookId/mixed/$itemId/text",
            normalizationVersion = SHELFVIEW_TEXT_NORMALIZATION_VERSION,
          )
        } else {
          null
        },
      pages =
        if (kind == ShelfViewPublicationKind.FIXED_PAGE_SEQUENCE) {
          ShelfViewPagesDto(endpointTemplate = "/api/shelfview/v1/books/$bookId/mixed/$itemId/pages/{page}?zero_based=true")
        } else {
          null
        },
    )
  }

  private fun ShelfViewManifestItemDto.toContentsItemDto(bookContentFingerprint: String): ShelfViewManifestContentsItemDto =
    ShelfViewManifestContentsItemDto(
      label = label,
      locator =
        mapper
          .createObjectNode()
          .put("schemaVersion", 1)
          .put("kind", "MIXED")
          .put("itemId", itemId)
          .put("itemIndex", index)
          .set<ObjectNode>(
            "itemLocator",
            firstItemLocator(),
          ).put("contentFingerprint", bookContentFingerprint),
    )

  private fun ShelfViewManifestItemDto.firstItemLocator(): ObjectNode {
    val itemContentFingerprint = contentFingerprint

    return if (kind == ShelfViewPublicationKind.TEXT_REFLOW) {
      mapper
        .createObjectNode()
        .put("schemaVersion", 1)
        .put("kind", "TEXT")
        .put("paragraph", 0)
        .put("characterOffset", 0)
        .put("progression", 0.0)
        .put("normalizationVersion", SHELFVIEW_TEXT_NORMALIZATION_VERSION)
        .put("contentFingerprint", itemContentFingerprint)
    } else {
      mapper
        .createObjectNode()
        .put("schemaVersion", 1)
        .put("kind", "PAGE")
        .put("page", 0)
        .put("offset", 0.0)
        .put("contentFingerprint", itemContentFingerprint)
    }
  }

  private companion object {
    val fixedPageProfiles = setOf(MediaProfile.DIVINA, MediaProfile.PDF, MediaProfile.IMAGE_DIRECTORY)

    val fixedPageCapabilities =
      ShelfViewCapabilitiesDto(
        search = false,
        textSelection = false,
        doublePage = true,
        continuousScroll = true,
        rtl = true,
        imageFilters = true,
      )

    val textCapabilities =
      ShelfViewCapabilitiesDto(
        search = true,
        textSelection = true,
        doublePage = false,
        continuousScroll = true,
        rtl = false,
        imageFilters = false,
      )

    val mixedContentCapabilities =
      ShelfViewCapabilitiesDto(
        search = true,
        textSelection = true,
        doublePage = true,
        continuousScroll = true,
        rtl = false,
        imageFilters = true,
      )

    val epubCapabilities =
      ShelfViewCapabilitiesDto(
        search = false,
        textSelection = true,
        doublePage = false,
        continuousScroll = true,
        rtl = true,
        imageFilters = false,
      )
  }
}

internal const val SHELFVIEW_TEXT_NORMALIZATION_VERSION = 1
