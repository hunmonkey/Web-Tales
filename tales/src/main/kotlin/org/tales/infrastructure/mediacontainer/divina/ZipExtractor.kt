package org.tales.infrastructure.mediacontainer.divina

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.springframework.stereotype.Service
import org.tales.domain.model.EntryNotFoundException
import org.tales.domain.model.MediaContainerEntry
import org.tales.domain.model.MediaType
import org.tales.infrastructure.image.ImageAnalyzer
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.shelfview.infrastructure.archive.ArchiveLimitExceededException
import org.tales.shelfview.infrastructure.archive.ArchiveReadBudget
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import org.tales.shelfview.infrastructure.archive.copyArchiveTo
import org.tales.shelfview.infrastructure.archive.ensureArchiveCompressionRatio
import org.tales.shelfview.infrastructure.archive.ensureArchiveEntrySize
import org.tales.shelfview.infrastructure.archive.ensureArchiveImageDimension
import org.tales.shelfview.infrastructure.archive.readArchiveBytes
import org.tales.shelfview.infrastructure.archive.withArchiveBudget
import org.tales.shelfview.infrastructure.archive.withArchiveByteLimit
import org.tales.shelfview.infrastructure.archive.withPreflightedZip
import org.tales.shelfview.interfaces.api.ShelfViewNestedArchiveCache
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists

private val logger = KotlinLogging.logger {}

internal const val ZIP_NESTED_ENTRY_SEPARATOR = "\u001F"

/** Entries that make a ZIP a mixed-content book, read by ShelfView rather than as pages (ADR-0003). */
internal val ZIP_MIXED_CONTENT_EXTENSIONS = setOf("txt", "pdf")

@Service
class ZipExtractor(
  private val contentDetector: ContentDetector,
  private val imageAnalyzer: ImageAnalyzer,
  private val archiveProperties: ShelfViewArchiveProperties,
) : DivinaExtractor {
  private val natSortComparator: Comparator<String> = CaseInsensitiveSimpleNaturalComparator.getInstance()
  private val nestedArchiveCache = ShelfViewNestedArchiveCache()

  @PreDestroy
  fun closeNestedArchiveCache() {
    nestedArchiveCache.close()
  }

  override fun mediaTypes(): List<String> = listOf(MediaType.ZIP.type)

  override fun getEntries(
    path: Path,
    analyzeDimensions: Boolean,
  ): List<MediaContainerEntry> =
    withZip(path) { zip ->
      val budget = ArchiveReadBudget(archiveProperties)
      val entries = zip.listEntries(budget)

      val expandNestedArchives = entries.none { isMixedContentEntry(it.name) }
      val mediaEntries = mutableListOf<MediaContainerEntry>()
      var expandedNestedPages = 0

      entries
        .forEach { entry ->
          val entryResults =
            when {
              entry.name.contains(ZIP_NESTED_ENTRY_SEPARATOR) -> {
                logger.warn { "Skipping ZIP entry with reserved nested-entry separator" }
                emptyList()
              }

              expandNestedArchives && isNestedArchiveEntry(entry.name) -> {
                val remainingPageSlots = archiveProperties.maxEntries - expandedNestedPages
                if (remainingPageSlots <= 0) {
                  logger.warn { "Skipping nested ZIP entries after reaching the nested-page limit" }
                  emptyList()
                } else {
                  analyzeNestedArchive(zip, entry, analyzeDimensions, remainingPageSlots, budget)
                }
              }

              else -> listOf(analyzeRegularEntry(zip, entry, analyzeDimensions, budget))
            }

          expandedNestedPages += entryResults.count { it.name.contains(ZIP_NESTED_ENTRY_SEPARATOR) }
          mediaEntries += entryResults
        }

      mediaEntries.sortedWith(compareBy(natSortComparator) { it.name })
    }

  override fun getEntryStream(
    path: Path,
    entryName: String,
  ): ByteArray =
    if (entryName.contains(ZIP_NESTED_ENTRY_SEPARATOR)) {
      getNestedEntryBytes(path, entryName)
    } else {
      getRegularEntryBytes(path, entryName)
    }

  private fun analyzeRegularEntry(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    analyzeDimensions: Boolean,
    budget: ArchiveReadBudget,
  ): MediaContainerEntry =
    try {
      ensureEntryCanBeRead(zip, entry)
      // Only the header is read here. TXT, PDF and nested archive entries are
      // not pages: the mixed-content path holds each to its own limit when it
      // serves it, so the image ceiling must not fail the whole book.
      if (!isMixedContentEntry(entry.name) && !isNestedArchiveEntry(entry.name)) ensureEntryWithinImageLimits(entry)
      zip
        .getInputStream(entry)
        .withArchiveByteLimit(archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
        .withArchiveBudget(budget)
        .buffered()
        .use { stream ->
          val mediaType = contentDetector.detectMediaType(stream)
          val dimension =
            if (analyzeDimensions && contentDetector.isImage(mediaType))
              imageAnalyzer.getDimension(stream)
            else
              null
          ensureArchiveImageDimension(dimension, archiveProperties)
          val fileSize = entry.knownSizeOrNull()
          MediaContainerEntry(name = entry.name, mediaType = mediaType, dimension = dimension, fileSize = fileSize)
        }
    } catch (e: ArchiveLimitExceededException) {
      throw e
    } catch (e: Exception) {
      logger.warn { "Could not analyze a ZIP entry: errorType=${e.javaClass.simpleName}" }
      MediaContainerEntry(name = entry.name, comment = e.message)
    }

  private fun analyzeNestedArchive(
    zip: ZipFile,
    outerEntry: ZipArchiveEntry,
    analyzeDimensions: Boolean,
    remainingPageSlots: Int,
    budget: ArchiveReadBudget,
  ): List<MediaContainerEntry> =
    try {
      ensureEntryCanBeRead(zip, outerEntry)
      ensureEntrySizeWithinLimit(outerEntry, archiveProperties.maxNestedArchiveBytes)
      withNestedZip(zip, outerEntry, budget) { nestedZip ->
        val nestedEntries = nestedZip.listEntries(budget)

        val pages = mutableListOf<MediaContainerEntry>()

        nestedEntries.forEach { innerEntry ->
          if (pages.size >= remainingPageSlots) {
            logger.warn { "Skipping remaining nested ZIP images after reaching the page limit" }
            return@forEach
          }

          val page = analyzeNestedImageEntry(nestedZip, outerEntry, innerEntry, analyzeDimensions, budget)
          if (page != null) pages += page
        }

        pages
      }
    } catch (e: ArchiveLimitExceededException) {
      throw e
    } catch (e: Exception) {
      logger.warn { "Could not analyze nested ZIP entry: errorType=${e.javaClass.simpleName}" }
      emptyList()
    }

  private fun analyzeNestedImageEntry(
    nestedZip: ZipFile,
    outerEntry: ZipArchiveEntry,
    innerEntry: ZipArchiveEntry,
    analyzeDimensions: Boolean,
    budget: ArchiveReadBudget,
  ): MediaContainerEntry? {
    if (innerEntry.name.contains(ZIP_NESTED_ENTRY_SEPARATOR)) return null
    ensureEntryCanBeRead(nestedZip, innerEntry)
    ensureEntryWithinImageLimits(innerEntry)

    val bytes = nestedZip.readEntryBytes(innerEntry, archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE", budget)
    val mediaType = contentDetector.detectMediaType(bytes.inputStream())
    if (!contentDetector.isImage(mediaType)) return null

    val dimension =
      if (analyzeDimensions)
        imageAnalyzer.getDimension(bytes.inputStream())
      else
        null
    ensureArchiveImageDimension(dimension, archiveProperties)

    return MediaContainerEntry(
      name = compositeEntryName(outerEntry.name, innerEntry.name),
      mediaType = mediaType,
      dimension = dimension,
      fileSize = innerEntry.knownSizeOrNull() ?: bytes.size.toLong(),
    )
  }

  private fun getNestedEntryBytes(
    path: Path,
    entryName: String,
  ): ByteArray {
    val separatorIndex = entryName.indexOf(ZIP_NESTED_ENTRY_SEPARATOR)
    if (
      separatorIndex <= 0 ||
      separatorIndex == entryName.lastIndex ||
      entryName.indexOf(ZIP_NESTED_ENTRY_SEPARATOR, separatorIndex + 1) != -1
    ) {
      throw EntryNotFoundException("Nested archive entry does not exist")
    }

    val outerName = entryName.substring(0, separatorIndex)
    val innerName = entryName.substring(separatorIndex + 1)
    if (!isNestedArchiveEntry(outerName)) {
      throw EntryNotFoundException("Nested archive entry does not exist")
    }

    return withZip(path) { zip ->
      val budget = ArchiveReadBudget(archiveProperties)
      val outerEntry = zip.listEntries(budget).firstOrNull { it.name == outerName } ?: throw EntryNotFoundException("Archive entry does not exist")
      ensureEntryCanBeRead(zip, outerEntry)
      ensureEntrySizeWithinLimit(outerEntry, archiveProperties.maxNestedArchiveBytes)
      withCachedNestedZip(path, zip, outerEntry, budget) { nestedZip ->
        val innerEntry = nestedZip.listEntries(budget).firstOrNull { it.name == innerName } ?: throw EntryNotFoundException("Nested archive entry does not exist")

        ensureEntryCanBeRead(nestedZip, innerEntry)
        ensureEntryWithinImageLimits(innerEntry)
        val bytes = nestedZip.readEntryBytes(innerEntry, archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE", budget)
        val mediaType = contentDetector.detectMediaType(bytes.inputStream())
        if (!contentDetector.isImage(mediaType)) throw EntryNotFoundException("Nested archive entry does not exist")
        ensureArchiveImageDimension(imageAnalyzer.getDimension(bytes.inputStream()), archiveProperties)
        bytes
      }
    }
  }

  /**
   * Serves pages of a nested archive from one cached extraction instead of
   * decompressing the whole inner archive again for every page. Falls back to
   * the one-shot copy when the outer file has no readable identity to key on.
   * Analysis keeps using [withNestedZip] directly: it runs once per scan.
   */
  private fun <T> withCachedNestedZip(
    path: Path,
    zip: ZipFile,
    entry: ZipArchiveEntry,
    budget: ArchiveReadBudget,
    block: (ZipFile) -> T,
  ): T {
    val key = nestedArchiveCacheKey(path, entry.name) ?: return withNestedZip(zip, entry, budget, block)

    val cachedPath =
      nestedArchiveCache.get(key) { tempPath ->
        zip.getInputStream(entry).use { input ->
          Files.newOutputStream(tempPath).use { output ->
            input.copyArchiveTo(
              output,
              archiveProperties.maxNestedArchiveBytes,
              "SHELFVIEW_NESTED_ARCHIVE_TOO_LARGE",
              entry.knownSizeOrNull(),
              budget,
            )
          }
        }
      }
    return withPreflightedZip(cachedPath, archiveProperties, block = block)
  }

  private fun nestedArchiveCacheKey(
    path: Path,
    entryName: String,
  ): String? =
    try {
      "${path.toAbsolutePath()}|${Files.getLastModifiedTime(path).toMillis()}|${Files.size(path)}|$entryName"
    } catch (_: Exception) {
      null
    }

  private fun <T> withNestedZip(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    budget: ArchiveReadBudget,
    block: (ZipFile) -> T,
  ): T {
    val tempPath = Files.createTempFile("tales-nested-", ".zip")
    try {
      zip.getInputStream(entry).use { input ->
        Files.newOutputStream(tempPath).use { output ->
          input.copyArchiveTo(
            output,
            archiveProperties.maxNestedArchiveBytes,
            "SHELFVIEW_NESTED_ARCHIVE_TOO_LARGE",
            entry.knownSizeOrNull(),
            budget,
          )
        }
      }
      return withPreflightedZip(tempPath, archiveProperties, block = block)
    } finally {
      tempPath.deleteIfExists()
    }
  }

  private fun getRegularEntryBytes(
    path: Path,
    entryName: String,
  ): ByteArray =
    readRegularEntryBytes(path, entryName, ignoreLocalFileHeader = true)
      ?: readRegularEntryBytes(path, entryName, ignoreLocalFileHeader = false)
      ?: throw EntryNotFoundException("Archive entry does not exist")

  private fun readRegularEntryBytes(
    path: Path,
    entryName: String,
    ignoreLocalFileHeader: Boolean,
  ): ByteArray? {
    return withPreflightedZip(
      path,
      archiveProperties,
      configure = {
        setUseUnicodeExtraFields(true)
        setIgnoreLocalFileHeader(ignoreLocalFileHeader)
      },
    ) { zip ->
      val budget = ArchiveReadBudget(archiveProperties)
      val entry = zip.listEntries(budget).firstOrNull { it.name == entryName } ?: return@withPreflightedZip null
      ensureEntryCanBeRead(zip, entry)
      ensureEntryWithinImageLimits(entry)
      val bytes = zip.readEntryBytes(entry, archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE", budget)
      validateImageBytes(bytes)
      bytes
    }
  }

  private fun <T> withZip(
    path: Path,
    block: (ZipFile) -> T,
  ): T = withPreflightedZip(path, archiveProperties, block = block)

  private fun isMixedContentEntry(name: String): Boolean = entryExtension(name) in ZIP_MIXED_CONTENT_EXTENSIONS

  private fun isNestedArchiveEntry(name: String): Boolean = entryExtension(name) in NESTED_ARCHIVE_EXTENSIONS

  private fun entryExtension(name: String): String =
    name
      .substringAfterLast('/')
      .substringAfterLast('.', missingDelimiterValue = "")
      .lowercase()

  private fun compositeEntryName(
    outerName: String,
    innerName: String,
  ): String = "$outerName$ZIP_NESTED_ENTRY_SEPARATOR$innerName"

  private fun ensureEntryCanBeRead(
    zip: ZipFile,
    entry: ZipArchiveEntry,
  ) {
    if (!zip.canReadEntryData(entry) || entry.generalPurposeBit.usesEncryption()) {
      throw IllegalArgumentException("Unreadable ZIP entry")
    }
  }

  private fun ensureEntrySizeWithinLimit(
    entry: ZipArchiveEntry,
    maxBytes: Long,
  ) {
    ensureArchiveEntrySize(entry.knownSizeOrNull(), maxBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
    ensureArchiveCompressionRatio(entry.knownSizeOrNull(), entry.knownCompressedSizeOrNull(), archiveProperties.maxCompressionRatio)
  }

  private fun ZipFile.readEntryBytes(
    entry: ZipArchiveEntry,
    maxBytes: Long,
    reason: String,
    budget: ArchiveReadBudget,
  ): ByteArray = getInputStream(entry).use { it.readArchiveBytes(maxBytes, reason, entry.knownSizeOrNull(), budget) }

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

  private fun ensureEntryWithinImageLimits(entry: ZipArchiveEntry) {
    ensureArchiveEntrySize(entry.knownSizeOrNull(), archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
    ensureArchiveCompressionRatio(entry.knownSizeOrNull(), entry.knownCompressedSizeOrNull(), archiveProperties.maxCompressionRatio)
  }

  private fun validateImageBytes(bytes: ByteArray) {
    val mediaType = contentDetector.detectMediaType(bytes.inputStream())
    if (contentDetector.isImage(mediaType)) {
      ensureArchiveImageDimension(imageAnalyzer.getDimension(bytes.inputStream()), archiveProperties)
    }
  }

  private fun ZipArchiveEntry.knownSizeOrNull(): Long? = size.takeIf { it != ArchiveEntry.SIZE_UNKNOWN }

  private fun ZipArchiveEntry.knownCompressedSizeOrNull(): Long? = compressedSize.takeIf { it != ArchiveEntry.SIZE_UNKNOWN }

  private companion object {
    val NESTED_ARCHIVE_EXTENSIONS = setOf("zip", "cbz")
  }
}
