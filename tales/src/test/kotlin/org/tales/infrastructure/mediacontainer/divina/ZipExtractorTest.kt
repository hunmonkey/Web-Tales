package org.tales.infrastructure.mediacontainer.divina

import io.mockk.every
import io.mockk.mockk
import org.apache.tika.config.TikaConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.ClassPathResource
import org.tales.captureLogs
import org.tales.domain.model.Dimension
import org.tales.infrastructure.image.ImageAnalyzer
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.shelfview.infrastructure.archive.ArchiveLimitExceededException
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

class ZipExtractorTest {
  private val contentDetector = ContentDetector(TikaConfig())
  private val imageAnalyzer = ImageAnalyzer()
  private val archiveProperties = ShelfViewArchiveProperties()
  private val zipExtractor = ZipExtractor(contentDetector, imageAnalyzer, archiveProperties)

  @BeforeEach
  fun resetArchiveLimits() {
    archiveProperties.maxEntries = 10_000
    archiveProperties.maxImageBytes = 50L * 1024 * 1024
    archiveProperties.maxTotalUncompressedBytes = 2L * 1024 * 1024 * 1024
  }

  @Test
  fun `given zip file when parsing for entries then returns all images`() {
    val fileResource = ClassPathResource("archives/zip.zip")

    val entries = zipExtractor.getEntries(fileResource.file.toPath(), true)

    assertThat(entries).hasSize(1)
    with(entries.first()) {
      assertThat(name).isEqualTo("tales.png")
      assertThat(mediaType).isEqualTo("image/png")
      assertThat(dimension).isEqualTo(Dimension(48, 48))
      assertThat(fileSize).isEqualTo(3108)
    }
  }

  @Test
  fun `given zip file when parsing for entries without analyzing dimensions then returns all images without dimensions`() {
    val fileResource = ClassPathResource("archives/zip.zip")

    val entries = zipExtractor.getEntries(fileResource.file.toPath(), false)

    assertThat(entries).hasSize(1)
    with(entries.first()) {
      assertThat(name).isEqualTo("tales.png")
      assertThat(mediaType).isEqualTo("image/png")
      assertThat(dimension).isNull()
      assertThat(fileSize).isEqualTo(3108)
    }
  }

  @Test
  fun `given zip with nested cbz image archives when parsing for entries then returns inner images in order`(
    @TempDir directory: Path,
  ) {
    val firstPage = pngBytes(10, 12)
    val secondPage = pngBytes(11, 13)
    val thirdPage = pngBytes(12, 14)
    val archive = directory.resolve("nested-cbz.zip")
    writeZip(
      archive,
      "ch02.cbz" to zipBytes("images/001.png" to thirdPage),
      "ch01.cbz" to zipBytes("images/002.png" to secondPage, "images/001.png" to firstPage),
    )

    val entries = zipExtractor.getEntries(archive, true)

    assertThat(entries.map { it.name })
      .containsExactly(
        "ch01.cbz${ZIP_NESTED_ENTRY_SEPARATOR}images/001.png",
        "ch01.cbz${ZIP_NESTED_ENTRY_SEPARATOR}images/002.png",
        "ch02.cbz${ZIP_NESTED_ENTRY_SEPARATOR}images/001.png",
      )
    assertThat(entries.map { it.mediaType }).containsExactly("image/png", "image/png", "image/png")
    assertThat(entries.map { it.dimension }).containsExactly(Dimension(10, 12), Dimension(11, 13), Dimension(12, 14))

    assertThat(zipExtractor.getEntryStream(archive, entries[1].name)).isEqualTo(secondPage)
  }

  @Test
  fun `given zip with nested text archive when parsing for entries then ignores nested non images`(
    @TempDir directory: Path,
  ) {
    val archive = directory.resolve("nested-text.zip")
    writeZip(archive, "ch01.zip" to zipBytes("chapter.txt" to "hello".toByteArray()))

    val entries = zipExtractor.getEntries(archive, false)

    assertThat(entries).isEmpty()
  }

  @Test
  fun `given zip over entry count limit when parsing for entries then rejects archive`(
    @TempDir directory: Path,
  ) {
    archiveProperties.maxEntries = 2
    val archive = directory.resolve("too-many-entries.zip")
    writeZip(
      archive,
      "001.png" to byteArrayOf(1),
      "002.png" to byteArrayOf(2),
      "003.png" to byteArrayOf(3),
    )

    assertThatThrownBy { zipExtractor.getEntries(archive, false) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES")
  }

  @Test
  fun `given zip entry over image byte limit when parsing for entries then rejects archive`(
    @TempDir directory: Path,
  ) {
    archiveProperties.maxImageBytes = 4
    val archive = directory.resolve("oversized-entry.zip")
    writeZip(archive, "001.png" to ByteArray(5))

    assertThatThrownBy { zipExtractor.getEntries(archive, false) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
  }

  @Test
  fun `given mixed content entries over the image byte limit when parsing for entries then the archive is still listed`(
    @TempDir directory: Path,
  ) {
    // Above what content detection reads from an entry's header, below the entries.
    archiveProperties.maxImageBytes = 100_000
    val large = ByteArray(150_000) { 'a'.code.toByte() }
    val archive = directory.resolve("mixed-large.zip")
    writeZip(archive, "01.txt" to large, "02.pdf" to large, "03.cbz" to zipBytes("001.png" to large))

    val entries = zipExtractor.getEntries(archive, false)

    assertThat(entries.map { it.name }).containsExactlyInAnyOrder("01.txt", "02.pdf", "03.cbz")
  }

  @Test
  fun `given pages beside a video over the image byte limit when parsing for entries then the pages are kept and the video is not a page`(
    @TempDir directory: Path,
  ) {
    // Above what content detection reads from an entry's header, below the video.
    archiveProperties.maxImageBytes = 100_000
    val video = mp4Bytes(150_000)
    val page = pngBytes(10, 12)
    val flat = directory.resolve("pages-and-video.zip")
    writeZip(flat, "001.png" to page, "clip.mp4" to video)
    val nested = directory.resolve("nested-pages-and-video.zip")
    writeZip(nested, "ch01.cbz" to zipBytes("001.png" to page, "clip.mp4" to video))

    val flatEntries = zipExtractor.getEntries(flat, true)
    val nestedEntries = zipExtractor.getEntries(nested, true)

    assertThat(flatEntries.map { it.name }).containsExactlyInAnyOrder("001.png", "clip.mp4")
    assertThat(flatEntries.filter { contentDetector.isImage(it.mediaType.orEmpty()) }.map { it.name }).containsExactly("001.png")
    assertThat(nestedEntries.map { it.name }).containsExactly("ch01.cbz${ZIP_NESTED_ENTRY_SEPARATOR}001.png")
    assertThat(nestedEntries.single().dimension).isEqualTo(Dimension(10, 12))
  }

  @Test
  fun `given zip entry over image byte limit when reading page then rejects before allocation`(
    @TempDir directory: Path,
  ) {
    archiveProperties.maxImageBytes = 4
    val archive = directory.resolve("oversized-page.zip")
    writeZip(archive, "001.png" to ByteArray(5))

    assertThatThrownBy { zipExtractor.getEntryStream(archive, "001.png") }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
  }

  @Test
  fun `given large sibling entry when reading small page then budgets selected page only`(
    @TempDir directory: Path,
  ) {
    archiveProperties.maxTotalUncompressedBytes = 4
    val archive = directory.resolve("large-sibling.zip")
    val selected = byteArrayOf(1, 2, 3, 4)
    writeZip(archive, "001.png" to selected, "002.png" to ByteArray(32))

    assertThat(zipExtractor.getEntryStream(archive, "001.png")).isEqualTo(selected)
  }

  @Test
  fun `given nested outer and page over cumulative budget when reading page then rejects request`(
    @TempDir directory: Path,
  ) {
    val selected = byteArrayOf(1, 2, 3, 4)
    val nested = zipBytes("001.png" to selected)
    archiveProperties.maxTotalUncompressedBytes = nested.size + selected.size.toLong() - 1
    val archive = directory.resolve("nested-budget.zip")
    writeZip(archive, "chapter.cbz" to nested)

    assertThatThrownBy {
      zipExtractor.getEntryStream(archive, "chapter.cbz${ZIP_NESTED_ENTRY_SEPARATOR}001.png")
    }.isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_TOTAL_TOO_LARGE")
  }

  @Test
  fun `zip parser failure log omits raw parser message`(
    @TempDir directory: Path,
  ) {
    val failingDetector = mockk<ContentDetector>()
    every { failingDetector.detectMediaType(any<InputStream>()) } throws IllegalStateException(SENSITIVE_PARSER_MESSAGE)
    val extractor = ZipExtractor(failingDetector, imageAnalyzer, archiveProperties)
    val archive = directory.resolve("parser-failure.zip")
    writeZip(archive, "001.bin" to byteArrayOf(1, 2, 3))

    val captured = captureLogs(ZipExtractor::class.java) { extractor.getEntries(archive, false) }

    assertThat(captured.result).hasSize(1)
    assertThat(captured.result.single().comment).isEqualTo(SENSITIVE_PARSER_MESSAGE)
    assertThat(captured.rendered)
      .contains("errorType=IllegalStateException")
      .doesNotContain(SENSITIVE_PARSER_MESSAGE, archive.toString(), "IllegalStateException:")
  }

  private fun writeZip(
    path: Path,
    vararg entries: Pair<String, ByteArray>,
  ) {
    ZipOutputStream(Files.newOutputStream(path)).use { zip ->
      entries.forEach { (name, bytes) ->
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
      }
    }
  }

  private fun zipBytes(vararg entries: Pair<String, ByteArray>): ByteArray =
    ByteArrayOutputStream().use { output ->
      ZipOutputStream(output).use { zip ->
        entries.forEach { (name, bytes) ->
          zip.putNextEntry(ZipEntry(name))
          zip.write(bytes)
          zip.closeEntry()
        }
      }
      output.toByteArray()
    }

  /** An MP4 `ftyp` box followed by filler, enough for content detection to call it video/mp4. */
  private fun mp4Bytes(size: Int): ByteArray =
    ByteArray(size).also { bytes ->
      val header = byteArrayOf(0, 0, 0, 0x20) + "ftypisom".toByteArray() + byteArrayOf(0, 0, 2, 0) + "isomiso2avc1mp41".toByteArray()
      header.copyInto(bytes)
    }

  private fun pngBytes(
    width: Int,
    height: Int,
  ): ByteArray =
    ByteArrayOutputStream().use { output ->
      val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      ImageIO.write(image, "png", output)
      output.toByteArray()
    }

  private companion object {
    const val SENSITIVE_PARSER_MESSAGE = "SV1274_PRIVATE_ZIP_PARSER_DETAIL"
  }
}
