package org.tales.infrastructure.mediacontainer.pdf

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.apache.pdfbox.Loader
import org.apache.pdfbox.multipdf.PageExtractor
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.rendering.ImageType.RGB
import org.apache.pdfbox.rendering.PDFRenderer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.tales.domain.model.Dimension
import org.tales.domain.model.MediaContainerEntry
import org.tales.domain.model.MediaType
import org.tales.domain.model.TypedBytes
import org.tales.infrastructure.image.ImageType
import org.tales.shelfview.infrastructure.archive.ArchiveLimitExceededException
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import org.tales.shelfview.infrastructure.archive.ensureArchiveImageDimension
import org.tales.shelfview.infrastructure.archive.withArchiveByteLimit
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.imageio.ImageIO
import kotlin.concurrent.withLock
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger {}

@Service
class PdfExtractor(
  @Qualifier("pdfImageType")
  private val imageType: ImageType,
  @Qualifier("pdfResolution")
  private val resolution: Float,
  private val archiveProperties: ShelfViewArchiveProperties,
) {
  /**
   * Open documents kept for the page-image path, per ADR-0020.
   *
   * Rendering a page used to re-parse the whole file, and the reader asks for
   * six pages at a time when it prefetches, so reading a long PDF parsed it
   * hundreds of times. The key carries last-modified and size as well as the
   * path, so ordinary replacements with changed metadata use a fresh handle.
   *
   * Two entries is enough for a reader that is in one book at a time, and it
   * bounds what the open documents cost in memory and file descriptors.
   */
  private val documentCache =
    Caffeine
      .newBuilder()
      .maximumSize(2)
      .expireAfterAccess(10, TimeUnit.MINUTES)
      .executor { command -> command.run() }
      .removalListener<String, CachedPdfDocument> { _, cached, _ -> cached?.close() }
      .build<String, CachedPdfDocument>()

  /**
   * Rendered page images, keyed by document identity and page number. Rendering
   * is the expensive step this class performs - seconds of CPU per page at the
   * configured resolution - and the reader, its prefetch burst and the page
   * thumbnail endpoint all ask for the same pages. An entry that falls out is
   * simply rendered again, so eviction costs time, never correctness. Bounded
   * by bytes rather than entries so unusually large pages cannot grow the
   * worst case.
   */
  private val pageImageCache =
    Caffeine
      .newBuilder()
      .maximumWeight(64L * 1024 * 1024)
      .weigher<String, TypedBytes> { _, page -> page.bytes.size }
      .expireAfterAccess(30, TimeUnit.MINUTES)
      .build<String, TypedBytes>()

  fun getPages(
    path: Path,
    analyzeDimensions: Boolean,
  ): List<MediaContainerEntry> =
    Loader.loadPDF(path.toFile()).use { pdf ->
      (0 until pdf.numberOfPages).map { index ->
        val page = pdf.getPage(index)
        val dimension = if (analyzeDimensions) Dimension(page.cropBox.width.roundToInt(), page.cropBox.height.roundToInt()) else null
        MediaContainerEntry(name = "${index + 1}", dimension = dimension)
      }
    }

  fun getPageContentAsImage(
    path: Path,
    pageNumber: Int,
  ): TypedBytes {
    // No readable file attributes means no stable identity to cache under.
    val key = cacheKey(path) ?: return renderPageContent(path, null, pageNumber)

    // Caffeine computes one entry at a time, so concurrent requests for the
    // same page share a single render instead of each paying for their own.
    return requireNotNull(pageImageCache.get("$key|$pageNumber") { renderPageContent(path, key, pageNumber) })
  }

  private fun renderPageContent(
    path: Path,
    key: String?,
    pageNumber: Int,
  ): TypedBytes {
    if (key != null) renderFromCache(key, pageNumber)?.let { return it }

    // Document cache miss. This costs exactly what the method cost before the
    // cache existed, and it leaves a warm document behind for the next page.
    val document = Loader.loadPDF(path.toFile())
    var cacheOwnsDocument = false
    try {
      val rendered = renderPage(document, pageNumber)
      cacheOwnsDocument = key != null && keepInCache(key, document)
      return rendered
    } finally {
      // `finally` also covers Error subclasses and a failed cache insertion.
      // The document stays open only after the cache explicitly took ownership.
      if (!cacheOwnsDocument) document.close()
    }
  }

  fun getPageContentAsPdf(
    path: Path,
    pageNumber: Int,
  ): TypedBytes {
    Loader.loadPDF(path.toFile()).use { pdf ->
      val bytes =
        ByteArrayOutputStream().use { out ->
          PageExtractor(pdf, pageNumber, pageNumber).extract().save(out)
          out.toByteArray()
        }
      return TypedBytes(bytes, MediaType.PDF.type)
    }
  }

  @PreDestroy
  fun closeCachedDocuments() {
    documentCache.invalidateAll()
    documentCache.cleanUp()
  }

  internal fun evictCachedDocument(path: Path) {
    cacheKey(path)?.let { key ->
      documentCache.invalidate(key)
      // The key embeds the file identity, so this drops exactly that
      // document's rendered pages.
      pageImageCache.asMap().keys.removeIf { it.startsWith("$key|") }
    }
    documentCache.cleanUp()
  }

  /**
   * Renders from the cached document, or returns null to say "load the file
   * again". Waits for the lock: `PDDocument` cannot render concurrently, and
   * with the page-image cache deduplicating repeat requests, a queue here is at
   * most one render per distinct page. The old non-waiting fallback re-parsed
   * the whole file once per concurrent request in the reader's prefetch burst,
   * which cost far more than the wait it avoided.
   */
  private fun renderFromCache(
    key: String,
    pageNumber: Int,
  ): TypedBytes? {
    val cached = documentCache.getIfPresent(key) ?: return null

    val rendered =
      cached.lock.withLock {
        try {
          renderPage(cached.document, pageNumber)
        } catch (e: ArchiveLimitExceededException) {
          throw e
        } catch (e: Exception) {
          logger.debug { "Cached PDF render failed; loading the file again: page=$pageNumber, errorType=${e.javaClass.simpleName}" }
          null
        }
      }

    // A cached document that cannot render is spent - it was closed under us by
    // an eviction, or the file changed underneath. Drop it so the next request
    // starts from a fresh one.
    if (rendered == null) documentCache.invalidate(key)

    return rendered
  }

  /** True when the cache took ownership of the document and will close it. */
  private fun keepInCache(
    key: String,
    document: PDDocument,
  ): Boolean = documentCache.asMap().putIfAbsent(key, CachedPdfDocument(document)) == null

  private fun renderPage(
    pdf: PDDocument,
    pageNumber: Int,
  ): TypedBytes {
    val page = pdf.getPage(pageNumber - 1)
    ensureArchiveImageDimension(page.scaledDimension(), archiveProperties)
    val image = PDFRenderer(pdf).renderImage(pageNumber - 1, page.getScale(), RGB)
    val bytes =
      ByteArrayOutputStream().use { out ->
        ImageIO.write(
          image,
          imageType.imageIOFormat,
          out.withArchiveByteLimit(archiveProperties.maxImageBytes, "SHELFVIEW_IMAGE_TOO_LARGE"),
        )
        out.toByteArray()
      }
    return TypedBytes(bytes, imageType.mediaType)
  }

  private fun cacheKey(path: Path): String? =
    try {
      "${path.toAbsolutePath()}|${Files.getLastModifiedTime(path).toMillis()}|${Files.size(path)}"
    } catch (e: Exception) {
      logger.debug { "Could not read PDF file attributes; cache disabled: errorType=${e.javaClass.simpleName}" }
      null
    }

  private class CachedPdfDocument(
    val document: PDDocument,
  ) {
    val lock = ReentrantLock()

    /**
     * Waits for any render in flight before closing, so an eviction cannot pull
     * the document out from under a request that already holds the lock.
     */
    fun close() {
      lock.withLock {
        try {
          document.close()
        } catch (e: Exception) {
          logger.debug { "Closing a cached PDF document failed: errorType=${e.javaClass.simpleName}" }
        }
      }
    }
  }

  private fun PDPage.getScale() = getScale(cropBox.width, cropBox.height)

  private fun PDPage.scaledDimension(): Dimension {
    val scale = getScale()
    return Dimension((cropBox.width * scale).roundToInt(), (cropBox.height * scale).roundToInt())
  }

  private fun getScale(
    width: Float,
    height: Float,
  ) = resolution / minOf(width, height)

  fun scaleDimension(dimension: Dimension): Dimension {
    val scale = getScale(dimension.width.toFloat(), dimension.height.toFloat())
    return Dimension((dimension.width * scale).roundToInt(), (dimension.height * scale).roundToInt())
  }
}
