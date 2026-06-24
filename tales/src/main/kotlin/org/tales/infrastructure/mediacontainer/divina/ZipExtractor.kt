package org.tales.infrastructure.mediacontainer.divina

import io.github.oshai.kotlinlogging.KotlinLogging
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import org.tales.domain.model.EntryNotFoundException
import org.tales.domain.model.MediaContainerEntry
import org.tales.domain.model.MediaType
import org.tales.infrastructure.image.ImageAnalyzer
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.infrastructure.util.getZipEntryBytes
import org.tales.infrastructure.util.use
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

internal const val ZIP_NESTED_ENTRY_SEPARATOR = "\u001F"

@Service
class ZipExtractor(
  private val contentDetector: ContentDetector,
  private val imageAnalyzer: ImageAnalyzer,
) : DivinaExtractor {
  private val natSortComparator: Comparator<String> = CaseInsensitiveSimpleNaturalComparator.getInstance()

  override fun mediaTypes(): List<String> = listOf(MediaType.ZIP.type)

  override fun getEntries(
    path: Path,
    analyzeDimensions: Boolean,
  ): List<MediaContainerEntry> =
    ZipFile.builder().setPath(path).use { zip ->
      val entries =
        zip.entries
          .toList()
          .filter { !it.isDirectory }

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
                val remainingPageSlots = MAX_EXPANDED_NESTED_PAGES - expandedNestedPages
                if (remainingPageSlots <= 0) {
                  logger.warn { "Skipping nested ZIP entries after reaching the nested-page limit" }
                  emptyList()
                } else {
                  analyzeNestedArchive(zip, entry, analyzeDimensions, remainingPageSlots)
                }
              }

              else -> listOf(analyzeRegularEntry(zip, entry, analyzeDimensions))
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
      getZipEntryBytes(path, entryName)
    }

  private fun analyzeRegularEntry(
    zip: ZipFile,
    entry: ZipArchiveEntry,
    analyzeDimensions: Boolean,
  ): MediaContainerEntry =
    try {
      zip.getInputStream(entry).buffered().use { stream ->
        val mediaType = contentDetector.detectMediaType(stream)
        val dimension =
          if (analyzeDimensions && contentDetector.isImage(mediaType))
            imageAnalyzer.getDimension(stream)
          else
            null
        val fileSize = entry.knownSizeOrNull()
        MediaContainerEntry(name = entry.name, mediaType = mediaType, dimension = dimension, fileSize = fileSize)
      }
    } catch (e: Exception) {
      logger.warn(e) { "Could not analyze entry: ${entry.name}" }
      MediaContainerEntry(name = entry.name, comment = e.message)
    }

  private fun analyzeNestedArchive(
    zip: ZipFile,
    outerEntry: ZipArchiveEntry,
    analyzeDimensions: Boolean,
    remainingPageSlots: Int,
  ): List<MediaContainerEntry> =
    try {
      ensureEntryCanBeRead(zip, outerEntry)
      ensureEntrySizeWithinLimit(outerEntry, MAX_NESTED_ARCHIVE_BYTES)
      val nestedBytes = zip.readEntryBytes(outerEntry, MAX_NESTED_ARCHIVE_BYTES)

      ZipFile.builder().setSeekableByteChannel(SeekableInMemoryByteChannel(nestedBytes)).use { nestedZip ->
        val nestedEntries =
          nestedZip.entries
            .toList()
            .filter { !it.isDirectory }

        val pages = mutableListOf<MediaContainerEntry>()

        nestedEntries.forEachIndexed { index, innerEntry ->
          if (index + 1 > MAX_NESTED_ENTRIES) {
            logger.warn { "Skipping remaining nested ZIP entries after reaching the entry limit" }
            return@forEachIndexed
          }

          if (pages.size >= remainingPageSlots) {
            logger.warn { "Skipping remaining nested ZIP images after reaching the page limit" }
            return@forEachIndexed
          }

          val page = analyzeNestedImageEntry(nestedZip, outerEntry, innerEntry, analyzeDimensions)
          if (page != null) pages += page
        }

        pages
      }
    } catch (e: Exception) {
      logger.warn(e) { "Could not analyze nested ZIP entry" }
      emptyList()
    }

  private fun analyzeNestedImageEntry(
    nestedZip: ZipFile,
    outerEntry: ZipArchiveEntry,
    innerEntry: ZipArchiveEntry,
    analyzeDimensions: Boolean,
  ): MediaContainerEntry? {
    if (innerEntry.name.contains(ZIP_NESTED_ENTRY_SEPARATOR)) return null
    ensureEntryCanBeRead(nestedZip, innerEntry)
    ensureEntrySizeWithinLimit(innerEntry, MAX_NESTED_IMAGE_BYTES)

    val bytes = nestedZip.readEntryBytes(innerEntry, MAX_NESTED_IMAGE_BYTES)
    val mediaType = contentDetector.detectMediaType(bytes.inputStream())
    if (!contentDetector.isImage(mediaType)) return null

    val dimension =
      if (analyzeDimensions)
        imageAnalyzer.getDimension(bytes.inputStream())
      else
        null

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
      throw EntryNotFoundException("Nested entry does not exist: $entryName")
    }

    val outerName = entryName.substring(0, separatorIndex)
    val innerName = entryName.substring(separatorIndex + 1)
    if (!isNestedArchiveEntry(outerName)) {
      throw EntryNotFoundException("Nested entry does not exist: $entryName")
    }

    return ZipFile.builder().setPath(path).use { zip ->
      val outerEntry = zip.getEntry(outerName) ?: throw EntryNotFoundException("Entry does not exist: $outerName")
      ensureEntryCanBeRead(zip, outerEntry)
      ensureEntrySizeWithinLimit(outerEntry, MAX_NESTED_ARCHIVE_BYTES)
      val nestedBytes = zip.readEntryBytes(outerEntry, MAX_NESTED_ARCHIVE_BYTES)

      ZipFile.builder().setSeekableByteChannel(SeekableInMemoryByteChannel(nestedBytes)).use { nestedZip ->
        val innerEntry = nestedZip.getEntry(innerName) ?: throw EntryNotFoundException("Nested entry does not exist: $entryName")
        if (innerEntry.isDirectory) throw EntryNotFoundException("Nested entry does not exist: $entryName")

        ensureEntryCanBeRead(nestedZip, innerEntry)
        ensureEntrySizeWithinLimit(innerEntry, MAX_NESTED_IMAGE_BYTES)
        val bytes = nestedZip.readEntryBytes(innerEntry, MAX_NESTED_IMAGE_BYTES)
        val mediaType = contentDetector.detectMediaType(bytes.inputStream())
        if (!contentDetector.isImage(mediaType)) throw EntryNotFoundException("Nested entry does not exist: $entryName")
        bytes
      }
    }
  }

  private fun isMixedContentEntry(name: String): Boolean = entryExtension(name) in MIXED_CONTENT_EXTENSIONS

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
    val size = entry.knownSizeOrNull() ?: return
    if (size > maxBytes) {
      throw IllegalArgumentException("ZIP entry is too large")
    }

    val compressedSize = entry.compressedSize.takeIf { it != ArchiveEntry.SIZE_UNKNOWN && it > 0 } ?: return
    if (size.toDouble() / compressedSize.toDouble() > MAX_COMPRESSION_RATIO) {
      throw IllegalArgumentException("ZIP entry compression ratio is too high")
    }
  }

  private fun ZipFile.readEntryBytes(
    entry: ZipArchiveEntry,
    maxBytes: Long,
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
          throw IllegalArgumentException("ZIP entry is too large")
        }
        output.write(buffer, 0, read)
      }

      output.toByteArray()
    }

  private fun ZipArchiveEntry.knownSizeOrNull(): Long? = size.takeIf { it != ArchiveEntry.SIZE_UNKNOWN }

  private companion object {
    val MIXED_CONTENT_EXTENSIONS = setOf("txt", "pdf")
    val NESTED_ARCHIVE_EXTENSIONS = setOf("zip", "cbz")
    const val MAX_NESTED_ENTRIES = 10_000
    const val MAX_EXPANDED_NESTED_PAGES = 10_000
    const val MAX_NESTED_ARCHIVE_BYTES = 100L * 1024 * 1024
    const val MAX_NESTED_IMAGE_BYTES = 50L * 1024 * 1024
    const val MAX_COMPRESSION_RATIO = 100.0
  }
}
