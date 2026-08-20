package org.tales.shelfview.interfaces.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.model.Book
import org.tales.domain.model.Media
import org.tales.domain.model.MediaFile
import org.tales.domain.model.TalesUser
import org.tales.domain.model.makeBook
import org.tales.domain.model.makeLibrary
import org.tales.domain.model.makeSeries
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.domain.persistence.SeriesRepository
import org.tales.domain.persistence.TalesUserRepository
import org.tales.domain.service.LibraryLifecycle
import org.tales.domain.service.SeriesLifecycle
import org.tales.interfaces.api.persistence.BookDtoRepository
import org.tales.interfaces.api.rest.WithMockCustomUser
import org.tales.shelfview.infrastructure.jooq.ShelfViewReaderStateDao
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.tales.domain.model.MediaType as TalesMediaType

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ShelfViewMixedLocatorBindingTest(
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val mediaRepository: MediaRepository,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val bookDtoRepository: BookDtoRepository,
  @Autowired private val userRepository: TalesUserRepository,
  @Autowired private val readerStateDao: ShelfViewReaderStateDao,
  @Autowired private val locatorValidator: ShelfViewLocatorValidator,
  @Autowired private val mapper: ObjectMapper,
  @Autowired private val mockMvc: MockMvc,
) {
  private val library = makeLibrary(id = "shelfview-mixed-locator-library")
  private val user = TalesUser("shelfview-mixed-locator@example.org", "", id = USER_ID)

  @BeforeAll
  fun setup() {
    libraryRepository.insert(library)
    userRepository.insert(user)
  }

  @AfterEach
  fun clearRepository() {
    readerStateDao.deleteAllPositions()
    readerStateDao.deleteAllBookmarks()
    readerStateDao.deleteAllHighlights()
    seriesLifecycle.deleteMany(seriesRepository.findAll())
  }

  @AfterAll
  fun teardown() {
    userRepository.delete(USER_ID)
    libraryRepository.findAll().forEach {
      libraryLifecycle.deleteLibrary(it)
    }
  }

  @Test
  @WithMockCustomUser(id = USER_ID)
  fun `given current mixed item when saving reader state then all state types are persisted`(
    @TempDir dir: Path,
  ) {
    val fixture = createMixedBook(dir)
    val locator = mixedTextLocatorJson(fixture.itemId, fixture.itemContentFingerprint)

    mockMvc
      .put("/api/shelfview/v1/books/${fixture.book.id}/position") {
        contentType = MediaType.APPLICATION_JSON
        content = positionJson(locator)
      }.andExpect {
        status { isOk() }
        jsonPath("$.locator.itemId") { value(fixture.itemId) }
        jsonPath("$.locator.itemLocator.contentFingerprint") { value(fixture.itemContentFingerprint) }
      }

    mockMvc
      .post("/api/shelfview/v1/books/${fixture.book.id}/bookmarks") {
        contentType = MediaType.APPLICATION_JSON
        content = bookmarkJson(locator)
      }.andExpect {
        status { isCreated() }
        jsonPath("$.locator.itemId") { value(fixture.itemId) }
      }

    mockMvc
      .post("/api/shelfview/v1/books/${fixture.book.id}/highlights") {
        contentType = MediaType.APPLICATION_JSON
        content = highlightJson(locator, fixture.itemId)
      }.andExpect {
        status { isCreated() }
        jsonPath("$.locator.itemId") { value(fixture.itemId) }
        jsonPath("$.selector.itemId") { value(fixture.itemId) }
      }

    assertThat(readerStateDao.findPositionOrNull(fixture.book.id, USER_ID)).isNotNull
    assertThat(readerStateDao.findBookmarks(fixture.book.id, USER_ID)).hasSize(1)
    assertThat(readerStateDao.findHighlights(fixture.book.id, USER_ID)).hasSize(1)
  }

  @Test
  @WithMockCustomUser(id = USER_ID)
  fun `given nonexistent mixed item when saving reader state then all state types reject it`(
    @TempDir dir: Path,
  ) {
    val fixture = createMixedBook(dir)
    val nonexistentItemId = "A".repeat(fixture.itemId.length)

    assertRejectedByAllStateEndpoints(
      fixture = fixture,
      itemId = nonexistentItemId,
      itemContentFingerprint = ShelfViewZipMixedItemIds.itemContentFingerprint(CONTENT_FINGERPRINT, nonexistentItemId),
    )
  }

  @Test
  @WithMockCustomUser(id = USER_ID)
  fun `given stale mixed item fingerprint when saving reader state then all state types reject it`(
    @TempDir dir: Path,
  ) {
    val fixture = createMixedBook(dir)

    assertRejectedByAllStateEndpoints(
      fixture = fixture,
      itemId = fixture.itemId,
      itemContentFingerprint = "$CONTENT_FINGERPRINT:zip-item:stale",
    )
  }

  @Test
  @WithMockCustomUser(id = USER_ID)
  fun `given forged mixed item index when saving reader state then all state types reject it`(
    @TempDir dir: Path,
  ) {
    val fixture = createMixedBook(dir)

    assertRejectedByAllStateEndpoints(
      fixture = fixture,
      itemId = fixture.itemId,
      itemContentFingerprint = fixture.itemContentFingerprint,
      itemIndex = 1,
    )
  }

  @Test
  @WithMockCustomUser(id = USER_ID)
  fun `given oversized mixed item id when saving reader state then all state types reject it`(
    @TempDir dir: Path,
  ) {
    val fixture = createMixedBook(dir)
    val oversizedItemId = "x".repeat(129)

    assertRejectedByAllStateEndpoints(
      fixture = fixture,
      itemId = oversizedItemId,
      itemContentFingerprint = ShelfViewZipMixedItemIds.itemContentFingerprint(CONTENT_FINGERPRINT, oversizedItemId),
    )
  }

  @Test
  @WithMockCustomUser(id = USER_ID)
  fun `given oversized mixed item id when validating locator then length bound rejects it before manifest lookup`(
    @TempDir dir: Path,
  ) {
    val fixture = createMixedBook(dir)
    val book = requireNotNull(bookDtoRepository.findByIdOrNull(fixture.book.id, USER_ID))
    val media = mediaRepository.findById(fixture.book.id)
    val oversizedItemId = "x".repeat(129)
    val locator = mapper.readTree(mixedTextLocatorJson(oversizedItemId, fixture.itemContentFingerprint))

    val exception =
      catchThrowableOfType(ResponseStatusException::class.java) {
        locatorValidator.validate(locator, book, media)
      }

    assertThat(exception.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(exception.reason).isEqualTo("SHELFVIEW_INVALID_LOCATOR: itemId must be 128 characters or fewer")
  }

  @Test
  @WithMockCustomUser(id = USER_ID)
  fun `given mixed highlight selector for another item when saving highlight then it is rejected`(
    @TempDir dir: Path,
  ) {
    val fixture = createMixedBook(dir)
    val locator = mixedTextLocatorJson(fixture.itemId, fixture.itemContentFingerprint)

    mockMvc
      .post("/api/shelfview/v1/books/${fixture.book.id}/highlights") {
        contentType = MediaType.APPLICATION_JSON
        content = highlightJson(locator, "another-item")
      }.andExpect {
        status { isBadRequest() }
      }

    assertThat(readerStateDao.findHighlights(fixture.book.id, USER_ID)).isEmpty()
  }

  private fun assertRejectedByAllStateEndpoints(
    fixture: MixedBookFixture,
    itemId: String,
    itemContentFingerprint: String,
    itemIndex: Int = 0,
  ) {
    val locator = mixedTextLocatorJson(itemId, itemContentFingerprint, itemIndex)

    mockMvc
      .put("/api/shelfview/v1/books/${fixture.book.id}/position") {
        contentType = MediaType.APPLICATION_JSON
        content = positionJson(locator)
      }.andExpect {
        status { isBadRequest() }
      }

    mockMvc
      .post("/api/shelfview/v1/books/${fixture.book.id}/bookmarks") {
        contentType = MediaType.APPLICATION_JSON
        content = bookmarkJson(locator)
      }.andExpect {
        status { isBadRequest() }
      }

    mockMvc
      .post("/api/shelfview/v1/books/${fixture.book.id}/highlights") {
        contentType = MediaType.APPLICATION_JSON
        content = highlightJson(locator, itemId, itemIndex)
      }.andExpect {
        status { isBadRequest() }
      }

    assertThat(readerStateDao.findPositionOrNull(fixture.book.id, USER_ID)).isNull()
    assertThat(readerStateDao.findBookmarks(fixture.book.id, USER_ID)).isEmpty()
    assertThat(readerStateDao.findHighlights(fixture.book.id, USER_ID)).isEmpty()
  }

  private fun createMixedBook(dir: Path): MixedBookFixture {
    val textBytes = "Current chapter".toByteArray(StandardCharsets.UTF_8)
    val zip = dir.resolve("mixed-locator.zip")
    ZipOutputStream(Files.newOutputStream(zip)).use { output ->
      output.putNextEntry(ZipEntry(ENTRY_NAME))
      output.write(textBytes)
      output.closeEntry()
    }

    val createdSeries = seriesLifecycle.createSeries(makeSeries(name = "series", libraryId = library.id))
    seriesLifecycle.addBooks(
      createdSeries,
      listOf(makeBook("mixed-locator.zip", libraryId = library.id, url = zip.toUri().toURL())),
    )

    val book =
      bookRepository
        .findAll()
        .first { it.libraryId == library.id }
        .copy(
          url = zip.toUri().toURL(),
          fileSize = Files.size(zip),
          fileHash = BOOK_HASH,
        )
    bookRepository.update(book)
    mediaRepository.findById(book.id).let { media ->
      mediaRepository.update(
        media.copy(
          status = Media.Status.READY,
          mediaType = TalesMediaType.ZIP.type,
          pageCount = 1,
          files = listOf(MediaFile(ENTRY_NAME, "text/plain", fileSize = textBytes.size.toLong())),
        ),
      )
    }

    val itemId =
      ShelfViewZipMixedItemIds.itemId(
        bookContentFingerprint = CONTENT_FINGERPRINT,
        normalizedEntryName = ENTRY_NAME,
        fileSize = textBytes.size.toLong(),
      )
    return MixedBookFixture(
      book = book,
      itemId = itemId,
      itemContentFingerprint = ShelfViewZipMixedItemIds.itemContentFingerprint(CONTENT_FINGERPRINT, itemId),
    )
  }

  private fun mixedTextLocatorJson(
    itemId: String,
    itemContentFingerprint: String,
    itemIndex: Int = 0,
  ) = """
    {
      "schemaVersion": 1,
      "kind": "MIXED",
      "itemId": "$itemId",
      "itemIndex": $itemIndex,
      "itemLocator": {
        "schemaVersion": 1,
        "kind": "TEXT",
        "paragraph": 0,
        "characterOffset": 0,
        "progression": 0,
        "normalizationVersion": 1,
        "contentFingerprint": "$itemContentFingerprint"
      },
      "contentFingerprint": "$CONTENT_FINGERPRINT"
    }
    """.trimIndent()

  private fun positionJson(locator: String) =
    """
    {
      "locator": $locator,
      "progression": 0.5
    }
    """.trimIndent()

  private fun bookmarkJson(locator: String) =
    """
    {
      "locator": $locator,
      "label": "Mixed chapter",
      "note": null
    }
    """.trimIndent()

  private fun highlightJson(
    locator: String,
    itemId: String,
    itemIndex: Int = 0,
  ) = """
    {
      "locator": $locator,
      "selector": {
        "schemaVersion": 1,
        "kind": "MIXED_TEXT_RANGE",
        "itemId": "$itemId",
        "itemIndex": $itemIndex,
        "itemSelector": {
          "schemaVersion": 1,
          "kind": "TEXT_RANGE",
          "startParagraph": 0,
          "startCharacterOffset": 0,
          "endParagraph": 0,
          "endCharacterOffset": 7,
          "normalizationVersion": 1
        }
      },
      "color": "YELLOW",
      "note": null,
      "textQuote": "Current"
    }
    """.trimIndent()

  private data class MixedBookFixture(
    val book: Book,
    val itemId: String,
    val itemContentFingerprint: String,
  )

  private companion object {
    const val USER_ID = "shelfview-mixed-locator-user"
    const val BOOK_HASH = "mixed-locator-hash"
    const val CONTENT_FINGERPRINT = "tales-file-hash:$BOOK_HASH"
    const val ENTRY_NAME = "chapter.txt"
  }
}
