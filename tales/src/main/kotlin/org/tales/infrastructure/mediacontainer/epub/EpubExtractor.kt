package org.tales.infrastructure.mediacontainer.epub

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.compress.archivers.ArchiveEntry
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.tales.domain.model.BookPage
import org.tales.domain.model.EntryNotFoundException
import org.tales.domain.model.EpubTocEntry
import org.tales.domain.model.MediaFile
import org.tales.domain.model.R2Locator
import org.tales.domain.model.TypedBytes
import org.tales.infrastructure.image.ImageAnalyzer
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.infrastructure.util.getEntryBytes
import org.tales.infrastructure.util.getEntryInputStream
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import org.tales.shelfview.infrastructure.archive.withPreflightedZip
import java.net.URLDecoder
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.math.ceil
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger {}

@Service
class EpubExtractor(
  private val contentDetector: ContentDetector,
  private val imageAnalyzer: ImageAnalyzer,
  @param:Value("#{@talesProperties.epubDivinaLetterCountThreshold}") private val letterCountThreshold: Int,
  private val archiveProperties: ShelfViewArchiveProperties,
) {
  /**
   * Retrieves a specific entry by name from the zip archive
   */
  fun getEntryStream(
    path: Path,
    entryName: String,
  ): ByteArray =
    readEntryBytes(path, entryName, ignoreLocalFileHeader = true)
      ?: readEntryBytes(path, entryName, ignoreLocalFileHeader = false)
      ?: throw EntryNotFoundException("Entry does not exist")

  fun isEpub(path: Path): Boolean =
    try {
      getEntryStream(path, "mimetype").decodeToString().trim() == "application/epub+zip"
    } catch (_: Exception) {
      false
    }

  /**
   * Retrieves the book cover along with its mediaType from the epub 2/3 manifest
   */
  fun getCover(path: Path): TypedBytes? =
    path.epub(archiveProperties) { (zip, opfDoc, opfDir, manifest) ->
      val coverManifestItem =
        // EPUB 3 - try to get cover from manifest properties 'cover-image'
        manifest.values.firstOrNull { it.properties.contains("cover-image") }
          ?: // EPUB 2 - get cover from meta element with name="cover"
          opfDoc
            .selectFirst("*|metadata > *|meta[name=cover]")
            ?.attr("content")
            ?.ifBlank { null }
            ?.let { manifest[it] }
          ?: // try id="cover-image"
          manifest.values.firstOrNull { it.id == "cover-image" }
      if (coverManifestItem != null) {
        val href = URLDecoder.decode(coverManifestItem.href, Charsets.UTF_8)
        val mediaType = coverManifestItem.mediaType
        val coverPath = normalizeHref(opfDir, href)
        zip.getEntryBytes(coverPath)?.let { coverBytes ->
          TypedBytes(
            coverBytes,
            mediaType,
          )
        }
      } else {
        null
      }
    }

  private fun readEntryBytes(
    path: Path,
    entryName: String,
    ignoreLocalFileHeader: Boolean,
  ): ByteArray? =
    withPreflightedZip(
      path,
      archiveProperties,
      configure = {
        setUseUnicodeExtraFields(true)
        setIgnoreLocalFileHeader(ignoreLocalFileHeader)
      },
    ) { zip ->
      zip.getEntryBytes(entryName)
    }

  fun getResources(epub: EpubPackage): List<MediaFile> {
    val spine =
      epub.opfDoc
        .select("*|spine > *|itemref")
        .map { it.attr("idref") }
        .mapNotNull { epub.manifest[it] }

    val pages =
      spine.map { page ->
        MediaFile(
          normalizeHref(epub.opfDir, URLDecoder.decode(page.href, Charsets.UTF_8)),
          page.mediaType,
          MediaFile.SubType.EPUB_PAGE,
        )
      }

    val assets =
      epub.manifest.values.filterNot { spine.contains(it) }.map {
        MediaFile(
          normalizeHref(epub.opfDir, URLDecoder.decode(it.href, Charsets.UTF_8)),
          it.mediaType,
          MediaFile.SubType.EPUB_ASSET,
        )
      }

    val zipEntries = epub.zip.entries.toList()
    return (pages + assets).map { resource ->
      resource.copy(fileSize = zipEntries.firstOrNull { it.name == resource.fileName }?.let { if (it.size == ArchiveEntry.SIZE_UNKNOWN) null else it.size })
    }
  }

  fun getDivinaPages(
    epub: EpubPackage,
    analyzeDimensions: Boolean,
  ): List<BookPage> {
    val pageCount =
      run {
        val spine =
          epub.opfDoc
            .select("*|spine > *|itemref")
            .map { it.attr("idref") }
            .mapNotNull { idref -> epub.manifest[idref]?.href?.let { normalizeHref(epub.opfDir, it) } }

        epub.zip.entries
          .toList()
          .count { it.name in spine }
      }

    val pagesWithImages =
      epub.opfDoc
        .select("*|spine > *|itemref")
        .map { it.attr("idref") }
        .mapNotNull { idref ->
          val manifestItem = epub.manifest[idref] ?: return@mapNotNull null
          normalizeHref(epub.opfDir, manifestItem.href) to manifestItem.mediaType
        }.map { (pagePath, mediaType) ->
          if (mediaType.startsWith("image", true)) {
            // image in spine
            listOf(Path(pagePath).normalize().invariantSeparatorsPathString)
          } else {
            val doc = epub.zip.getEntryInputStream(pagePath)?.use { Jsoup.parse(it, null, "") } ?: return@map emptyList()

            // if a page has text over the threshold then the book is not divina compatible
            if (doc.body().text().length > letterCountThreshold) return emptyList()

            val img =
              doc
                .getElementsByTag("img")
                .map { it.attr("src") } // get the src, which can be a relative path

            val svg =
              doc
                .select("svg > image[xlink:href]")
                .map { it.attr("xlink:href") } // get the source, which can be a relative path

            (img + svg).map { (Path(pagePath).parent ?: Path("")).resolve(it).normalize().invariantSeparatorsPathString } // resolve it against the page folder
          }
        }

    if (pagesWithImages.size != pageCount) {
      logger.info { "Epub Divina detection failed: book has ${pagesWithImages.size} pages with images, but $pageCount total pages" }
      return emptyList()
    }
    // Only keep unique image path for each page. KCC sometimes generates HTML pages with 5 times the same image.
    val imagesPath = pagesWithImages.flatMap { it.distinct() }
    if (imagesPath.size != pageCount) {
      logger.info { "Epub Divina detection failed: book has ${imagesPath.size} detected images, but $pageCount total pages" }
      return emptyList()
    }

    val divinaPages =
      imagesPath.mapNotNull { imagePath ->
        val mediaType =
          epub.manifest.values
            .firstOrNull { normalizeHref(epub.opfDir, it.href) == imagePath }
            ?.mediaType ?: return@mapNotNull null
        val zipEntry = epub.zip.getEntry(imagePath)
        if (!contentDetector.isImage(mediaType)) return@mapNotNull null

        val dimension =
          if (analyzeDimensions)
            epub.zip.getInputStream(zipEntry).use { imageAnalyzer.getDimension(it) }
          else
            null
        val fileSize = if (zipEntry.size == ArchiveEntry.SIZE_UNKNOWN) null else zipEntry.size
        BookPage(fileName = imagePath, mediaType = mediaType, dimension = dimension, fileSize = fileSize)
      }

    if (divinaPages.size != pageCount) {
      logger.info { "Epub Divina detection failed: book has ${divinaPages.size} detected divina pages, but $pageCount total pages" }
      return emptyList()
    }
    return divinaPages
  }

  /**
   * Computes an approximate page count using the Readium method,
   * which counts 1 page for every 1024 bytes of compressed data for each resource.
   */
  fun computePageCount(epub: EpubPackage): Int {
    val spine =
      epub.opfDoc
        .select("*|spine > *|itemref")
        .map { it.attr("idref") }
        .mapNotNull { idref -> epub.manifest[idref]?.href?.let { normalizeHref(epub.opfDir, it) } }

    return epub.zip.entries
      .toList()
      .filter { it.name in spine }
      .sumOf { ceil(it.compressedSize / 1024.0).toInt() }
  }

  fun isFixedLayout(epub: EpubPackage) =
    epub.opfDoc.selectFirst("*|metadata > *|meta[property=rendition:layout]")?.text() == "pre-paginated" ||
      epub.opfDoc.selectFirst("*|metadata > *|meta[name=fixed-layout]")?.attr("content") == "true"

  fun computePositions(
    resources: List<MediaFile>,
    isFixedLayout: Boolean,
  ): List<R2Locator> {
    val readingOrder = resources.filter { it.subType == MediaFile.SubType.EPUB_PAGE }

    var startPosition = 1

    val positions =
      if (isFixedLayout) {
        // for fixed-layout book we create 1 position per page
        readingOrder.map {
          R2Locator(
            href = it.fileName,
            type = it.mediaType ?: "application/octet-stream",
            locations = R2Locator.Location(progression = 0F, position = startPosition++),
          )
        }
      } else {
        // this is the Readium algorithm
        // we create 1 position every 1024 bytes
        readingOrder.flatMap { file ->
          val positionCount = maxOf(1, ceil((file.fileSize ?: 0) / 1024.0).roundToInt())
          (0 until positionCount).map { p ->
            R2Locator(
              href = file.fileName,
              type = file.mediaType ?: "application/octet-stream",
              locations = R2Locator.Location(progression = p.toFloat() / positionCount, position = startPosition++),
            )
          }
        }
      }

    // finally we compute the total progression for each position
    return positions.map { locator ->
      val totalProgression = locator.locations?.position?.let { it.toFloat() / positions.size }
      locator.copy(locations = locator.locations?.copy(totalProgression = totalProgression))
    }
  }

  fun getToc(epub: EpubPackage): List<EpubTocEntry> {
    // Epub 3
    epub.getNavResource()?.let { processNav(it, Epub3Nav.TOC) }?.let { if (it.isNotEmpty()) return it }

    // Epub 2
    epub.getNcxResource()?.let { return processNcx(it, Epub2Nav.TOC) }
    return emptyList()
  }

  fun getPageList(epub: EpubPackage): List<EpubTocEntry> {
    // Epub 3
    epub.getNavResource()?.let { processNav(it, Epub3Nav.PAGELIST) }?.let { if (it.isNotEmpty()) return it }

    // Epub 2
    epub.getNcxResource()?.let { return processNcx(it, Epub2Nav.PAGELIST) }
    return emptyList()
  }

  fun getLandmarks(epub: EpubPackage): List<EpubTocEntry> {
    // Epub 3
    epub.getNavResource()?.let { processNav(it, Epub3Nav.LANDMARKS) }?.let { if (it.isNotEmpty()) return it }

    // Epub 2
    return processOpfGuide(epub.opfDoc, epub.opfDir)
  }
}
