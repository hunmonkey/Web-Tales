package org.tales.infrastructure.mediacontainer.divina

import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import io.github.oshai.kotlinlogging.KotlinLogging
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import org.springframework.stereotype.Service
import org.tales.domain.model.EntryNotFoundException
import org.tales.domain.model.MediaContainerEntry
import org.tales.domain.model.MediaType
import org.tales.domain.model.MediaUnsupportedException
import org.tales.infrastructure.image.ImageAnalyzer
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.shelfview.infrastructure.archive.ArchiveLimitExceededException
import org.tales.shelfview.infrastructure.archive.ArchiveReadBudget
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import org.tales.shelfview.infrastructure.archive.ensureArchiveCompressionRatio
import org.tales.shelfview.infrastructure.archive.ensureArchiveEntrySize
import org.tales.shelfview.infrastructure.archive.ensureArchiveImageDimension
import org.tales.shelfview.infrastructure.archive.extractArchiveBytes
import org.tales.shelfview.infrastructure.archive.withMetadataLimitedRar4
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

@Service
class RarExtractor(
  private val contentDetector: ContentDetector,
  private val imageAnalyzer: ImageAnalyzer,
  private val archiveProperties: ShelfViewArchiveProperties,
) : DivinaExtractor {
  private val natSortComparator: Comparator<String> = CaseInsensitiveSimpleNaturalComparator.getInstance()

  override fun mediaTypes(): List<String> = listOf(MediaType.RAR_GENERIC.type, MediaType.RAR_4.type)

  override fun getEntries(
    path: Path,
    analyzeDimensions: Boolean,
  ): List<MediaContainerEntry> =
    withRar(path) { rar ->
      validateArchive(rar)
      val budget = ArchiveReadBudget(archiveProperties)
      val entries = collectHeaders(rar, budget)

      entries
        .map { entry ->
          try {
            ensureEntryWithinLimits(entry.fullUnpackSize, entry.fullPackSize)
            val buffer = extractEntry(rar, entry, budget)
            val mediaType = buffer.inputStream().use { contentDetector.detectMediaType(it) }
            val dimension =
              if (analyzeDimensions && contentDetector.isImage(mediaType))
                buffer.inputStream().use { imageAnalyzer.getDimension(it) }
              else
                null
            ensureArchiveImageDimension(dimension, archiveProperties)
            val fileSize = entry.fullUnpackSize
            MediaContainerEntry(name = entry.fileName, mediaType = mediaType, dimension = dimension, fileSize = fileSize)
          } catch (e: ArchiveLimitExceededException) {
            throw e
          } catch (e: Exception) {
            logger.warn { "Could not analyze a RAR entry: errorType=${e.javaClass.simpleName}" }
            MediaContainerEntry(name = entry.fileName, comment = e.message)
          }
        }.sortedWith(compareBy(natSortComparator) { it.name })
    }

  override fun getEntryStream(
    path: Path,
    entryName: String,
  ): ByteArray =
    withRar(path) { rar ->
      validateArchive(rar)
      val budget = ArchiveReadBudget(archiveProperties)
      val header =
        collectHeaders(rar, budget).firstOrNull { it.fileName == entryName }
          ?: throw EntryNotFoundException("Archive entry does not exist")
      ensureEntryWithinLimits(header.fullUnpackSize, header.fullPackSize)
      val bytes = extractEntry(rar, header, budget)
      validateImageBytes(bytes)
      bytes
    }

  private fun <T> withRar(
    path: Path,
    block: (Archive) -> T,
  ): T = withMetadataLimitedRar4(path, archiveProperties, block)

  private fun validateArchive(rar: Archive) {
    if (rar.isEncrypted) throw MediaUnsupportedException("Encrypted RAR archives are not supported", "ERR_1002")
    if (rar.mainHeader.isSolid) throw MediaUnsupportedException("Solid RAR archives are not supported", "ERR_1003")
    if (rar.mainHeader.isMultiVolume) throw MediaUnsupportedException("Multi-Volume RAR archives are not supported", "ERR_1004")
  }

  private fun collectHeaders(
    rar: Archive,
    budget: ArchiveReadBudget,
  ): List<FileHeader> {
    val result = mutableListOf<FileHeader>()
    while (true) {
      val entry = rar.nextFileHeader() ?: break
      budget.recordEntry()
      if (entry.isEncrypted) throw MediaUnsupportedException("Encrypted RAR archives are not supported", "ERR_1002")
      if (!entry.isDirectory) result += entry
    }
    return result
  }

  private fun extractEntry(
    rar: Archive,
    entry: FileHeader,
    budget: ArchiveReadBudget,
  ): ByteArray =
    try {
      extractArchiveBytes(entry.fullUnpackSize, archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE", budget) { output ->
        rar.extractFile(entry, output)
      }
    } catch (e: Exception) {
      generateSequence<Throwable>(e) { it.cause }
        .filterIsInstance<ArchiveLimitExceededException>()
        .firstOrNull()
        ?.let { throw it }
      throw e
    }

  private fun ensureEntryWithinLimits(
    unpackedSize: Long,
    packedSize: Long,
  ) {
    ensureArchiveEntrySize(unpackedSize, archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
    ensureArchiveCompressionRatio(unpackedSize, packedSize, archiveProperties.maxCompressionRatio)
  }

  private fun validateImageBytes(bytes: ByteArray) {
    val mediaType = contentDetector.detectMediaType(bytes.inputStream())
    if (contentDetector.isImage(mediaType)) {
      ensureArchiveImageDimension(imageAnalyzer.getDimension(bytes.inputStream()), archiveProperties)
    }
  }
}
