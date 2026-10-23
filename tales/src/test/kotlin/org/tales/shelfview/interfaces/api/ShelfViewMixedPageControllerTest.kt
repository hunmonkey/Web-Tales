package org.tales.shelfview.interfaces.api

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.tales.domain.model.Book
import org.tales.domain.model.BookPage
import org.tales.domain.model.Media
import org.tales.domain.model.makeBook
import org.tales.domain.model.makeLibrary
import org.tales.domain.model.makeSeries
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.domain.persistence.SeriesRepository
import org.tales.domain.service.LibraryLifecycle
import org.tales.domain.service.SeriesLifecycle
import org.tales.interfaces.api.rest.WithMockCustomUser
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import org.tales.domain.model.MediaType as TalesMediaType

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@TestPropertySource(
  properties = [
    "shelfview.archive.max-entries=8",
    "shelfview.archive.max-image-bytes=1048576",
    "shelfview.archive.max-image-width=20000",
    "shelfview.archive.max-image-height=20000",
    "shelfview.archive.max-image-pixels=100000000",
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
  @Autowired private val archiveProperties: ShelfViewArchiveProperties,
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
  fun `given mixed zip jpg item when getting page then returns cache headers and not modified on revalidation`(
    @TempDir dir: Path,
  ) {
    val imageBytes = jpegBytes()
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("001.jpg" to imageBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("001.jpg", imageBytes.size.toLong())

    val response =
      mockMvc
        .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
        .andExpect {
          status { isOk() }
          header { exists(HttpHeaders.LAST_MODIFIED) }
          header { string(HttpHeaders.CACHE_CONTROL, "max-age=86400, private") }
        }.andReturn()

    val lastModified = response.response.getHeader(HttpHeaders.LAST_MODIFIED)
    assertThat(lastModified).isNotBlank

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1") {
        header(HttpHeaders.IF_MODIFIED_SINCE, lastModified!!)
      }.andExpect {
        status { isNotModified() }
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
  fun `given a served mixed zip pdf page when getting the next page then the pdf is not parsed again`(
    @TempDir dir: Path,
  ) {
    val pdfBytes = pdfBytes(pageCount = 2)
    val zip = dir.resolve("mixed-pdf-pages.zip")
    writeZip(zip, mapOf("two-pages.pdf" to pdfBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("two-pages.pdf", pdfBytes.size.toLong())

    mockkStatic(Loader::class)
    try {
      var loads = 0
      every { Loader.loadPDF(any<File>()) } answers {
        loads += 1
        callOriginal()
      }

      mockMvc.get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1").andExpect { status { isOk() } }
      val loadsAfterFirstPage = loads
      mockMvc.get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/2").andExpect { status { isOk() } }

      assertThat(loads).isEqualTo(loadsAfterFirstPage)
    } finally {
      unmockkStatic(Loader::class)
    }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip pdf render exceeds output limit when getting page then returns payload too large`(
    @TempDir dir: Path,
  ) {
    val pdfBytes = pdfBytes()
    val zip = dir.resolve("mixed-pdf-output-limit.zip")
    writeZip(zip, mapOf("limited.pdf" to pdfBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("limited.pdf", pdfBytes.size.toLong())
    val previousLimit = archiveProperties.maxImageBytes

    try {
      archiveProperties.maxImageBytes = 1
      mockMvc
        .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
        .andExpect {
          status { isPayloadTooLarge() }
          jsonPath("$.code") { value("SHELFVIEW_IMAGE_TOO_LARGE") }
        }
    } finally {
      archiveProperties.maxImageBytes = previousLimit
    }
  }

  @Test
  @WithMockCustomUser
  fun `given real oversized zip page when authenticated user requests legacy page then returns safe payload too large`(
    @TempDir dir: Path,
  ) {
    val entryBytes = ByteArray((archiveProperties.maxImageBytes + 1).toInt()) { index -> index.toByte() }
    val zip = dir.resolve("legacy-page-limit.zip")
    writeZip(zip, mapOf("private-entry.png" to entryBytes))
    val book = createBookWithMedia(zip)
    mediaRepository.findById(book.id).let { media ->
      mediaRepository.update(
        media.copy(
          pageCount = 1,
          pages = listOf(BookPage("private-entry.png", "image/png")),
        ),
      )
    }

    val response =
      mockMvc
        .get("/api/v1/books/${book.id}/pages/1") {
          accept(MediaType.APPLICATION_JSON)
        }.andExpect {
          status { isPayloadTooLarge() }
          jsonPath("$.code") { value("SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE") }
        }.andReturn()
        .response.contentAsString

    assertThat(response).isEqualTo("{\"code\":\"SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE\"}")
    assertThat(response).doesNotContain(zip.toString(), "private-entry.png")
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip nested cbz image chapter when getting page then returns sorted image bytes`(
    @TempDir dir: Path,
  ) {
    val firstPageBytes = pngBytes(color = Color.WHITE)
    val secondPageBytes = pngBytes(color = Color.BLACK)
    val chapterBytes =
      zipBytes(
        mapOf(
          "images/002.png" to secondPageBytes,
          "images/001.png" to firstPageBytes,
        ),
      )
    val zip = dir.resolve("mixed-nested.zip")
    writeZip(zip, mapOf("ch01.cbz" to chapterBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("ch01.cbz", chapterBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
      .andExpect {
        status { isOk() }
        header { string(HttpHeaders.CONTENT_TYPE, "image/png") }
        content { bytes(firstPageBytes) }
      }

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/2")
      .andExpect {
        status { isOk() }
        header { string(HttpHeaders.CONTENT_TYPE, "image/png") }
        content { bytes(secondPageBytes) }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip nested cbz page number over chapter count when getting page then returns bad request`(
    @TempDir dir: Path,
  ) {
    val chapterBytes = zipBytes(mapOf("images/001.png" to pngBytes()))
    val zip = dir.resolve("mixed-nested.zip")
    writeZip(zip, mapOf("ch01.cbz" to chapterBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("ch01.cbz", chapterBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/2")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip nested cbz with non image entry when getting page then returns bad request`(
    @TempDir dir: Path,
  ) {
    val chapterBytes = zipBytes(mapOf("chapter.txt" to "not an image".toByteArray(StandardCharsets.UTF_8)))
    val zip = dir.resolve("mixed-nested.zip")
    writeZip(zip, mapOf("ch01.cbz" to chapterBytes))
    val book = createBookWithMedia(zip)
    val itemId = mixedItemId("ch01.cbz", chapterBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
      .andExpect {
        status { isBadRequest() }
      }
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
    val previousLimit = archiveProperties.maxImageWidth

    try {
      archiveProperties.maxImageWidth = 64
      mockMvc
        .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/pages/1")
        .andExpect {
          status { isPayloadTooLarge() }
        }
    } finally {
      archiveProperties.maxImageWidth = previousLimit
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
    val book = createBookWithMedia(zip, TalesMediaType.PDF.type)

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
    mediaType: String = TalesMediaType.ZIP.type,
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

  private fun zipBytes(entries: Map<String, ByteArray>): ByteArray =
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

  private fun pngBytes(
    width: Int = 2,
    height: Int = 2,
    color: Color = Color.WHITE,
  ): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    try {
      graphics.color = color
      graphics.fillRect(0, 0, width, height)
    } finally {
      graphics.dispose()
    }

    return ByteArrayOutputStream().use { output ->
      ImageIO.write(image, "PNG", output)
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
    const val CONTENT_FINGERPRINT = "tales-file-hash:$ZIP_HASH"
  }
}
