package org.tales.infrastructure.mediacontainer.pdf

import org.assertj.core.api.Assertions.assertThat
import org.tales.infrastructure.image.ImageType
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class PdfExtractorTest {
  private val pdfExtractor = PdfExtractor(ImageType.JPEG, 1000F)

  @Test
  fun `given pdf file when getting pages then pages are returned`() {
    val fileResource = ClassPathResource("pdf/tales.pdf")

    val pages = pdfExtractor.getPages(fileResource.file.toPath(), true)

    assertThat(pages).hasSize(1)
    assertThat(pages.first().dimension?.width).isEqualTo(512)
  }
}
