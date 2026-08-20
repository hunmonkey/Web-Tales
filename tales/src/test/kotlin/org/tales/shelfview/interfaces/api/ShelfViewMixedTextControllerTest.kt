package org.tales.shelfview.interfaces.api

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.tales.domain.model.Book
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
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.tales.domain.model.MediaType as TalesMediaType

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@TestPropertySource(
  properties = [
    "shelfview.text.max-bytes=128",
    "shelfview.text.max-line-length=64",
    "shelfview.archive.max-entries=8",
  ],
)
class ShelfViewMixedTextControllerTest(
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val mediaRepository: MediaRepository,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val mockMvc: MockMvc,
) {
  private val library = makeLibrary(id = "shelfview-mixed-text-library")

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
  fun `given mixed zip text item when getting text then returns normalized plain text paragraphs`(
    @TempDir dir: Path,
  ) {
    val textBytes = "First paragraph\r\n\r\n<script>alert(\"x\")</script>\r\nSecond line".toByteArray(StandardCharsets.UTF_8)
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("chapter.txt" to textBytes, "cover.jpg" to byteArrayOf(1, 2, 3)))
    val book = createBookWithMedia(zip)
    val itemId = textItemId("chapter.txt", textBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/text")
      .andExpect {
        status { isOk() }
        jsonPath("$.schemaVersion") { value(1) }
        jsonPath("$.bookId") { value(book.id) }
        jsonPath("$.contentFingerprint") { value(ShelfViewZipMixedItemIds.itemContentFingerprint(CONTENT_FINGERPRINT, itemId)) }
        jsonPath("$.encoding") { value("utf-8") }
        jsonPath("$.normalizationVersion") { value(1) }
        jsonPath("$.paragraphs[0]") { value("First paragraph") }
        jsonPath("$.paragraphs[1]") { value("<script>alert(\"x\")</script>\nSecond line") }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given euc kr mixed zip text item when getting text with requested encoding then decodes text`(
    @TempDir dir: Path,
  ) {
    val textBytes = byteArrayOf(0xB0.toByte(), 0xA1.toByte())
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("korean.txt" to textBytes))
    val book = createBookWithMedia(zip)
    val itemId = textItemId("korean.txt", textBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/text?encoding=euc-kr")
      .andExpect {
        status { isOk() }
        jsonPath("$.encoding") { value("euc-kr") }
        jsonPath("$.paragraphs[0]") { value("\uAC00") }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip text item over size limit when getting text then returns payload too large`(
    @TempDir dir: Path,
  ) {
    val textBytes = "a".repeat(129).toByteArray(StandardCharsets.UTF_8)
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("large.txt" to textBytes))
    val book = createBookWithMedia(zip)
    val itemId = textItemId("large.txt", textBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/text")
      .andExpect {
        status { isPayloadTooLarge() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip text item with line over limit when getting text then returns payload too large`(
    @TempDir dir: Path,
  ) {
    val textBytes = "b".repeat(65).toByteArray(StandardCharsets.UTF_8)
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("long-line.txt" to textBytes))
    val book = createBookWithMedia(zip)
    val itemId = textItemId("long-line.txt", textBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/text")
      .andExpect {
        status { isPayloadTooLarge() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip over entry count limit when getting text then returns safe payload too large`(
    @TempDir dir: Path,
  ) {
    val textBytes = "selected".toByteArray(StandardCharsets.UTF_8)
    val entries = linkedMapOf("selected.txt" to textBytes)
    repeat(8) { index -> entries["ignored-$index.bin"] = byteArrayOf(index.toByte()) }
    val zip = dir.resolve("too-many-entries.zip")
    writeZip(zip, entries)
    val book = createBookWithMedia(zip)
    val itemId = textItemId("selected.txt", textBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/text")
      .andExpect {
        status { isPayloadTooLarge() }
        jsonPath("$.code") { value("SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES") }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given mixed zip with traversal entry when getting text then returns bad request`(
    @TempDir dir: Path,
  ) {
    val zip = dir.resolve("traversal.zip")
    writeZip(zip, mapOf("../evil.txt" to "bad".toByteArray(StandardCharsets.UTF_8)))
    val book = createBookWithMedia(zip)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/not-found/text")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given unknown mixed zip text item when getting text then returns not found`(
    @TempDir dir: Path,
  ) {
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("chapter.txt" to "text".toByteArray(StandardCharsets.UTF_8)))
    val book = createBookWithMedia(zip)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/not-found/text")
      .andExpect {
        status { isNotFound() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given non zip book when getting mixed text then returns bad request`(
    @TempDir dir: Path,
  ) {
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("chapter.txt" to "text".toByteArray(StandardCharsets.UTF_8)))
    val book = createBookWithMedia(zip, TalesMediaType.TXT.type)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/not-found/text")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser(sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given user without library access when getting mixed text then returns forbidden`(
    @TempDir dir: Path,
  ) {
    val textBytes = "Hidden".toByteArray(StandardCharsets.UTF_8)
    val zip = dir.resolve("mixed.zip")
    writeZip(zip, mapOf("chapter.txt" to textBytes))
    val book = createBookWithMedia(zip)
    val itemId = textItemId("chapter.txt", textBytes.size.toLong())

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/mixed/$itemId/text")
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

  private fun textItemId(
    normalizedEntryName: String,
    fileSize: Long,
  ): String =
    ShelfViewZipMixedItemIds.itemId(
      bookContentFingerprint = CONTENT_FINGERPRINT,
      normalizedEntryName = normalizedEntryName,
      fileSize = fileSize,
    )

  private companion object {
    const val ZIP_HASH = "mixed-zip-hash"
    const val CONTENT_FINGERPRINT = "tales-file-hash:$ZIP_HASH"
  }
}
