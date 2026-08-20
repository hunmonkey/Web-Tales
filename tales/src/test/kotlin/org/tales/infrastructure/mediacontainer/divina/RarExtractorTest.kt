package org.tales.infrastructure.mediacontainer.divina

import io.mockk.every
import io.mockk.mockk
import org.apache.tika.config.TikaConfig
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.tales.captureLogs
import org.tales.domain.model.Dimension
import org.tales.infrastructure.image.ImageAnalyzer
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.shelfview.infrastructure.archive.ArchiveLimitExceededException
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import java.io.InputStream

class RarExtractorTest {
  private val contentDetector = ContentDetector(TikaConfig())
  private val imageAnalyzer = ImageAnalyzer()
  private val archiveProperties = ShelfViewArchiveProperties()
  private val rarExtractor = RarExtractor(contentDetector, imageAnalyzer, archiveProperties)

  @BeforeEach
  fun resetArchiveLimits() {
    archiveProperties.maxEntries = 10_000
    archiveProperties.maxImageBytes = 50L * 1024 * 1024
  }

  @Test
  fun `given rar file when parsing for entries then returns all images`() {
    val fileResource = ClassPathResource("archives/rar4.rar")

    val entries = rarExtractor.getEntries(fileResource.file.toPath(), true)

    Assertions.assertThat(entries).hasSize(3)
    with(entries.first()) {
      Assertions.assertThat(name).isEqualTo("tales-1.png")
      Assertions.assertThat(mediaType).isEqualTo("image/png")
      Assertions.assertThat(dimension).isEqualTo(Dimension(48, 48))
      Assertions.assertThat(fileSize).isEqualTo(3108)
    }
  }

  @Test
  fun `given rar file when parsing for entries without analyzing dimensions then returns all images without dimensions`() {
    val fileResource = ClassPathResource("archives/rar4.rar")

    val entries = rarExtractor.getEntries(fileResource.file.toPath(), false)

    Assertions.assertThat(entries).hasSize(3)
    with(entries.first()) {
      Assertions.assertThat(name).isEqualTo("tales-1.png")
      Assertions.assertThat(mediaType).isEqualTo("image/png")
      Assertions.assertThat(dimension).isNull()
      Assertions.assertThat(fileSize).isEqualTo(3108)
    }
  }

  @Test
  fun `given rar over entry count limit when parsing for entries then rejects archive`() {
    archiveProperties.maxEntries = 2
    val fileResource = ClassPathResource("archives/rar4.rar")

    Assertions
      .assertThatThrownBy { rarExtractor.getEntries(fileResource.file.toPath(), false) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES")
  }

  @Test
  fun `given rar entry over image byte limit when parsing for entries then rejects archive`() {
    archiveProperties.maxImageBytes = 3107
    val fileResource = ClassPathResource("archives/rar4.rar")

    Assertions
      .assertThatThrownBy { rarExtractor.getEntries(fileResource.file.toPath(), false) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
  }

  @Test
  fun `given rar entry over image byte limit when reading page then rejects before allocation`() {
    archiveProperties.maxImageBytes = 3107
    val fileResource = ClassPathResource("archives/rar4.rar")

    Assertions
      .assertThatThrownBy { rarExtractor.getEntryStream(fileResource.file.toPath(), "tales-1.png") }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
  }

  @Test
  fun `rar parser failure log omits raw parser message`() {
    val failingDetector = mockk<ContentDetector>()
    every { failingDetector.detectMediaType(any<InputStream>()) } throws IllegalStateException(SENSITIVE_PARSER_MESSAGE)
    val extractor = RarExtractor(failingDetector, imageAnalyzer, archiveProperties)
    val archive = ClassPathResource("archives/rar4.rar").file.toPath()

    val captured = captureLogs(RarExtractor::class.java) { extractor.getEntries(archive, false) }

    Assertions.assertThat(captured.result).hasSize(3)
    Assertions.assertThat(captured.result).allMatch { it.comment == SENSITIVE_PARSER_MESSAGE }
    Assertions.assertThat(captured.rendered).contains("errorType=IllegalStateException")
    Assertions.assertThat(captured.rendered).doesNotContain(SENSITIVE_PARSER_MESSAGE, archive.toString(), "IllegalStateException:")
  }

  private companion object {
    const val SENSITIVE_PARSER_MESSAGE = "SV1274_PRIVATE_RAR_PARSER_DETAIL"
  }
}
