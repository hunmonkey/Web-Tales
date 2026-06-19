package org.gotson.komga.shelfview.interfaces.api

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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.gotson.komga.domain.model.MediaType as KomgaMediaType

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@TestPropertySource(
  properties = [
    "shelfview.text.max-bytes=128",
    "shelfview.text.max-line-length=64",
  ],
)
class ShelfViewTextControllerTest(
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val mediaRepository: MediaRepository,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val mockMvc: MockMvc,
) {
  private val library = makeLibrary(id = "shelfview-text-library")

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
  fun `given utf8 txt book when getting text then returns normalized plain text paragraphs`(
    @TempDir dir: Path,
  ) {
    val file = dir.resolve("story.txt")
    Files.writeString(
      file,
      "First paragraph\r\n\r\n<script>alert(\"x\")</script>\r\nSecond line",
      StandardCharsets.UTF_8,
    )
    val book = createBookWithMedia(file)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/text")
      .andExpect {
        status { isOk() }
        jsonPath("$.schemaVersion") { value(1) }
        jsonPath("$.bookId") { value(book.id) }
        jsonPath("$.contentFingerprint") { value("komga-file-hash:$TXT_HASH") }
        jsonPath("$.encoding") { value("utf-8") }
        jsonPath("$.normalizationVersion") { value(1) }
        jsonPath("$.paragraphs[0]") { value("First paragraph") }
        jsonPath("$.paragraphs[1]") { value("<script>alert(\"x\")</script>\nSecond line") }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given utf8 bom txt book when getting text then strips bom`(
    @TempDir dir: Path,
  ) {
    val file = dir.resolve("bom.txt")
    Files.write(
      file,
      byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "BOM text".toByteArray(StandardCharsets.UTF_8),
    )
    val book = createBookWithMedia(file)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/text")
      .andExpect {
        status { isOk() }
        jsonPath("$.encoding") { value("utf-8") }
        jsonPath("$.paragraphs[0]") { value("BOM text") }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given euc kr txt book when getting text with requested encoding then decodes text`(
    @TempDir dir: Path,
  ) {
    val file = dir.resolve("korean.txt")
    Files.write(file, byteArrayOf(0xB0.toByte(), 0xA1.toByte()))
    val book = createBookWithMedia(file)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/text?encoding=euc-kr")
      .andExpect {
        status { isOk() }
        jsonPath("$.encoding") { value("euc-kr") }
        jsonPath("$.paragraphs[0]") { value("\uAC00") }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given unsupported encoding when getting text then returns bad request`(
    @TempDir dir: Path,
  ) {
    val file = dir.resolve("story.txt")
    Files.writeString(file, "Text", StandardCharsets.UTF_8)
    val book = createBookWithMedia(file)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/text?encoding=latin1")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given txt file over size limit when getting text then returns payload too large`(
    @TempDir dir: Path,
  ) {
    val file = dir.resolve("large.txt")
    Files.writeString(file, "a".repeat(129), StandardCharsets.UTF_8)
    val book = createBookWithMedia(file)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/text")
      .andExpect {
        status { isPayloadTooLarge() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given txt line over line limit when getting text then returns payload too large`(
    @TempDir dir: Path,
  ) {
    val file = dir.resolve("long-line.txt")
    Files.writeString(file, "b".repeat(65), StandardCharsets.UTF_8)
    val book = createBookWithMedia(file)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/text")
      .andExpect {
        status { isPayloadTooLarge() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given non txt book when getting text then returns bad request`(
    @TempDir dir: Path,
  ) {
    val file = dir.resolve("book.pdf")
    Files.writeString(file, "Not a text profile", StandardCharsets.UTF_8)
    val book = createBookWithMedia(file, KomgaMediaType.PDF.type)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/text")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser(sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given user without library access when getting text then returns forbidden`(
    @TempDir dir: Path,
  ) {
    val file = dir.resolve("story.txt")
    Files.writeString(file, "Text", StandardCharsets.UTF_8)
    val book = createBookWithMedia(file)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/text")
      .andExpect {
        status { isForbidden() }
      }
  }

  private fun createBookWithMedia(
    file: Path,
    mediaType: String = KomgaMediaType.TXT.type,
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
          fileHash = TXT_HASH,
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

  private companion object {
    const val TXT_HASH = "txt-hash"
  }
}
