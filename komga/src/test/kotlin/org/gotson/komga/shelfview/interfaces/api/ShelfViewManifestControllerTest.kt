package org.gotson.komga.shelfview.interfaces.api

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.gotson.komga.domain.model.BookPage
import org.gotson.komga.domain.model.Dimension
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.MediaFile
import org.gotson.komga.domain.model.MediaType
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ShelfViewManifestControllerTest(
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val mediaRepository: MediaRepository,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val mockMvc: MockMvc,
) {
  private val library = makeLibrary(id = "1")

  @BeforeAll
  fun `setup library`() {
    libraryRepository.insert(library)
  }

  @AfterAll
  fun teardown() {
    libraryRepository.findAll().forEach {
      libraryLifecycle.deleteLibrary(it)
    }
  }

  @AfterEach
  fun `clear repository`() {
    seriesLifecycle.deleteMany(seriesRepository.findAll())
  }

  @Test
  @WithMockCustomUser
  fun `given fixed page book when getting manifest then returns fixed page response`() {
    val book = createBookWithMedia(MediaType.ZIP.type, "Book One.cbz")

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isOk() }
        jsonPath("$.schemaVersion") { value(1) }
        jsonPath("$.bookId") { value(book.id) }
        jsonPath("$.kind") { value("FIXED_PAGE_SEQUENCE") }
        jsonPath("$.sourceFormat") { value("CBZ") }
        jsonPath("$.pageCount") { value(2) }
        jsonPath("$.pages.endpointTemplate") { value("/api/v1/books/${book.id}/pages/{page}?zero_based=true") }
        jsonPath("$.pages.thumbnailEndpointTemplate") { value("/api/v1/books/${book.id}/pages/{pageOneBased}/thumbnail") }
        jsonPath("$.capabilities.search") { value(false) }
        jsonPath("$.capabilities.doublePage") { value(true) }
        jsonPath("$.capabilities.imageFilters") { value(true) }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip book when getting manifest then returns ordered mixed items`(
    @TempDir dir: Path,
  ) {
    val textBytes = "Chapter".toByteArray(StandardCharsets.UTF_8)
    val imageBytes = byteArrayOf(1, 2, 3)
    val pdfBytes = pdfBytes()
    val zip = dir.resolve("mixed.zip")
    writeZip(
      zip,
      mapOf(
        "10.pdf" to pdfBytes,
        "2.jpg" to imageBytes,
        "1.txt" to textBytes,
      ),
    )
    val book =
      createBookWithMedia(
        MediaType.ZIP.type,
        "mixed.zip",
        file = zip,
        files =
          listOf(
            MediaFile("1.txt", "text/plain", fileSize = textBytes.size.toLong()),
            MediaFile("10.pdf", "application/pdf", fileSize = pdfBytes.size.toLong()),
          ),
      )
    val textItemId = mixedItemId("1.txt", textBytes.size.toLong())
    val imageItemId = mixedItemId("2.jpg", imageBytes.size.toLong())
    val pdfItemId = mixedItemId("10.pdf", pdfBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isOk() }
        jsonPath("$.schemaVersion") { value(1) }
        jsonPath("$.bookId") { value(book.id) }
        jsonPath("$.kind") { value("MIXED_CONTENT_SEQUENCE") }
        jsonPath("$.sourceFormat") { value("ZIP") }
        jsonPath("$.contentFingerprint") { value(CONTENT_FINGERPRINT) }
        jsonPath("$.pageCount") { value(3) }
        jsonPath("$.items[0].itemId") { value(textItemId) }
        jsonPath("$.items[0].index") { value(0) }
        jsonPath("$.items[0].kind") { value("TEXT_REFLOW") }
        jsonPath("$.items[0].label") { value("1.txt") }
        jsonPath("$.items[0].sourceFormat") { value("TXT") }
        jsonPath("$.items[0].contentFingerprint") { value(ShelfViewZipMixedItemIds.itemContentFingerprint(CONTENT_FINGERPRINT, textItemId)) }
        jsonPath("$.items[0].text.endpoint") { value("/api/shelfview/v1/books/${book.id}/mixed/$textItemId/text") }
        jsonPath("$.items[1].itemId") { value(imageItemId) }
        jsonPath("$.items[1].index") { value(1) }
        jsonPath("$.items[1].kind") { value("FIXED_PAGE_SEQUENCE") }
        jsonPath("$.items[1].label") { value("2.jpg") }
        jsonPath("$.items[1].sourceFormat") { value("JPG") }
        jsonPath("$.items[1].pageCount") { value(1) }
        jsonPath("$.items[1].pages.endpointTemplate") { value("/api/shelfview/v1/books/${book.id}/mixed/$imageItemId/pages/{page}?zero_based=true") }
        jsonPath("$.items[2].itemId") { value(pdfItemId) }
        jsonPath("$.items[2].index") { value(2) }
        jsonPath("$.items[2].kind") { value("FIXED_PAGE_SEQUENCE") }
        jsonPath("$.items[2].label") { value("10.pdf") }
        jsonPath("$.items[2].sourceFormat") { value("PDF") }
        jsonPath("$.items[2].pageCount") { value(1) }
        jsonPath("$.items[2].pages.endpointTemplate") { value("/api/shelfview/v1/books/${book.id}/mixed/$pdfItemId/pages/{page}?zero_based=true") }
        jsonPath("$.pages") { doesNotExist() }
        jsonPath("$.capabilities.textSelection") { value(true) }
        jsonPath("$.capabilities.search") { value(true) }
        jsonPath("$.capabilities.imageFilters") { value(true) }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given image only zip book when getting manifest then keeps fixed page response`(
    @TempDir dir: Path,
  ) {
    val zip = dir.resolve("images.zip")
    writeZip(zip, mapOf("1.jpg" to byteArrayOf(1, 2, 3)))
    val book = createBookWithMedia(MediaType.ZIP.type, "images.zip", file = zip)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isOk() }
        jsonPath("$.kind") { value("FIXED_PAGE_SEQUENCE") }
        jsonPath("$.sourceFormat") { value("ZIP") }
        jsonPath("$.items") { isEmpty() }
        jsonPath("$.pages.endpointTemplate") { value("/api/v1/books/${book.id}/pages/{page}?zero_based=true") }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given pdf book when getting manifest then returns pdf source format`() {
    val book = createBookWithMedia(MediaType.PDF.type, "Book One.pdf")

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isOk() }
        jsonPath("$.sourceFormat") { value("PDF") }
        jsonPath("$.kind") { value("FIXED_PAGE_SEQUENCE") }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given image directory book when getting manifest then returns image directory page endpoint`(
    @TempDir dir: Path,
  ) {
    val book = createBookWithMedia(MediaType.IMAGE_DIRECTORY.type, "Image Folder", file = dir)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isOk() }
        jsonPath("$.kind") { value("FIXED_PAGE_SEQUENCE") }
        jsonPath("$.sourceFormat") { value("IMAGE_DIRECTORY") }
        jsonPath("$.pageCount") { value(2) }
        jsonPath("$.pages.endpointTemplate") { value("/api/shelfview/v1/books/${book.id}/image-directory/pages/{page}?zero_based=true") }
        jsonPath("$.pages.thumbnailEndpointTemplate") { doesNotExist() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given txt book when getting manifest then returns text response`() {
    val book = createBookWithMedia(MediaType.TXT.type, "Book One.txt")

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isOk() }
        jsonPath("$.schemaVersion") { value(1) }
        jsonPath("$.bookId") { value(book.id) }
        jsonPath("$.kind") { value("TEXT_REFLOW") }
        jsonPath("$.sourceFormat") { value("TXT") }
        jsonPath("$.text.endpoint") { value("/api/shelfview/v1/books/${book.id}/text") }
        jsonPath("$.text.normalizationVersion") { value(1) }
        jsonPath("$.pages") { doesNotExist() }
        jsonPath("$.capabilities.search") { value(true) }
        jsonPath("$.capabilities.textSelection") { value(true) }
        jsonPath("$.capabilities.doublePage") { value(false) }
        jsonPath("$.capabilities.imageFilters") { value(false) }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given epub book when getting manifest then returns bad request`() {
    val book = createBookWithMedia(MediaType.EPUB.type, "Book One.epub")

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser(sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given user without library access when getting manifest then returns forbidden`() {
    val book = createBookWithMedia(MediaType.ZIP.type, "Book One.cbz")

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isForbidden() }
      }
  }

  private fun createBookWithMedia(
    mediaType: String,
    bookName: String,
    file: Path? = null,
    files: List<MediaFile> = emptyList(),
  ) = makeSeries(name = "series", libraryId = library.id).let { series ->
    val created = seriesLifecycle.createSeries(series)
    seriesLifecycle.addBooks(
      created,
      listOf(
        if (file != null) {
          makeBook(bookName, libraryId = library.id, url = file.toUri().toURL())
        } else {
          makeBook(bookName, libraryId = library.id)
        },
      ),
    )

    val book =
      bookRepository
        .findAll()
        .first()
        .let { book ->
          if (file != null) {
            book.copy(
              url = file.toUri().toURL(),
              fileSize = Files.size(file),
              fileHash = ZIP_HASH,
            )
          } else {
            book
          }
        }
    bookRepository.update(book)

    book.also {
      mediaRepository.findById(book.id).let {
        mediaRepository.update(
          it.copy(
            status = Media.Status.READY,
            mediaType = mediaType,
            pages =
              listOf(
                BookPage("001.jpg", "image/jpeg", Dimension(800, 1200), fileSize = 1024),
                BookPage("002.jpg", "image/jpeg", Dimension(800, 1200), fileSize = 2048),
              ),
            files = files,
          ),
        )
      }
    }
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

  private fun pdfBytes(): ByteArray =
    ByteArrayOutputStream().use { output ->
      PDDocument().use { document ->
        document.addPage(PDPage())
        document.save(output)
      }
      output.toByteArray()
    }

  private companion object {
    const val ZIP_HASH = "mixed-zip-manifest-hash"
    const val CONTENT_FINGERPRINT = "komga-file-hash:$ZIP_HASH"
  }
}
