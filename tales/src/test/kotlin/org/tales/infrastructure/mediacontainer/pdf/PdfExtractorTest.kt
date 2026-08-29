package org.tales.infrastructure.mediacontainer.pdf

import com.github.benmanes.caffeine.cache.Cache
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.ClassPathResource
import org.tales.infrastructure.image.ImageType
import org.tales.shelfview.infrastructure.archive.ArchiveLimitExceededException
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import java.nio.file.Files
import java.nio.file.Path

class PdfExtractorTest {
  private val archiveProperties = ShelfViewArchiveProperties()
  private val pdfExtractor = PdfExtractor(ImageType.JPEG, 1000F, archiveProperties)

  @BeforeEach
  fun resetArchiveLimits() {
    archiveProperties.maxImageBytes = 50L * 1024 * 1024
    archiveProperties.maxImageWidth = 20_000
    archiveProperties.maxImageHeight = 20_000
    archiveProperties.maxImagePixels = 100_000_000
  }

  @AfterEach
  fun closeCachedDocuments() {
    pdfExtractor.closeCachedDocuments()
  }

  @Test
  fun `given pdf file when getting pages then pages are returned`() {
    val fileResource = ClassPathResource("pdf/tales.pdf")

    val pages = pdfExtractor.getPages(fileResource.file.toPath(), true)

    assertThat(pages).hasSize(1)
    assertThat(pages.first().dimension?.width).isEqualTo(512)
  }

  @Test
  fun `given cached pdf when extractor closes then document is already closed on return`() {
    val path = ClassPathResource("pdf/tales.pdf").file.toPath()

    val image = pdfExtractor.getPageContentAsImage(path, 1)
    val document = cachedDocument(pdfExtractor)

    assertThat(image.bytes).isNotEmpty()
    assertThat(document.document.isClosed).isFalse()

    pdfExtractor.closeCachedDocuments()

    assertThat(document.document.isClosed).isTrue()
  }

  @Test
  fun `given cached pdf when its source is evicted then document is closed`() {
    val path = ClassPathResource("pdf/tales.pdf").file.toPath()

    pdfExtractor.getPageContentAsImage(path, 1)
    val document = cachedDocument(pdfExtractor)

    pdfExtractor.evictCachedDocument(path)

    assertThat(document.document.isClosed).isTrue()
  }

  @Test
  fun `given rendered page when document cache is gone then page is served from the page image cache`() {
    val path = ClassPathResource("pdf/tales.pdf").file.toPath()

    val first = pdfExtractor.getPageContentAsImage(path, 1)
    pdfExtractor.closeCachedDocuments()

    mockkStatic(Loader::class)
    try {
      every { Loader.loadPDF(path.toFile()) } throws AssertionError("page should come from the page image cache")

      val second = pdfExtractor.getPageContentAsImage(path, 1)

      assertThat(second.bytes).isEqualTo(first.bytes)
      assertThat(second.mediaType).isEqualTo(first.mediaType)
    } finally {
      unmockkStatic(Loader::class)
    }
  }

  @Test
  fun `given rendered page when its source is evicted then the page image cache forgets it`() {
    val path = ClassPathResource("pdf/tales.pdf").file.toPath()

    pdfExtractor.getPageContentAsImage(path, 1)
    pdfExtractor.evictCachedDocument(path)

    mockkStatic(Loader::class)
    try {
      every { Loader.loadPDF(path.toFile()) } throws AssertionError("eviction must drop the rendered page")

      assertThatThrownBy { pdfExtractor.getPageContentAsImage(path, 1) }
        .isInstanceOf(AssertionError::class.java)
    } finally {
      unmockkStatic(Loader::class)
    }
  }

  @Test
  fun `given Error while rendering then locally loaded document is closed`(
    @TempDir directory: Path,
  ) {
    // A private copy keeps the page image cache cold for this path; tests share
    // one extractor instance under the per_class lifecycle.
    val path = directory.resolve("render-error.pdf")
    Files.copy(ClassPathResource("pdf/tales.pdf").file.toPath(), path)
    val document = mockk<PDDocument>()
    mockkStatic(Loader::class)
    try {
      every { Loader.loadPDF(path.toFile()) } returns document
      every { document.getPage(0) } throws AssertionError("render failed")
      every { document.close() } just Runs

      assertThatThrownBy { pdfExtractor.getPageContentAsImage(path, 1) }
        .isInstanceOf(AssertionError::class.java)

      verify(exactly = 1) { document.close() }
    } finally {
      unmockkStatic(Loader::class)
    }
  }

  @Test
  fun `given pdf page over decoded image limit when rendering then rejects before allocation`(
    @TempDir directory: Path,
  ) {
    val path = directory.resolve("oversized-page.pdf")
    PDDocument().use { document ->
      document.addPage(PDPage(PDRectangle(1F, 100_000F)))
      document.save(path.toFile())
    }

    assertThatThrownBy { pdfExtractor.getPageContentAsImage(path, 1) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_IMAGE_DIMENSIONS_TOO_LARGE")
  }

  @Test
  fun `given encoded pdf page over byte limit when rendering then rejects output`(
    @TempDir directory: Path,
  ) {
    archiveProperties.maxImageBytes = 1
    // A private copy keeps the page image cache cold for this path; tests share
    // one extractor instance under the per_class lifecycle.
    val path = directory.resolve("over-byte-limit.pdf")
    Files.copy(ClassPathResource("pdf/tales.pdf").file.toPath(), path)

    assertThatThrownBy { pdfExtractor.getPageContentAsImage(path, 1) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_IMAGE_TOO_LARGE")
  }

  private fun cachedDocument(extractor: PdfExtractor): PDDocument {
    val cacheField = PdfExtractor::class.java.getDeclaredField("documentCache")
    cacheField.isAccessible = true
    val cache = cacheField.get(extractor) as Cache<*, *>
    val cached = cache.asMap().values.single()
    val documentField = cached.javaClass.getDeclaredField("document")
    documentField.isAccessible = true
    return documentField.get(cached) as PDDocument
  }
}
