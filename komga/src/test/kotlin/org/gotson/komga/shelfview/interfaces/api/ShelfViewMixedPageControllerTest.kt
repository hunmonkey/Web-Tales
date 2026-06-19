package org.gotson.komga.shelfview.interfaces.api

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.makeBook
import org.gotson.komga.domain.model.makeLibrary
import org.gotson.komga.domain.model.makeSeries
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.domain.service.LibraryLifecycle
import org.gotson.komga.domain.service.SeriesLifecycle
import org.gotson.komga.interfaces.api.rest.WithMockCustomUser
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import org.gotson.komga.domain.model.MediaType as KomgaMediaType

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@TestPropertySource(
  properties = [
    "shelfview.archive.max-entries=8",
    "shelfview.archive.max-image-bytes=2048",
    "shelfview.archive.max-image-width=64",
    "shelfview.archive.max-image-height=64",
    "shelfview.archive.max-image-pixels=4096",
    "shelfview.archive.max-pdf-bytes=65536",
    "shelfview.archive.max-pdf-pages=2",
  ],
)
class ShelfViewMixedPageControllerTest(
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val mediaRepository: MediaRepository,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val mockMvc: MockMvc,
) {
  private val library = makeLibrary(id = "shelfview-mixed-page-library")

  @BeforeAll
  fun `setup library`() {
    libraryRepository.insert(library)
  }

  @AfterEach
  fun `clear repository`() {
    seriesLifecycle.deleteMany(seriesRepository.findAll())
  }

  @AfterAll
  fun teardown() {
    libraryRepository.findAll().forEach {
      libraryLifecycle.deleteLibrary(it)
    }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip jpg item when getting page then returns image bytes`(
    @TempDir dir: Path,
  ) {
    val imageBytes = jpegBytes()
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("001.jpg" to imageBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("001.jpg", imageBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
      .andExpect {
        status { isOk() }
        header { string(HttpHeaders.CONTENT_TYPE, "image/jpeg") }
        content { bytes(imageBytes) }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip gif item when getting page then preserves original gif bytes`(
    @TempDir dir: Path,
  ) {
    val gifBytes = onePixelGifBytes()
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("002.gif" to gifBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("002.gif", gifBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
      .andExpect {
        status { isOk() }
        header { string(HttpHeaders.CONTENT_TYPE, "image/gif") }
        content { bytes(gifBytes) }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip pdf item when getting page then renders page image`(
    @TempDir dir: Path,
  ) {
    val pdfBytes = pdfBytes()
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("bonus.pdf" to pdfBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("bonus.pdf", pdfBytes.size.toLong())

    val response =
      mockMvc
        .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
        .andExpect {
          status { isOk() }
          header { string(HttpHeaders.CONTENT_TYPE, "image/jpeg") }
        }.andReturn()

    assertThat(response.response.contentAsByteArray).isNotEmpty
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip image page number over one when getting page then returns bad request`(
    @TempDir dir: Path,
  ) {
    val imageBytes = jpegBytes()
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("001.jpg" to imageBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("001.jpg", imageBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/2")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip image with wrong mime when getting page then returns bad request`(
    @TempDir dir: Path,
  ) {
    val fakeJpeg = "not an image".toByteArray(StandardCharsets.UTF_8)
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("001.jpg" to fakeJpeg))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("001.jpg", fakeJpeg.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip image over dimension limit when getting page then returns payload too large`(
    @TempDir dir: Path,
  ) {
    val imageBytes = jpegBytes(width = 65, height = 1)
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("wide.jpg" to imageBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("wide.jpg", imageBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
      .andExpect {
        status { isPayloadTooLarge() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip pdf over page limit when getting page then returns payload too large`(
    @TempDir dir: Path,
  ) {
    val pdfBytes = pdfBytes(pageCount = 3)
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("large.pdf" to pdfBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("large.pdf", pdfBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
      .andExpect {
        status { isPayloadTooLarge() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip traversal entry when getting page then returns bad request`(
    @TempDir dir: Path,
  ) {
    val imageBytes = jpegBytes()
    val zip = dir.resolve("traversal.zip")
    writeZip(zip, mapOf("../evil.jpg" to imageBytes))
    val book = createBookWithMedia(zip)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/not-found/pages/1")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given unknown mixed zip page item when getting page then returns not found`(
    @TempDir dir: Path,
  ) {
    val imageBytes = jpegBytes()
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("001.jpg" to imageBytes))
    val book = createBookWithMedia(zip)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/not-found/pages/1")
      .andExpect {
        status { isNotFound() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given non zip book when getting mixed page then returns bad request`(
    @TempDir dir: Path,
  ) {
    val imageBytes = jpegBytes()
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("001.jpg" to imageBytes))
    val book = createBookWithMedia(zip, KomgaMediaType.PDF.type)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/not-found/pages/1")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser(sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given user without library access when getting mixed page then returns forbidden`(
    @TempDir dir: Path,
  ) {
    val imageBytes = jpegBytes()
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("001.jpg" to imageBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("001.jpg", imageBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
      .andExpect {
        status { isForbidden() }
      }
  }

  private fun createBookWithMedia(
    file: Path,
    mediaType: String = KomgaMediaType.ZIP.type,
  ): Book {
    val createdSeries = seriesLifecycle.createSeries(makeSeries(name = "series", libraryId = library.id))
    seriesLifecycle.addBooks(
      createdSeries,
      listOf(
        makeBook(
          file.fileName.toString(),
          libraryId = library.id,
          url = file.toUri().toURL(),
        ),
      ),
    )

    val book =
      bookRepository
        .findAll()
        .first { it.libraryId == library.id && it.name == file.fileName.toString() }
        .copy(
          url = file.toUri().toURL(),
          fileSize = Files.size(file),
          fileHash = ZIP_HASH,
        )
    bookRepository.update(book)

    mediaRepository.findById(book.id).let {
      mediaRepository.update(
        it.copy(
          status = Media.Status.READY,
          mediaType = mediaType,
          pageCount = 0,
          pages = emptyList(),
        ),
      )
    }

    return book
  }

  private fun writeZip(
    path: Path,
    entries: Map<String, ByteArray>,
  ) {
    ZipOutputStream(Files.newOutputStream(path)).use { zip ->
      entries.forEach { (name, bytes) ->
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
      }
    }
  }

  private fun mixedItemId(
    normalizedEntryName: String,
    fileSize: Long,
  ): String =
    ShelfViewZipMixedItemIds.itemId(
      bookContentFingerprint = CONTENT_FINGERPRINT,
      normalizedEntryName = normalizedEntryName,
      fileSize = fileSize,
    )

  private fun jpegBytes(
    width: Int = 2,
    height: Int = 2,
  ): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    try {
      graphics.color = Color.WHITE
      graphics.fillRect(0, 0, width, height)
    } finally {
      graphics.dispose()
    }

    return ByteArrayOutputStream().use { output ->
      ImageIO.write(image, "JPEG", output)
      output.toByteArray()
    }
  }

  private fun onePixelGifBytes(): ByteArray = Base64.getDecoder().decode("R0lGODlhAQABAPAAAP///wAAACH5BAAAAAAALAAAAAABAAEAAAICRAEAOw==")

  private fun pdfBytes(pageCount: Int = 1): ByteArray =
    ByteArrayOutputStream().use { output ->
      PDDocument().use { document ->
        repeat(pageCount) {
          document.addPage(PDPage())
        }
        document.save(output)
      }
      output.toByteArray()
    }

  private companion object {
    const val ZIP_HASH = "mixed-zip-page-hash"
    const val CONTENT_FINGERPRINT = "komga-file-hash:$ZIP_HASH"
  }
}
