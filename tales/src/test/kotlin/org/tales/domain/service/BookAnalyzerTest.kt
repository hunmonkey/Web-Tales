package org.tales.domain.service

import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.tales.domain.model.Book
import org.tales.domain.model.BookPage
import org.tales.domain.model.BookWithMedia
import org.tales.domain.model.Dimension
import org.tales.domain.model.Media
import org.tales.domain.model.MediaExtensionEpub
import org.tales.domain.model.MediaType
import org.tales.domain.model.makeBook
import org.tales.infrastructure.configuration.TalesProperties
import org.tales.infrastructure.mediacontainer.ContentDetector
import org.tales.infrastructure.mediacontainer.divina.ZIP_NESTED_ENTRY_SEPARATOR
import org.tales.infrastructure.mediacontainer.epub.EpubExtractor
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.toPath

@SpringBootTest
class BookAnalyzerTest(
  @Autowired private val talesProperties: TalesProperties,
) {
  @SpykBean
  private lateinit var bookAnalyzer: BookAnalyzer

  @SpykBean
  private lateinit var epubExtractor: EpubExtractor

  @SpykBean
  private lateinit var contentDetector: ContentDetector

  @AfterEach
  fun afterEach() {
    clearAllMocks()
  }

  @Nested
  inner class ArchiveFormats {
    @Test
    fun `given rar4 archive when analyzing then media status is READY`() {
      val file = ClassPathResource("archives/rar4.rar")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/x-rar-compressed; version=4")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.pages).hasSize(3)
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "rar4-solid.rar", "rar4-encrypted.rar",
      ],
    )
    fun `given rar4 solid or encrypted archive when analyzing then media status is UNSUPPORTED`(fileName: String) {
      val file = ClassPathResource("archives/$fileName")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/x-rar-compressed; version=4")
      assertThat(media.status).isEqualTo(Media.Status.UNSUPPORTED)
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "rar5.rar", "rar5-solid.rar", "rar5-encrypted.rar",
      ],
    )
    fun `given rar5 archive when analyzing then media status is UNSUPPORTED`(fileName: String) {
      val file = ClassPathResource("archives/$fileName")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/x-rar-compressed; version=5")
      assertThat(media.status).isEqualTo(Media.Status.UNSUPPORTED)
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "7zip.7z", "7zip-encrypted.7z",
      ],
    )
    fun `given 7zip archive when analyzing then media status is UNSUPPORTED`(fileName: String) {
      val file = ClassPathResource("archives/$fileName")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/x-7z-compressed")
      assertThat(media.status).isEqualTo(Media.Status.UNSUPPORTED)
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "zip.zip", "zip-bzip2.zip", "zip-copy.zip", "zip-deflate64.zip", "zip-lzma.zip", "zip-ppmd.zip",
      ],
    )
    fun `given zip archive when analyzing then media status is READY`(fileName: String) {
      val file = ClassPathResource("archives/$fileName")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.pages).hasSize(1)
    }

    @Test
    fun `given zip encrypted archive when analyzing then media status is ERROR`() {
      val file = ClassPathResource("archives/zip-encrypted.zip")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/zip")
      assertThat(media.status).isEqualTo(Media.Status.ERROR)
    }

    @Test
    fun `given zip with nested image archives when analyzing then media status is READY with pages`(
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
      val book = Book("book", archive.toUri().toURL(), LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, true)

      assertThat(media.mediaType).isEqualTo("application/zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.pageCount).isEqualTo(3)
      assertThat(media.pages.map { it.fileName })
        .containsExactly(
          "ch01.cbz${ZIP_NESTED_ENTRY_SEPARATOR}images/001.png",
          "ch01.cbz${ZIP_NESTED_ENTRY_SEPARATOR}images/002.png",
          "ch02.cbz${ZIP_NESTED_ENTRY_SEPARATOR}images/001.png",
        )
      assertThat(media.pages.map { it.dimension }).containsExactly(Dimension(10, 12), Dimension(11, 13), Dimension(12, 14))
      assertThat(media.files).isEmpty()
      assertThat(bookAnalyzer.getPageContent(BookWithMedia(book, media), 2)).isEqualTo(secondPage)
    }

    @Test
    fun `given zip with nested non image archive when analyzing then media status is ERROR with no pages`(
      @TempDir directory: Path,
    ) {
      val archive = directory.resolve("nested-text.zip")
      writeZip(archive, "ch01.zip" to zipBytes("chapter.txt" to "hello".toByteArray()))
      val book = Book("book", archive.toUri().toURL(), LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/zip")
      assertThat(media.status).isEqualTo(Media.Status.ERROR)
      assertThat(media.comment).isEqualTo("ERR_1006")
      assertThat(media.pages).isEmpty()
    }

    @Test
    fun `given epub archive when analyzing then media status is READY`() {
      val file = ClassPathResource("archives/epub3.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
    }

    @Test
    fun `given txt file when analyzing then media status is READY with text-plain profile and no pages`() {
      val file = ClassPathResource("txt/simple.txt")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("text/plain")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.pageCount).isEqualTo(0)
    }

    @Test
    fun `given txt file detected as octet-stream when analyzing then media status is READY with text-plain profile`(
      @TempDir directory: Path,
    ) {
      val file = directory.resolve("목항 01권.txt")
      Files.writeString(file, "첫 문장")
      val book = Book("book", file.toUri().toURL(), LocalDateTime.now())
      every { contentDetector.detectMediaType(file) } returns "application/octet-stream"

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("text/plain")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.pageCount).isEqualTo(0)
    }
  }

  @Nested
  inner class ImageDirectory {
    @Test
    fun `given marked image directory when analyzing then pages are naturally sorted and validated`(
      @TempDir directory: Path,
    ) {
      Files.createFile(directory.resolve(SHELFVIEW_IMAGE_DIRECTORY_MARKER))
      writeImage(directory.resolve("10.png"), "png", 10, 20)
      writeImage(directory.resolve("2.jpg"), "jpg", 2, 3)
      Files.writeString(directory.resolve("3.jpg"), "not an image")
      Files.writeString(directory.resolve("notes.txt"), "hello")

      val book = Book("book", directory.toUri().toURL(), LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, true)

      assertThat(media.mediaType).isEqualTo(MediaType.IMAGE_DIRECTORY.type)
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.pageCount).isEqualTo(2)
      assertThat(media.pages.map { it.fileName }).containsExactly("2.jpg", "10.png")
      assertThat(media.pages.map { it.mediaType }).containsExactly("image/jpeg", "image/png")
      assertThat(media.pages.map { it.dimension }).containsExactly(Dimension(2, 3), Dimension(10, 20))
      assertThat(media.pages.map { it.fileSize }).allMatch { it != null && it > 0 }
      assertThat(media.files.map { it.fileName }).containsExactly("3.jpg", "notes.txt")
      assertThat(media.comment).contains("ERR_1007 [3.jpg]")
    }

    @Test
    fun `given marked image directory without image pages when analyzing then media status is ERROR`(
      @TempDir directory: Path,
    ) {
      Files.createFile(directory.resolve(SHELFVIEW_IMAGE_DIRECTORY_MARKER))
      Files.writeString(directory.resolve("notes.txt"), "hello")

      val book = Book("book", directory.toUri().toURL(), LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, true)

      assertThat(media.mediaType).isEqualTo(MediaType.IMAGE_DIRECTORY.type)
      assertThat(media.status).isEqualTo(Media.Status.ERROR)
      assertThat(media.comment).isEqualTo("ERR_1006")
    }
  }

  @Nested
  inner class Epub {
    @Test
    fun `given broken epub archive when analyzing then media status is ERROR`() {
      val file = ClassPathResource("archives/zip-as-epub.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/zip")
      assertThat(media.status).isEqualTo(Media.Status.ERROR)
      assertThat(media.pages).hasSize(0)
    }

    @Test
    fun `given regular epub archive when analyzing then comment is empty`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).isNull()
    }

    @Test
    fun `given epub archive when toc cannot be extracted then media status is READY with comments`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      every { epubExtractor.getToc(any()) } throws Exception("mock exception")

      val media = bookAnalyzer.analyze(book, false)
      val extension = media.extension as? MediaExtensionEpub

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).contains("ERR_1035")
      assertThat(extension).isNotNull
      assertThat(extension!!.toc).isEmpty()
    }

    @Test
    fun `given epub archive when landmarks cannot be extracted then media status is READY with comments`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      every { epubExtractor.getLandmarks(any()) } throws Exception("mock exception")

      val media = bookAnalyzer.analyze(book, false)
      val extension = media.extension as? MediaExtensionEpub

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).contains("ERR_1036")
      assertThat(extension).isNotNull
      assertThat(extension!!.landmarks).isEmpty()
    }

    @Test
    fun `given epub archive when page list cannot be extracted then media status is READY with comments`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      every { epubExtractor.getPageList(any()) } throws Exception("mock exception")

      val media = bookAnalyzer.analyze(book, false)
      val extension = media.extension as? MediaExtensionEpub

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).contains("ERR_1037")
      assertThat(extension).isNotNull
      assertThat(extension!!.pageList).isEmpty()
    }

    @Test
    fun `given epub archive when divina pages cannot be extracted then media status is READY with comments`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      every { epubExtractor.getDivinaPages(any(), any()) } throws Exception("mock exception")

      val media = bookAnalyzer.analyze(book, false)

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).contains("ERR_1038")
      assertThat(media.pages).isEmpty()
    }

    @Test
    fun `given epub archive when positions cannot be extracted then media status is READY with comments`() {
      val file = ClassPathResource("epub/The Incomplete Theft - Ralph Burke.epub")
      val book = Book("book", file.url, LocalDateTime.now())

      every { epubExtractor.computePositions(any(), any()) } throws Exception("mock exception")

      val media = bookAnalyzer.analyze(book, false)
      val extension = media.extension as? MediaExtensionEpub

      assertThat(media.mediaType).isEqualTo("application/epub+zip")
      assertThat(media.status).isEqualTo(Media.Status.READY)
      assertThat(media.comment).contains("ERR_1039")
      assertThat(extension).isNotNull
      assertThat(extension!!.positions).isEmpty()
    }
  }

  @Nested
  inner class PageHashing {
    @Test
    fun `given book with a single page when hashing then all pages are hashed`() {
      val book = makeBook("book1")
      val pages = listOf(BookPage("1.jpeg", "image/jpeg"))
      val media = Media(Media.Status.READY, pages = pages)

      every { bookAnalyzer.getPageContent(any(), any()) } returns ByteArray(1)
      every { bookAnalyzer.hashPage(any(), any()) } returns "hashed"

      val hashedMedia = bookAnalyzer.hashPages(BookWithMedia(book, media))

      assertThat(hashedMedia.pages).hasSize(1)
      assertThat(hashedMedia.pages.first().fileHash).isEqualTo("hashed")
    }

    @Test
    fun `given book with more than 6 pages when hashing then only first and last 3 are hashed`() {
      val book = makeBook("book1")
      val pages = (1..30).map { BookPage("$it.jpeg", "image/jpeg") }
      val media = Media(Media.Status.READY, pages = pages)

      every { bookAnalyzer.getPageContent(any(), any()) } returns ByteArray(1)
      every { bookAnalyzer.hashPage(any(), any()) } returns "hashed"

      val hashedMedia = bookAnalyzer.hashPages(BookWithMedia(book, media))

      assertThat(hashedMedia.pages).hasSize(30)
      assertThat(hashedMedia.pages.take(talesProperties.pageHashing).map { it.fileHash })
        .hasSize(talesProperties.pageHashing)
        .containsOnly("hashed")
      assertThat(hashedMedia.pages.takeLast(talesProperties.pageHashing).map { it.fileHash })
        .hasSize(talesProperties.pageHashing)
        .containsOnly("hashed")
      assertThat(
        hashedMedia.pages
          .drop(talesProperties.pageHashing)
          .dropLast(talesProperties.pageHashing)
          .map { it.fileHash },
      ).hasSize(30 - (talesProperties.pageHashing * 2))
        .containsOnly("")
    }

    @Test
    fun `given book with already hashed pages when hashing then no hashing is done`() {
      val book = makeBook("book1")
      val pages = (1..30).map { BookPage("$it.jpeg", "image/jpeg", fileHash = "hashed") }
      val media = Media(Media.Status.READY, pages = pages)

      val hashedMedia = bookAnalyzer.hashPages(BookWithMedia(book, media))

      verify(exactly = 0) { bookAnalyzer.getPageContent(any(), any()) }
      verify(exactly = 0) { bookAnalyzer.hashPage(any(), any()) }

      assertThat(hashedMedia.pages.map { it.fileHash })
        .hasSize(30)
        .containsOnly("hashed")
    }

    @ParameterizedTest
    @MethodSource("provideDirectoriesForPageHashing")
    fun `given 2 exact pages when hashing then hashes are the same`(directory: Path) {
      val files = directory.listDirectoryEntries()
      assertThat(files).hasSize(2)

      val mediaType = "image/${directory.fileName.extension}"

      val hashes =
        files.map {
          bookAnalyzer.hashPage(BookPage(it.name, mediaType = mediaType), it.inputStream().readBytes())
        }

      assertThat(hashes.first()).isEqualTo(hashes.last())
    }

    private fun provideDirectoriesForPageHashing() = ClassPathResource("hashpage").uri.toPath().listDirectoryEntries()
  }

  private fun writeImage(
    path: Path,
    format: String,
    width: Int,
    height: Int,
  ) {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    ImageIO.write(image, format, path.toFile())
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
