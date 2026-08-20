package org.tales.infrastructure.mediacontainer.divina

import com.github.gotson.nightcompress.Archive
import io.mockk.every
import io.mockk.mockk
import org.apache.tika.config.TikaConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.tales.captureLogs
import org.tales.infrastructure.image.ImageAnalyzer
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.shelfview.infrastructure.archive.ArchiveLimitExceededException
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import java.io.InputStream

class Rar5ExtractorTest {
  private val contentDetector = ContentDetector(TikaConfig())
  private val imageAnalyzer = ImageAnalyzer()
  private val archiveProperties = ShelfViewArchiveProperties()
  private val rar5Extractor = Rar5Extractor(contentDetector, imageAnalyzer, archiveProperties)

  @Test
  fun `given rar5 entry over image byte limit when parsing for entries then rejects archive`() {
    assumeTrue(runCatching { Archive.isAvailable() }.getOrDefault(false), "RAR5 native support is unavailable")
    archiveProperties.maxImageBytes = 3107
    val fileResource = ClassPathResource("archives/rar5.rar")

    assertThatThrownBy { rar5Extractor.getEntries(fileResource.file.toPath(), false) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE")
  }

  @Test
  fun `rar5 parser failure log omits raw parser message when native support is available`() {
    assumeTrue(runCatching { Archive.isAvailable() }.getOrDefault(false), "RAR5 native support is unavailable")
    val failingDetector = mockk<ContentDetector>()
    every { failingDetector.detectMediaType(any<InputStream>()) } throws IllegalStateException(SENSITIVE_PARSER_MESSAGE)
    val extractor = Rar5Extractor(failingDetector, imageAnalyzer, archiveProperties)
    val archive = ClassPathResource("archives/rar5.rar").file.toPath()

    val captured = captureLogs(Rar5Extractor::class.java) { extractor.getEntries(archive, false) }

    assertThat(captured.result).isNotEmpty
    assertThat(captured.result).allMatch { it.comment == SENSITIVE_PARSER_MESSAGE }
    assertThat(captured.rendered)
      .contains("errorType=IllegalStateException")
      .doesNotContain(SENSITIVE_PARSER_MESSAGE, archive.toString(), "IllegalStateException:")
  }

  private companion object {
    const val SENSITIVE_PARSER_MESSAGE = "SV1274_PRIVATE_RAR5_PARSER_DETAIL"
  }
}
