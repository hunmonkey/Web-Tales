package org.tales.infrastructure.mediacontainer.divina

import org.apache.tika.config.TikaConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.core.io.ClassPathResource
import org.tales.domain.model.Dimension
import org.tales.infrastructure.image.ImageAnalyzer
import org.tales.infrastructure.mediacontainer.ContentDetector
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

class ZipExtractorTest {
  private val contentDetector = ContentDetector(TikaConfig())
  private val imageAnalyzer = ImageAnalyzer()
  private val zipExtractor = ZipExtractor(contentDetector, imageAnalyzer)

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

  private fun pngBytes(
    width: Int,
    height: Int,
  ): ByteArray =
    ByteArrayOutputStream().use { output ->
      val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
      ImageIO.write(image, "png", output)
      output.toByteArray()
    }
}
