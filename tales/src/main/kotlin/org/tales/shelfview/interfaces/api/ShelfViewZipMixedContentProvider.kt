package org.tales.shelfview.interfaces.api

import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.validation.constraints.Positive
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.FilenameUtils
import org.apache.pdfbox.Loader
import org.tales.domain.model.Dimension
import org.tales.domain.model.TypedBytes
import org.tales.infrastructure.image.ImageAnalyzer
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.infrastructure.mediacontainer.pdf.PdfExtractor
import org.tales.infrastructure.util.use
import org.tales.shelfview.domain.ShelfViewPublicationKind
import org.tales.shelfview.domain.ShelfViewSourceFormat
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists

@Component
@ConfigurationProperties(prefix = "shelfview.archive")
@Validated
class ShelfViewArchiveProperties {
  @Positive
  var maxEntries: Int = 10_000

  @Positive
  var maxImageBytes: Long = 50L * 1024 * 1024

  @Positive
  var maxImageWidth: Int = 20_000

  @Positive
  var maxImageHeight: Int = 20_000

  @Positive
  var maxImagePixels: Long = 100_000_000

  @Positive
  var maxPdfBytes: Long = 100L * 1024 * 1024

  @Positive
  var maxPdfPages: Int = 1_000
}

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
  private val pdfCacheDirectory: Path = Path.of(System.getProperty("java.io.tmpdir")) / "shelfview-mixed-pdf-cache"
  private val natSortComparator: Comparator<String> = CaseInsensitiveSimpleNaturalComparator.getInstance()

  private val pdfCache =
    Caffeine
      .newBuilder()
      .maximumSize(64)
      .expireAfterAccess(30, TimeUnit.MINUTES)
      .removalListener<String, Path> { _, path, _ -> path?.deleteIfExists() }
      .build<String, Path>()

  fun listManifestItems(
    path: Path,
    bookContentFingerprint: String,
  ): List<ShelfViewZipMixedManifestItem> =
    ZipFile.builder().setPath(path).use { zip ->
      val normalizedNames = mutableSetOf<String>()

      zip.entries
        .toList()
        .filter { !it.isDirectory }
        .onEachIndexed { index, _ ->
          if (index + 1 > archiveProperties.maxEntries) {
            throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES")
          }
        }.map { entry ->
          val normalizedName = normalizeEntryName(entry.name)
          if (!normalizedNames.add(normalizedName)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_DUPLICATE_ENTRY")
          }
          entry to normalizedName
        }.filter { (_, normalizedName) ->
          isTextEntry(normalizedName) || isFixedPageEntry(normalizedName)
        }.sortedWith(compareBy(natSortComparator) { (_, normalizedName) -> normalizedName })
        .map { (entry, normalizedName) ->
          manifestItem(zip, entry, bookContentFingerprint, normalizedName)
        }
    }

  fun readTextEntry(
    path: Path,
    bookContentFingerprint: String,
    itemId: String,
    maxBytes: Long,
  ): ShelfViewZipTextEntryContent =
    ZipFile.builder().setPath(path).use { zip ->
      var entriesCount = 0
      val normalizedNames = mutableSetOf<String>()
      var selectedEntry: ZipArchiveEntry? = null
      var selectedName: String? = null

      zip.entries.toList().forEach { entry ->
        if (entry.isDirectory) return@forEach

        entriesCount += 1
        if (entriesCount > archiveProperties.maxEntries) {
          throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES")
        }

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

      ShelfViewZipTextEntryContent(
        itemId = itemId,
        normalizedEntryName = normalizedName,
        bytes = zip.readEntryBytes(entry, maxBytes),
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

    return ZipFile.builder().setPath(path).use { zip ->
      var entriesCount = 0
      val normalizedNames = mutableSetOf<String>()
      var selectedEntry: ZipArchiveEntry? = null
      var selectedName: String? = null

      zip.entries.toList().forEach { entry ->
        if (entry.isDirectory) return@forEach

        entriesCount += 1
        if (entriesCount > archiveProperties.maxEntries) {
          throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES")
        }

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
        isPdfEntry(normalizedName) -> readPdfPage(zip, entry, bookContentFingerprint, itemId, normalizedName, pageNumber)
        isImageEntry(normalizedName) -> readImagePage(zip, entry, itemId, normalizedName, pageNumber)
        else -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "SHELFVIEW_ZIP_PAGE_ENTRY_NOT_FOUND")
      }
    }
  }

  private fun manifestItem(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    bookContentFingerprint: String,
    normalizedName: String,
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
          pageCount = validatedPdfPageCount(zip, entry, bookContentFingerprint, itemId),
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
    val normalized = name.replace('\\', '/')
    if (normalized.isBlank()) invalidEntry()
    if (normalized.startsWith('/')) invalidEntry()
    if (WINDOWS_DRIVE_PATH.matches(normalized)) invalidEntry()
    if (normalized.any { it.code < 32 }) invalidEntry()

    val parts = normalized.split('/')
    if (parts.any { it.isBlank() || it == "." || it == ".." }) invalidEntry()

    if (isNestedArchive(normalized)) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_NESTED_ARCHIVE")
    }

    return parts.joinToString("/")
  }

  private fun displayLabel(normalizedName: String): String = normalizedName.substringAfterLast('/').ifBlank { "item" }

  private fun isTextEntry(normalizedName: String): Boolean = FilenameUtils.getExtension(normalizedName).equals("txt", ignoreCase = true)

  private fun isFixedPageEntry(normalizedName: String): Boolean = isImageEntry(normalizedName) || isPdfEntry(normalizedName)

  private fun isImageEntry(normalizedName: String): Boolean = IMAGE_EXTENSIONS.contains(FilenameUtils.getExtension(normalizedName).lowercase())

  private fun isPdfEntry(normalizedName: String): Boolean = FilenameUtils.getExtension(normalizedName).equals("pdf", ignoreCase = true)

  private fun isNestedArchive(normalizedName: String): Boolean = NESTED_ARCHIVE_EXTENSIONS.contains(FilenameUtils.getExtension(normalizedName).lowercase())

  private fun ensureEntrySizeWithinLimit(
    entry: ZipArchiveEntry,
    maxBytes: Long,
  ) {
    val size = entry.knownSizeOrNull() ?: return
    if (size > maxBytes) {
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_TEXT_TOO_LARGE")
    }
  }

  private fun readImagePage(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    itemId: String,
    normalizedName: String,
    pageNumber: Int,
  ): ShelfViewZipPageContent {
    if (pageNumber != 1) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PAGE_NUMBER_INVALID")
    }

    val bytes = zip.readEntryBytes(entry, archiveProperties.maxImageBytes, "SHELFVIEW_IMAGE_TOO_LARGE")
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

  private fun validatedPdfPageCount(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    bookContentFingerprint: String,
    itemId: String,
  ): Int {
    val pdfPath = cachedPdfPath(zip, entry, bookContentFingerprint, itemId)
    val pageCount = pdfPageCount(pdfPath)
    if (pageCount > archiveProperties.maxPdfPages) {
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_PDF_TOO_MANY_PAGES")
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
  ): ShelfViewZipPageContent {
    val pdfPath = cachedPdfPath(zip, entry, bookContentFingerprint, itemId)
    val pageCount = pdfPageCount(pdfPath)
    if (pageCount > archiveProperties.maxPdfPages) {
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_PDF_TOO_MANY_PAGES")
    }
    if (pageNumber > pageCount) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PAGE_NUMBER_INVALID")
    }

    val pageContent =
      try {
        pdfExtractor.getPageContentAsImage(pdfPath, pageNumber)
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
  ): Path {
    val cacheKey = ShelfViewZipMixedItemIds.itemContentFingerprint(bookContentFingerprint, itemId)
    pdfCache.getIfPresent(cacheKey)?.takeIf { it.exists() }?.let { return it }

    ensureEntrySizeWithinLimit(entry, archiveProperties.maxPdfBytes, "SHELFVIEW_PDF_TOO_LARGE")
    pdfCacheDirectory.createDirectories()

    val tempPath = Files.createTempFile(pdfCacheDirectory, "mixed-pdf-", ".pdf")
    try {
      zip.copyEntryToFile(entry, tempPath, archiveProperties.maxPdfBytes, "SHELFVIEW_PDF_TOO_LARGE")
      if (contentDetector.detectMediaType(tempPath) != PDF_MEDIA_TYPE) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PDF_MIME_MISMATCH")
      }
      pdfCache.put(cacheKey, tempPath)
      return tempPath
    } catch (ex: ResponseStatusException) {
      tempPath.deleteIfExists()
      throw ex
    } catch (ex: Exception) {
      tempPath.deleteIfExists()
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
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_IMAGE_DIMENSIONS_TOO_LARGE")
    }

    if (dimension.width.toLong() * dimension.height.toLong() > archiveProperties.maxImagePixels) {
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_IMAGE_PIXELS_TOO_LARGE")
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

  private fun ZipFile.readEntryBytes(
    entry: ZipArchiveEntry,
    maxBytes: Long,
    tooLargeReason: String = "SHELFVIEW_TEXT_TOO_LARGE",
  ): ByteArray =
    getInputStream(entry).use { input ->
      val output = ByteArrayOutputStream()
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      var totalBytes = 0L

      while (true) {
        val read = input.read(buffer)
        if (read == -1) break

        totalBytes += read.toLong()
        if (totalBytes > maxBytes) {
          throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, tooLargeReason)
        }
        output.write(buffer, 0, read)
      }

      output.toByteArray()
    }

  private fun ZipFile.copyEntryToFile(
    entry: ZipArchiveEntry,
    destination: Path,
    maxBytes: Long,
    tooLargeReason: String,
  ) {
    getInputStream(entry).use { input ->
      Files.newOutputStream(destination).use { output ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0L

        while (true) {
          val read = input.read(buffer)
          if (read == -1) break

          totalBytes += read.toLong()
          if (totalBytes > maxBytes) {
            throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, tooLargeReason)
          }
          output.write(buffer, 0, read)
        }
      }
    }
  }

  private fun ensureEntrySizeWithinLimit(
    entry: ZipArchiveEntry,
    maxBytes: Long,
    tooLargeReason: String,
  ) {
    val size = entry.knownSizeOrNull() ?: return
    if (size > maxBytes) {
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, tooLargeReason)
    }
  }

  private fun ZipArchiveEntry.knownSizeOrNull(): Long? = size.takeIf { it != ArchiveEntry.SIZE_UNKNOWN }

  private fun invalidEntry(): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_ARCHIVE_INVALID_ENTRY_PATH")

  private companion object {
    val WINDOWS_DRIVE_PATH = Regex("^[A-Za-z]:[/\\\\].*")
    val NESTED_ARCHIVE_EXTENSIONS = setOf("zip", "cbz", "cbr", "rar", "7z")
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
