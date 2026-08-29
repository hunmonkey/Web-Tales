package org.tales.shelfview.interfaces.api

import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.annotation.PreDestroy
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.FilenameUtils
import org.apache.pdfbox.Loader
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.model.Dimension
import org.tales.domain.model.TypedBytes
import org.tales.infrastructure.image.ImageAnalyzer
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.infrastructure.mediacontainer.pdf.PdfExtractor
import org.tales.shelfview.domain.ShelfViewPublicationKind
import org.tales.shelfview.domain.ShelfViewSourceFormat
import org.tales.shelfview.infrastructure.archive.ArchiveLimitExceededException
import org.tales.shelfview.infrastructure.archive.ArchiveReadBudget
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import org.tales.shelfview.infrastructure.archive.copyArchiveTo
import org.tales.shelfview.infrastructure.archive.readArchiveBytes
import org.tales.shelfview.infrastructure.archive.withPreflightedZip
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists

data class ShelfViewZipTextEntryContent(
  val itemId: String,
  val normalizedEntryName: String,
  val bytes: ByteArray,
)

data class ShelfViewZipPageContent(
  val itemId: String,
  val normalizedEntryName: String,
  val pageNumber: Int,
  val bytes: ByteArray,
  val mediaType: String,
)

data class ShelfViewZipMixedManifestItem(
  val itemId: String,
  val normalizedEntryName: String,
  val label: String,
  val kind: ShelfViewPublicationKind,
  val sourceFormat: ShelfViewSourceFormat,
  val pageCount: Int? = null,
)

@Component
class ShelfViewZipMixedContentProvider(
  private val archiveProperties: ShelfViewArchiveProperties,
  private val contentDetector: ContentDetector,
  private val imageAnalyzer: ImageAnalyzer,
  private val pdfExtractor: PdfExtractor,
) {
  private val pdfCacheFiles = ShelfViewPdfCacheFiles()
  private val nestedArchiveCache = ShelfViewNestedArchiveCache()
  private val natSortComparator: Comparator<String> = CaseInsensitiveSimpleNaturalComparator.getInstance()

  private val pdfCache =
    Caffeine
      .newBuilder()
      .maximumSize(64)
      .expireAfterAccess(30, TimeUnit.MINUTES)
      .executor { command -> command.run() }
      .removalListener<String, Path> { _, path, _ ->
        path?.let {
          pdfExtractor.evictCachedDocument(it)
          pdfCacheFiles.delete(it)
        }
      }.build<String, Path>()

  @PreDestroy
  fun closePdfCache() {
    pdfCache.invalidateAll()
    pdfCache.cleanUp()
    pdfCacheFiles.close()
    nestedArchiveCache.close()
  }

  fun listManifestItems(
    path: Path,
    bookContentFingerprint: String,
  ): List<ShelfViewZipMixedManifestItem> =
    withZip(path) { zip ->
      val budget = ArchiveReadBudget(archiveProperties)
      val normalizedNames = mutableSetOf<String>()

      val supportedEntries =
        zip
          .listEntries(budget)
          .map { entry ->
            val normalizedName = normalizeEntryName(entry.name)
            if (!normalizedNames.add(normalizedName)) {
              throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_DUPLICATE_ENTRY")
            }
            entry to normalizedName
          }.filter { (_, normalizedName) ->
            isTextEntry(normalizedName) || isFixedPageEntry(normalizedName)
          }.sortedWith(compareBy(natSortComparator) { (_, normalizedName) -> normalizedName })

      supportedEntries.map { (entry, normalizedName) ->
        manifestItem(zip, entry, bookContentFingerprint, normalizedName, budget)
      }
    }

  fun readTextEntry(
    path: Path,
    bookContentFingerprint: String,
    itemId: String,
    maxBytes: Long,
  ): ShelfViewZipTextEntryContent =
    withZip(path) { zip ->
      val budget = ArchiveReadBudget(archiveProperties)
      val normalizedNames = mutableSetOf<String>()
      var selectedEntry: ZipArchiveEntry? = null
      var selectedName: String? = null

      zip.listEntries(budget).forEach { entry ->
        val normalizedName = normalizeEntryName(entry.name)
        if (!normalizedNames.add(normalizedName)) {
          throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_DUPLICATE_ENTRY")
        }

        if (!isTextEntry(normalizedName)) return@forEach

        val candidateItemId =
          ShelfViewZipMixedItemIds.itemId(
            bookContentFingerprint = bookContentFingerprint,
            normalizedEntryName = normalizedName,
            fileSize = entry.knownSizeOrNull(),
          )
        if (candidateItemId == itemId) {
          selectedEntry = entry
          selectedName = normalizedName
        }
      }

      val entry = selectedEntry ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "SHELFVIEW_ZIP_TEXT_ENTRY_NOT_FOUND")
      val normalizedName = selectedName ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "SHELFVIEW_ZIP_TEXT_ENTRY_NOT_FOUND")
      ensureEntrySizeWithinLimit(entry, maxBytes)
      ensureEntryCanBeRead(zip, entry)
      ensureEntryCompressionRatioWithinLimit(entry)
      val bytes = zip.readEntryBytes(entry, maxBytes, budget = budget)

      ShelfViewZipTextEntryContent(
        itemId = itemId,
        normalizedEntryName = normalizedName,
        bytes = bytes,
      )
    }

  fun readPage(
    path: Path,
    bookContentFingerprint: String,
    itemId: String,
    pageNumber: Int,
  ): ShelfViewZipPageContent {
    if (pageNumber <= 0) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PAGE_NUMBER_INVALID")
    }

    return withZip(path) { zip ->
      val budget = ArchiveReadBudget(archiveProperties)
      val normalizedNames = mutableSetOf<String>()
      var selectedEntry: ZipArchiveEntry? = null
      var selectedName: String? = null

      zip.listEntries(budget).forEach { entry ->
        val normalizedName = normalizeEntryName(entry.name)
        if (!normalizedNames.add(normalizedName)) {
          throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_DUPLICATE_ENTRY")
        }

        if (!isFixedPageEntry(normalizedName)) return@forEach

        val candidateItemId =
          ShelfViewZipMixedItemIds.itemId(
            bookContentFingerprint = bookContentFingerprint,
            normalizedEntryName = normalizedName,
            fileSize = entry.knownSizeOrNull(),
          )
        if (candidateItemId == itemId) {
          selectedEntry = entry
          selectedName = normalizedName
        }
      }

      val entry = selectedEntry ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "SHELFVIEW_ZIP_PAGE_ENTRY_NOT_FOUND")
      val normalizedName = selectedName ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "SHELFVIEW_ZIP_PAGE_ENTRY_NOT_FOUND")

      when {
        isSupportedNestedImageArchive(normalizedName) -> readNestedImagePage(zip, entry, bookContentFingerprint, itemId, normalizedName, pageNumber, budget)
        isPdfEntry(normalizedName) -> readPdfPage(zip, entry, bookContentFingerprint, itemId, normalizedName, pageNumber, budget)
        isImageEntry(normalizedName) -> readImagePage(zip, entry, itemId, normalizedName, pageNumber, budget)
        else -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "SHELFVIEW_ZIP_PAGE_ENTRY_NOT_FOUND")
      }
    }
  }

  private fun manifestItem(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    bookContentFingerprint: String,
    normalizedName: String,
    budget: ArchiveReadBudget,
  ): ShelfViewZipMixedManifestItem {
    val itemId =
      ShelfViewZipMixedItemIds.itemId(
        bookContentFingerprint = bookContentFingerprint,
        normalizedEntryName = normalizedName,
        fileSize = entry.knownSizeOrNull(),
      )

    return when {
      isTextEntry(normalizedName) ->
        ShelfViewZipMixedManifestItem(
          itemId = itemId,
          normalizedEntryName = normalizedName,
          label = displayLabel(normalizedName),
          kind = ShelfViewPublicationKind.TEXT_REFLOW,
          sourceFormat = ShelfViewSourceFormat.TXT,
        )

      isPdfEntry(normalizedName) ->
        ShelfViewZipMixedManifestItem(
          itemId = itemId,
          normalizedEntryName = normalizedName,
          label = displayLabel(normalizedName),
          kind = ShelfViewPublicationKind.FIXED_PAGE_SEQUENCE,
          sourceFormat = ShelfViewSourceFormat.PDF,
          pageCount = validatedPdfPageCount(zip, entry, bookContentFingerprint, itemId, budget),
        )

      isSupportedNestedImageArchive(normalizedName) ->
        ShelfViewZipMixedManifestItem(
          itemId = itemId,
          normalizedEntryName = normalizedName,
          label = displayLabel(normalizedName),
          kind = ShelfViewPublicationKind.FIXED_PAGE_SEQUENCE,
          sourceFormat = sourceFormatForNestedImageArchive(normalizedName),
          pageCount = validatedNestedImagePageCount(zip, entry, bookContentFingerprint, itemId, budget),
        )

      isImageEntry(normalizedName) ->
        ShelfViewZipMixedManifestItem(
          itemId = itemId,
          normalizedEntryName = normalizedName,
          label = displayLabel(normalizedName),
          kind = ShelfViewPublicationKind.FIXED_PAGE_SEQUENCE,
          sourceFormat = sourceFormatForImage(normalizedName),
          pageCount = 1,
        )

      else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_MIXED_UNSUPPORTED_ENTRY")
    }
  }

  private fun normalizeEntryName(name: String): String {
    val normalized = normalizeArchiveEntryPath(name)
    if (isUnsupportedNestedArchive(normalized)) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_NESTED_ARCHIVE")
    }
    return normalized
  }

  private fun normalizeNestedImageEntryName(name: String): String {
    val normalized = normalizeArchiveEntryPath(name)
    if (isNestedArchive(normalized)) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_NESTED_ARCHIVE")
    }
    if (!isImageEntry(normalized)) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_NESTED_IMAGE_CHAPTER_UNSUPPORTED_ENTRY")
    }
    return normalized
  }

  private fun normalizeArchiveEntryPath(name: String): String {
    val normalized = name.replace('\\', '/')
    if (normalized.isBlank()) invalidEntry()
    if (normalized.startsWith('/')) invalidEntry()
    if (WINDOWS_DRIVE_PATH.matches(normalized)) invalidEntry()
    if (normalized.any { it.code < 32 }) invalidEntry()

    val parts = normalized.split('/')
    if (parts.any { it.isBlank() || it == "." || it == ".." }) invalidEntry()

    return parts.joinToString("/")
  }

  private fun displayLabel(normalizedName: String): String = normalizedName.substringAfterLast('/').ifBlank { "item" }

  private fun isTextEntry(normalizedName: String): Boolean = FilenameUtils.getExtension(normalizedName).equals("txt", ignoreCase = true)

  private fun isFixedPageEntry(normalizedName: String): Boolean = isImageEntry(normalizedName) || isPdfEntry(normalizedName) || isSupportedNestedImageArchive(normalizedName)

  private fun isImageEntry(normalizedName: String): Boolean = IMAGE_EXTENSIONS.contains(FilenameUtils.getExtension(normalizedName).lowercase())

  private fun isPdfEntry(normalizedName: String): Boolean = FilenameUtils.getExtension(normalizedName).equals("pdf", ignoreCase = true)

  private fun isNestedArchive(normalizedName: String): Boolean = NESTED_ARCHIVE_EXTENSIONS.contains(FilenameUtils.getExtension(normalizedName).lowercase())

  private fun isSupportedNestedImageArchive(normalizedName: String): Boolean = SUPPORTED_NESTED_IMAGE_ARCHIVE_EXTENSIONS.contains(FilenameUtils.getExtension(normalizedName).lowercase())

  private fun isUnsupportedNestedArchive(normalizedName: String): Boolean = isNestedArchive(normalizedName) && !isSupportedNestedImageArchive(normalizedName)

  private fun ensureEntrySizeWithinLimit(
    entry: ZipArchiveEntry,
    maxBytes: Long,
  ) {
    val size = entry.knownSizeOrNull() ?: return
    if (size > maxBytes) {
      throw ArchiveLimitExceededException("SHELFVIEW_TEXT_TOO_LARGE")
    }
  }

  private fun readImagePage(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    itemId: String,
    normalizedName: String,
    pageNumber: Int,
    budget: ArchiveReadBudget,
  ): ShelfViewZipPageContent {
    if (pageNumber != 1) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PAGE_NUMBER_INVALID")
    }

    ensureEntryCanBeRead(zip, entry)
    ensureEntrySizeWithinLimit(entry, archiveProperties.maxImageBytes, "SHELFVIEW_IMAGE_TOO_LARGE")
    ensureEntryCompressionRatioWithinLimit(entry)
    val bytes = zip.readEntryBytes(entry, archiveProperties.maxImageBytes, "SHELFVIEW_IMAGE_TOO_LARGE", budget)
    val mediaType = contentDetector.detectMediaType(bytes.inputStream())
    if (!allowedImageMediaTypes(normalizedName).contains(mediaType)) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_IMAGE_MIME_MISMATCH")
    }

    val dimension =
      imageAnalyzer.getDimension(bytes.inputStream())
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_IMAGE_UNREADABLE")
    validateImageDimension(dimension)

    return ShelfViewZipPageContent(
      itemId = itemId,
      normalizedEntryName = normalizedName,
      pageNumber = pageNumber,
      bytes = bytes,
      mediaType = mediaType,
    )
  }

  private fun validatedNestedImagePageCount(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    bookContentFingerprint: String,
    itemId: String,
    budget: ArchiveReadBudget,
  ): Int =
    withNestedImageArchive(zip, entry, ShelfViewZipMixedItemIds.itemContentFingerprint(bookContentFingerprint, itemId), budget) { nestedZip ->
      val nestedEntries = listNestedImageEntries(nestedZip, budget)
      nestedEntries.forEach { (innerEntry, normalizedName) ->
        readNestedImageEntry(nestedZip, innerEntry, normalizedName, budget)
      }
      nestedEntries.size
    }

  private fun readNestedImagePage(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    bookContentFingerprint: String,
    itemId: String,
    normalizedName: String,
    pageNumber: Int,
    budget: ArchiveReadBudget,
  ): ShelfViewZipPageContent =
    withNestedImageArchive(zip, entry, ShelfViewZipMixedItemIds.itemContentFingerprint(bookContentFingerprint, itemId), budget) { nestedZip ->
      val nestedEntries = listNestedImageEntries(nestedZip, budget)
      if (pageNumber > nestedEntries.size) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PAGE_NUMBER_INVALID")
      }

      val (innerEntry, innerName) = nestedEntries[pageNumber - 1]
      val page = readNestedImageEntry(nestedZip, innerEntry, innerName, budget)

      ShelfViewZipPageContent(
        itemId = itemId,
        normalizedEntryName = normalizedName,
        pageNumber = pageNumber,
        bytes = page.bytes,
        mediaType = page.mediaType,
      )
    }

  private fun <T> withNestedImageArchive(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    cacheKey: String,
    budget: ArchiveReadBudget,
    block: (ZipFile) -> T,
  ): T {
    ensureEntryCanBeRead(zip, entry)
    ensureEntrySizeWithinLimit(entry, archiveProperties.maxNestedArchiveBytes, "SHELFVIEW_NESTED_ARCHIVE_TOO_LARGE")
    ensureEntryCompressionRatioWithinLimit(entry)
    try {
      val cachedPath =
        nestedArchiveCache.get(cacheKey) { tempPath ->
          zip.copyEntryToFile(entry, tempPath, archiveProperties.maxNestedArchiveBytes, "SHELFVIEW_NESTED_ARCHIVE_TOO_LARGE", budget)
        }
      return withPreflightedZip(cachedPath, archiveProperties) { nestedZip ->
        block(nestedZip)
      }
    } catch (ex: ArchiveLimitExceededException) {
      throw ex
    } catch (ex: ResponseStatusException) {
      throw ex
    } catch (ex: Exception) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_NESTED_ARCHIVE_UNREADABLE", ex)
    }
  }

  private fun listNestedImageEntries(
    nestedZip: ZipFile,
    budget: ArchiveReadBudget,
  ): List<Pair<ZipArchiveEntry, String>> {
    val normalizedNames = mutableSetOf<String>()
    val entries =
      nestedZip
        .listEntries(budget)
        .map { entry ->
          val normalizedName = normalizeNestedImageEntryName(entry.name)
          if (!normalizedNames.add(normalizedName)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_DUPLICATE_ENTRY")
          }
          entry to normalizedName
        }.sortedWith(compareBy(natSortComparator) { (_, normalizedName) -> normalizedName })

    if (entries.isEmpty()) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_NESTED_IMAGE_CHAPTER_EMPTY")
    }

    return entries
  }

  private fun readNestedImageEntry(
    nestedZip: ZipFile,
    entry: ZipArchiveEntry,
    normalizedName: String,
    budget: ArchiveReadBudget,
  ): NestedImagePage {
    ensureEntryCanBeRead(nestedZip, entry)
    ensureEntrySizeWithinLimit(entry, archiveProperties.maxImageBytes, "SHELFVIEW_IMAGE_TOO_LARGE")
    ensureEntryCompressionRatioWithinLimit(entry)

    val bytes = nestedZip.readEntryBytes(entry, archiveProperties.maxImageBytes, "SHELFVIEW_IMAGE_TOO_LARGE", budget)
    val mediaType = contentDetector.detectMediaType(bytes.inputStream())
    if (!allowedImageMediaTypes(normalizedName).contains(mediaType)) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_IMAGE_MIME_MISMATCH")
    }

    val dimension =
      imageAnalyzer.getDimension(bytes.inputStream())
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_IMAGE_UNREADABLE")
    validateImageDimension(dimension)

    return NestedImagePage(bytes = bytes, mediaType = mediaType)
  }

  private fun validatedPdfPageCount(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    bookContentFingerprint: String,
    itemId: String,
    budget: ArchiveReadBudget,
  ): Int {
    val pdfPath = cachedPdfPath(zip, entry, bookContentFingerprint, itemId, budget)
    val pageCount = pdfPageCount(pdfPath)
    if (pageCount > archiveProperties.maxPdfPages) {
      throw ArchiveLimitExceededException("SHELFVIEW_PDF_TOO_MANY_PAGES")
    }
    return pageCount
  }

  private fun readPdfPage(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    bookContentFingerprint: String,
    itemId: String,
    normalizedName: String,
    pageNumber: Int,
    budget: ArchiveReadBudget,
  ): ShelfViewZipPageContent {
    val pdfPath = cachedPdfPath(zip, entry, bookContentFingerprint, itemId, budget)
    val pageCount = pdfPageCount(pdfPath)
    if (pageCount > archiveProperties.maxPdfPages) {
      throw ArchiveLimitExceededException("SHELFVIEW_PDF_TOO_MANY_PAGES")
    }
    if (pageNumber > pageCount) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PAGE_NUMBER_INVALID")
    }

    val pageContent =
      try {
        pdfExtractor.getPageContentAsImage(pdfPath, pageNumber)
      } catch (ex: ArchiveLimitExceededException) {
        throw ex
      } catch (ex: Exception) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PDF_RENDER_FAILED", ex)
      }

    return ShelfViewZipPageContent(
      itemId = itemId,
      normalizedEntryName = normalizedName,
      pageNumber = pageNumber,
      bytes = pageContent.bytes,
      mediaType = pageContent.mediaType,
    )
  }

  private fun cachedPdfPath(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    bookContentFingerprint: String,
    itemId: String,
    budget: ArchiveReadBudget,
  ): Path {
    val cacheKey = ShelfViewZipMixedItemIds.itemContentFingerprint(bookContentFingerprint, itemId)
    pdfCache.getIfPresent(cacheKey)?.let { cachedPath ->
      if (cachedPath.exists()) return cachedPath
      pdfCache.invalidate(cacheKey)
    }

    return requireNotNull(
      pdfCache.get(cacheKey) {
        createCachedPdfPath(zip, entry, budget)
      },
    )
  }

  private fun createCachedPdfPath(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    budget: ArchiveReadBudget,
  ): Path {
    ensureEntrySizeWithinLimit(entry, archiveProperties.maxPdfBytes, "SHELFVIEW_PDF_TOO_LARGE")
    ensureEntryCanBeRead(zip, entry)
    ensureEntryCompressionRatioWithinLimit(entry)

    val tempPath = pdfCacheFiles.createTempFile()
    try {
      zip.copyEntryToFile(entry, tempPath, archiveProperties.maxPdfBytes, "SHELFVIEW_PDF_TOO_LARGE", budget)
      if (contentDetector.detectMediaType(tempPath) != PDF_MEDIA_TYPE) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PDF_MIME_MISMATCH")
      }
      return tempPath
    } catch (ex: ArchiveLimitExceededException) {
      pdfCacheFiles.delete(tempPath)
      throw ex
    } catch (ex: ResponseStatusException) {
      pdfCacheFiles.delete(tempPath)
      throw ex
    } catch (ex: Exception) {
      pdfCacheFiles.delete(tempPath)
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PDF_UNREADABLE", ex)
    }
  }

  private fun pdfPageCount(path: Path): Int =
    try {
      Loader.loadPDF(path.toFile()).use { it.numberOfPages }
    } catch (ex: Exception) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PDF_UNREADABLE", ex)
    }

  private fun validateImageDimension(dimension: Dimension) {
    if (dimension.width > archiveProperties.maxImageWidth || dimension.height > archiveProperties.maxImageHeight) {
      throw ArchiveLimitExceededException("SHELFVIEW_IMAGE_DIMENSIONS_TOO_LARGE")
    }

    if (dimension.width.toLong() * dimension.height.toLong() > archiveProperties.maxImagePixels) {
      throw ArchiveLimitExceededException("SHELFVIEW_IMAGE_PIXELS_TOO_LARGE")
    }
  }

  private fun allowedImageMediaTypes(normalizedName: String): Set<String> =
    when (FilenameUtils.getExtension(normalizedName).lowercase()) {
      "jpg", "jpeg" -> setOf("image/jpeg")
      "png" -> setOf("image/png")
      "webp" -> setOf("image/webp")
      "gif" -> setOf("image/gif")
      else -> emptySet()
    }

  private fun sourceFormatForImage(normalizedName: String): ShelfViewSourceFormat =
    when (FilenameUtils.getExtension(normalizedName).lowercase()) {
      "jpg", "jpeg" -> ShelfViewSourceFormat.JPG
      "png" -> ShelfViewSourceFormat.PNG
      "webp" -> ShelfViewSourceFormat.WEBP
      "gif" -> ShelfViewSourceFormat.GIF
      else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_IMAGE_MIME_MISMATCH")
    }

  private fun sourceFormatForNestedImageArchive(normalizedName: String): ShelfViewSourceFormat =
    when (FilenameUtils.getExtension(normalizedName).lowercase()) {
      "zip" -> ShelfViewSourceFormat.ZIP
      "cbz" -> ShelfViewSourceFormat.CBZ
      else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_NESTED_ARCHIVE")
    }

  private fun ZipFile.readEntryBytes(
    entry: ZipArchiveEntry,
    maxBytes: Long,
    tooLargeReason: String = "SHELFVIEW_TEXT_TOO_LARGE",
    budget: ArchiveReadBudget,
  ): ByteArray =
    getInputStream(entry).use { input ->
      input.readArchiveBytes(maxBytes, tooLargeReason, entry.knownSizeOrNull(), budget)
    }

  private fun ZipFile.copyEntryToFile(
    entry: ZipArchiveEntry,
    destination: Path,
    maxBytes: Long,
    tooLargeReason: String,
    budget: ArchiveReadBudget,
  ) {
    getInputStream(entry).use { input ->
      Files.newOutputStream(destination).use { output ->
        input.copyArchiveTo(output, maxBytes, tooLargeReason, entry.knownSizeOrNull(), budget)
      }
    }
  }

  private fun ZipFile.listEntries(budget: ArchiveReadBudget): List<ZipArchiveEntry> {
    val result = mutableListOf<ZipArchiveEntry>()
    val enumeration = entries
    while (enumeration.hasMoreElements()) {
      val entry = enumeration.nextElement()
      budget.recordEntry()
      if (!entry.isDirectory) result += entry
    }
    return result
  }

  private fun <T> withZip(
    path: Path,
    block: (ZipFile) -> T,
  ): T = withPreflightedZip(path, archiveProperties, block = block)

  private fun ensureEntrySizeWithinLimit(
    entry: ZipArchiveEntry,
    maxBytes: Long,
    tooLargeReason: String,
  ) {
    val size = entry.knownSizeOrNull() ?: return
    if (size > maxBytes) {
      throw ArchiveLimitExceededException(tooLargeReason)
    }
  }

  private fun ensureEntryCanBeRead(
    zip: ZipFile,
    entry: ZipArchiveEntry,
  ) {
    if (!zip.canReadEntryData(entry) || entry.generalPurposeBit.usesEncryption()) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_UNREADABLE_ENTRY")
    }
  }

  private fun ensureEntryCompressionRatioWithinLimit(entry: ZipArchiveEntry) {
    val size = entry.knownSizeOrNull() ?: return
    val compressedSize = entry.compressedSize.takeIf { it != ArchiveEntry.SIZE_UNKNOWN && it >= 0 } ?: return
    if (
      (compressedSize == 0L && size > 0) ||
      (compressedSize > 0 && size.toDouble() / compressedSize.toDouble() > archiveProperties.maxCompressionRatio)
    ) {
      throw ArchiveLimitExceededException("SHELFVIEW_ARCHIVE_COMPRESSION_RATIO_TOO_HIGH")
    }
  }

  private fun ZipArchiveEntry.knownSizeOrNull(): Long? = size.takeIf { it != ArchiveEntry.SIZE_UNKNOWN }

  private fun invalidEntry(): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_INVALID_ENTRY_PATH")

  private data class NestedImagePage(
    val bytes: ByteArray,
    val mediaType: String,
  )

  private companion object {
    val WINDOWS_DRIVE_PATH = Regex("^[A-Za-z]:[/\\\\].*")
    val NESTED_ARCHIVE_EXTENSIONS = setOf("zip", "cbz", "cbr", "rar", "7z")
    val SUPPORTED_NESTED_IMAGE_ARCHIVE_EXTENSIONS = setOf("zip", "cbz")
    val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")
    const val PDF_MEDIA_TYPE = "application/pdf"
  }
}

internal object ShelfViewZipMixedItemIds {
  fun itemId(
    bookContentFingerprint: String,
    normalizedEntryName: String,
    fileSize: Long?,
  ): String {
    val digest =
      MessageDigest
        .getInstance("SHA-256")
        .digest("$bookContentFingerprint\n$normalizedEntryName\n${fileSize ?: -1}".toByteArray(Charsets.UTF_8))

    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  fun itemContentFingerprint(
    bookContentFingerprint: String,
    itemId: String,
  ): String = "$bookContentFingerprint:zip-item:$itemId"
}
