package org.tales.shelfview.interfaces.api

import com.github.benmanes.caffeine.cache.Caffeine
import org.apache.commons.io.FilenameUtils
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.model.Media
import org.tales.domain.model.MediaFile
import org.tales.domain.persistence.BookRepository
import org.tales.interfaces.api.rest.dto.BookDto
import org.tales.shelfview.domain.ShelfViewPublicationKind
import org.tales.shelfview.domain.ShelfViewSourceFormat
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import org.tales.domain.model.MediaType as TalesMediaType

internal data class ShelfViewCurrentMixedManifestItem(
  val itemId: String,
  val index: Int,
  val kind: ShelfViewPublicationKind,
  val label: String,
  val contentFingerprint: String,
  val sourceFormat: ShelfViewSourceFormat,
  val pageCount: Int?,
)

@Component
class ShelfViewMixedManifestResolver(
  private val bookRepository: BookRepository,
  private val zipMixedContentProvider: ShelfViewZipMixedContentProvider,
) {
  private val itemCache =
    Caffeine
      .newBuilder()
      .maximumWeight(MAX_CACHED_MANIFEST_ITEMS)
      .weigher<MixedManifestCacheKey, List<ShelfViewCurrentMixedManifestItem>> { _, items -> items.size.coerceAtLeast(1) }
      .expireAfterWrite(30, TimeUnit.MINUTES)
      .build<MixedManifestCacheKey, List<ShelfViewCurrentMixedManifestItem>>()

  internal fun listCurrentItems(
    book: BookDto,
    media: Media,
    bookContentFingerprint: String,
  ): List<ShelfViewCurrentMixedManifestItem> {
    if (!isMixedZipCandidate(book, media)) return emptyList()

    val sourceBook = bookRepository.findByIdOrNull(book.id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    val sourcePath = sourceBook.path.toAbsolutePath().normalize()
    val attributes = Files.readAttributes(sourcePath, BasicFileAttributes::class.java)
    val cacheKey =
      MixedManifestCacheKey(
        path = sourcePath,
        bookContentFingerprint = bookContentFingerprint,
        fileSize = attributes.size(),
        lastModifiedTime = attributes.lastModifiedTime().toString(),
        creationTime = attributes.creationTime().toString(),
        fileKey = attributes.fileKey()?.toString(),
      )

    return itemCache.get(cacheKey) {
      val items =
        zipMixedContentProvider
          .listManifestItems(sourcePath, bookContentFingerprint)
          .mapIndexed { index, item ->
            ShelfViewCurrentMixedManifestItem(
              itemId = item.itemId,
              index = index,
              kind = item.kind,
              label = item.label,
              contentFingerprint = ShelfViewZipMixedItemIds.itemContentFingerprint(bookContentFingerprint, item.itemId),
              sourceFormat = item.sourceFormat,
              pageCount = item.pageCount,
            )
          }

      items.takeIf { manifestItems ->
        manifestItems.any { it.kind == ShelfViewPublicationKind.TEXT_REFLOW || it.sourceFormat == ShelfViewSourceFormat.PDF }
      } ?: emptyList()
    }
  }

  private fun isMixedZipCandidate(
    book: BookDto,
    media: Media,
  ): Boolean =
    media.status == Media.Status.READY &&
      TalesMediaType.fromMediaType(media.mediaType) == TalesMediaType.ZIP &&
      FilenameUtils.getExtension(book.url).equals("zip", ignoreCase = true) &&
      hasMixedZipEntries(media.files)

  private fun hasMixedZipEntries(files: List<MediaFile>): Boolean =
    files.any { file ->
      val extension = FilenameUtils.getExtension(file.fileName).lowercase()
      extension == "txt" || extension == "pdf"
    }

  private data class MixedManifestCacheKey(
    val path: Path,
    val bookContentFingerprint: String,
    val fileSize: Long,
    val lastModifiedTime: String,
    val creationTime: String,
    val fileKey: String?,
  )

  private companion object {
    const val MAX_CACHED_MANIFEST_ITEMS = 50_000L
  }
}
