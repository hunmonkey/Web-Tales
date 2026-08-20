package org.tales.infrastructure.mediacontainer.divina

import com.github.gotson.nightcompress.Archive
import com.github.gotson.nightcompress.ReadSupportCompression
import com.github.gotson.nightcompress.ReadSupportFilter
import com.github.gotson.nightcompress.ReadSupportFormat
import io.github.oshai.kotlinlogging.KotlinLogging
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.annotation.Configuration
import org.tales.domain.model.EntryNotFoundException
import org.tales.domain.model.MediaContainerEntry
import org.tales.domain.model.MediaType
import org.tales.infrastructure.image.ImageAnalyzer
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.shelfview.infrastructure.archive.ArchiveLimitExceededException
import org.tales.shelfview.infrastructure.archive.ArchiveReadBudget
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import org.tales.shelfview.infrastructure.archive.ensureArchiveEntrySize
import org.tales.shelfview.infrastructure.archive.ensureArchiveImageDimension
import org.tales.shelfview.infrastructure.archive.readArchiveBytes
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

@Configuration(proxyBeanMethods = false)
class Rar5Configuration : BeanDefinitionRegistryPostProcessor {
  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {}

  override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
    try {
      if (Archive.isAvailable()) {
        val builder = BeanDefinitionBuilder.genericBeanDefinition(Rar5Extractor::class.java).setLazyInit(true)
        registry.registerBeanDefinition("rar5Extractor", builder.beanDefinition)
        logger.info { "Rar5 extractor is enabled" }
      }
    } catch (e: Throwable) {
      logger.warn { "RAR5 extractor could not be loaded: errorType=${e.javaClass.simpleName}" }
    }
  }
}

class Rar5Extractor(
  private val contentDetector: ContentDetector,
  private val imageAnalyzer: ImageAnalyzer,
  private val archiveProperties: ShelfViewArchiveProperties,
) : DivinaExtractor {
  private val natSortComparator: Comparator<String> = CaseInsensitiveSimpleNaturalComparator.getInstance()

  override fun mediaTypes(): List<String> = listOf(MediaType.RAR_5.type)

  override fun getEntries(
    path: Path,
    analyzeDimensions: Boolean,
  ): List<MediaContainerEntry> =
    Archive(path, setOf(ReadSupportCompression.NONE), setOf(ReadSupportFilter.NONE), setOf(ReadSupportFormat.RAR5)).use { rar ->
      val budget = ArchiveReadBudget(archiveProperties)
      val entries = mutableListOf<MediaContainerEntry>()

      while (true) {
        val entry = rar.nextEntry ?: break
        budget.recordEntry()
        if (entry.name.endsWith('/')) continue

        val mediaEntry =
          try {
            val declaredSize = requiredEntrySize(entry.size)
            ensureArchiveEntrySize(declaredSize, archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
            val buffer =
              rar.inputStream.use {
                it.readArchiveBytes(archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE", declaredSize, budget)
              }
            if (buffer.size.toLong() != declaredSize) throw IllegalArgumentException("RAR5 entry size does not match its metadata")
            val mediaType = buffer.inputStream().use { contentDetector.detectMediaType(it) }
            val dimension =
              if (analyzeDimensions && contentDetector.isImage(mediaType))
                buffer.inputStream().use { imageAnalyzer.getDimension(it) }
              else
                null
            ensureArchiveImageDimension(dimension, archiveProperties)
            val fileSize = entry.size
            MediaContainerEntry(name = entry.name, mediaType = mediaType, dimension = dimension, fileSize = fileSize)
          } catch (e: ArchiveLimitExceededException) {
            throw e
          } catch (e: Exception) {
            logger.warn { "Could not analyze a RAR5 entry: errorType=${e.javaClass.simpleName}" }
            MediaContainerEntry(name = entry.name, comment = e.message)
          }
        entries += mediaEntry
      }

      entries.sortedWith(compareBy(natSortComparator) { it.name })
    }

  override fun getEntryStream(
    path: Path,
    entryName: String,
  ): ByteArray {
    val fingerprint = archiveFingerprint(path)
    val expectedSize = inspectEntry(path, entryName)
    if (archiveFingerprint(path) != fingerprint) throw IllegalArgumentException("RAR5 archive changed while it was being inspected")

    return openArchive(path).use { rar ->
      val budget = ArchiveReadBudget(archiveProperties)
      var selectedBytes: ByteArray? = null
      while (selectedBytes == null) {
        val entry = rar.nextEntry ?: throw EntryNotFoundException("Archive entry does not exist")
        budget.recordEntry()
        if (entry.name != entryName) continue

        val declaredSize = requiredEntrySize(entry.size)
        if (declaredSize != expectedSize) throw IllegalArgumentException("RAR5 archive changed while it was being inspected")
        ensureArchiveEntrySize(declaredSize, archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
        val bytes =
          rar.inputStream.use {
            it.readArchiveBytes(archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE", declaredSize, budget)
          }
        if (bytes.size.toLong() != declaredSize) throw IllegalArgumentException("RAR5 entry size does not match its metadata")
        validateImageBytes(bytes)
        selectedBytes = bytes
      }
      requireNotNull(selectedBytes)
    }
  }

  private fun inspectEntry(
    path: Path,
    entryName: String,
  ): Long =
    openArchive(path).use { rar ->
      val budget = ArchiveReadBudget(archiveProperties)
      var selectedSize: Long? = null
      while (true) {
        val entry = rar.nextEntry ?: break
        budget.recordEntry()
        if (entry.name.endsWith('/')) continue

        val declaredSize = requiredEntrySize(entry.size)
        ensureArchiveEntrySize(declaredSize, archiveProperties.maxImageBytes, "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
        if (entry.name == entryName) selectedSize = declaredSize
      }
      selectedSize ?: throw EntryNotFoundException("Archive entry does not exist")
    }

  private fun openArchive(path: Path): Archive = Archive(path, setOf(ReadSupportCompression.NONE), setOf(ReadSupportFilter.NONE), setOf(ReadSupportFormat.RAR5))

  private fun requiredEntrySize(size: Long?): Long = size?.takeIf { it >= 0 } ?: throw ArchiveLimitExceededException("SHELFVIEW_ARCHIVE_ENTRY_SIZE_UNKNOWN")

  private fun validateImageBytes(bytes: ByteArray) {
    val mediaType = contentDetector.detectMediaType(bytes.inputStream())
    if (contentDetector.isImage(mediaType)) {
      ensureArchiveImageDimension(imageAnalyzer.getDimension(bytes.inputStream()), archiveProperties)
    }
  }

  private fun archiveFingerprint(path: Path): Pair<Long, Long> = Files.size(path) to Files.getLastModifiedTime(path).toMillis()
}
