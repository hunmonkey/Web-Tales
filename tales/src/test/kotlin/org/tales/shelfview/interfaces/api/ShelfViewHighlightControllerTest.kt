package org.tales.shelfview.interfaces.api

import org.tales.domain.model.Book
import org.tales.domain.model.BookPage
import org.tales.domain.model.TalesUser
import org.tales.domain.model.Media
import org.tales.domain.model.makeBook
import org.tales.domain.model.makeLibrary
import org.tales.domain.model.makeSeries
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.TalesUserRepository
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.domain.persistence.SeriesRepository
import org.tales.domain.service.LibraryLifecycle
import org.tales.domain.service.SeriesLifecycle
import org.tales.interfaces.api.rest.WithMockCustomUser
import org.tales.shelfview.domain.ShelfViewHighlight
import org.tales.shelfview.infrastructure.jooq.ShelfViewReaderStateDao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.tales.domain.model.MediaType as TalesMediaType

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ShelfViewHighlightControllerTest(
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val mediaRepository: MediaRepository,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val userRepository: TalesUserRepository,
  @Autowired private val readerStateDao: ShelfViewReaderStateDao,
  @Autowired private val mockMvc: MockMvc,
) {
  private val library = makeLibrary(id = "shelfview-highlight-library")
  private val user1 = TalesUser("shelfview-highlight-user1@example.org", "", id = USER_1_ID)
  private val user2 = TalesUser("shelfview-highlight-user2@example.org", "", id = USER_2_ID)

  @BeforeAll
  fun `setup library and users`() {
    libraryRepository.insert(library)
    userRepository.insert(user1)
    userRepository.insert(user2)
  }

  @AfterEach
  fun `clear repository`() {
    readerStateDao.deleteAllHighlights()
    seriesLifecycle.deleteMany(seriesRepository.findAll())
  }

  @AfterAll
  fun teardown() {
    userRepository.delete(USER_1_ID)
    userRepository.delete(USER_2_ID)
    libraryRepository.findAll().forEach {
      libraryLifecycle.deleteLibrary(it)
    }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given no highlights when listing highlights then returns empty list`() {
    val book = createBookWithMedia()

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/highlights")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(0) }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given highlight when creating updating and deleting then highlight changes are persisted`() {
    val book = createBookWithMedia()

    mockMvc
      .post("/api/shelfview/v1/books/${book.id}/highlights") {
        contentType = MediaType.APPLICATION_JSON
        content = highlightJson(color = "yellow", note = " Important  note ", textQuote = " Opening   text ")
      }.andExpect {
        status { isCreated() }
        jsonPath("$.id") { exists() }
        jsonPath("$.bookId") { value(book.id) }
        jsonPath("$.locator.kind") { value("TEXT") }
        jsonPath("$.selector.kind") { value("TEXT_RANGE") }
        jsonPath("$.contentFingerprint") { value(CONTENT_FINGERPRINT) }
        jsonPath("$.color") { value("YELLOW") }
        jsonPath("$.note") { value("Important note") }
        jsonPath("$.textQuote") { value("Opening text") }
        jsonPath("$.createdAt") { exists() }
        jsonPath("$.updatedAt") { exists() }
      }

    val highlightId = readerStateDao.findHighlights(book.id, USER_1_ID).first().id

    mockMvc
      .post("/api/shelfview/v1/books/${book.id}/highlights") {
        contentType = MediaType.APPLICATION_JSON
        content = highlightJson(selector = textRangeSelectorJson(startOffset = 15, endOffset = 28), color = "GREEN")
      }.andExpect {
        status { isCreated() }
      }

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/highlights")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(2) }
        jsonPath("${'$'}[0].color") { value("YELLOW") }
        jsonPath("${'$'}[1].color") { value("GREEN") }
      }

    mockMvc
      .put("/api/shelfview/v1/highlights/$highlightId") {
        contentType = MediaType.APPLICATION_JSON
        content = highlightJson(selector = textRangeSelectorJson(startOffset = 3, endOffset = 18), color = "BLUE", note = null, textQuote = "Updated")
      }.andExpect {
        status { isOk() }
        jsonPath("$.id") { value(highlightId) }
        jsonPath("$.selector.endCharacterOffset") { value(18) }
        jsonPath("$.color") { value("BLUE") }
        jsonPath("$.note") { doesNotExist() }
        jsonPath("$.textQuote") { value("Updated") }
      }

    mockMvc
      .delete("/api/shelfview/v1/highlights/$highlightId")
      .andExpect {
        status { isNoContent() }
      }

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/highlights")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(1) }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given mixed text selector when creating highlight then it can be fetched`() {
    val book = createBookWithMedia()

    mockMvc
      .post("/api/shelfview/v1/books/${book.id}/highlights") {
        contentType = MediaType.APPLICATION_JSON
        content =
          highlightJson(
            locator = mixedTextLocatorJson(),
            selector = mixedTextRangeSelectorJson(),
            color = "PINK",
            textQuote = "Archive text",
          )
      }.andExpect {
        status { isCreated() }
        jsonPath("$.locator.kind") { value("MIXED") }
        jsonPath("$.selector.kind") { value("MIXED_TEXT_RANGE") }
        jsonPath("$.selector.itemId") { value("opaque-item") }
        jsonPath("$.selector.itemSelector.kind") { value("TEXT_RANGE") }
        jsonPath("$.color") { value("PINK") }
      }

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/highlights")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(1) }
        jsonPath("${'$'}[0].selector.kind") { value("MIXED_TEXT_RANGE") }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_2_ID)
  fun `given highlight saved by one user when another user reads or changes highlight then it is isolated`() {
    val book = createBookWithMedia()
    val highlight =
      ShelfViewHighlight(
        id = "highlight-user1",
        userId = USER_1_ID,
        bookId = book.id,
        locatorJson = textLocatorJson(),
        selectorJson = textRangeSelectorJson(),
        contentFingerprint = CONTENT_FINGERPRINT,
        color = "YELLOW",
      )
    readerStateDao.saveHighlight(highlight)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/highlights")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(0) }
      }

    mockMvc
      .put("/api/shelfview/v1/highlights/${highlight.id}") {
        contentType = MediaType.APPLICATION_JSON
        content = highlightJson(color = "BLUE")
      }.andExpect {
        status { isNotFound() }
      }

    mockMvc
      .delete("/api/shelfview/v1/highlights/${highlight.id}")
      .andExpect {
        status { isNotFound() }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given invalid highlight request when creating highlight then returns bad request`() {
    val book = createBookWithMedia()
    val invalidRequests =
      listOf(
        """{"selector":${textRangeSelectorJson()},"color":"YELLOW"}""",
        highlightJson(selector = """{"schemaVersion":2,"kind":"TEXT_RANGE"}"""),
        highlightJson(locator = pageLocatorJson(), selector = textRangeSelectorJson()),
        highlightJson(selector = textRangeSelectorJson(startOffset = -1, endOffset = 8)),
        highlightJson(selector = textRangeSelectorJson(startOffset = 8, endOffset = 8)),
        highlightJson(selector = textRangeSelectorJson(normalizationVersion = 2)),
        highlightJson(locator = mixedTextLocatorJson(), selector = mixedTextRangeSelectorJson(itemId = "other-item")),
        highlightJson(color = "rainbow"),
        highlightJson(textQuote = "x".repeat(501)),
      )

    invalidRequests.forEach { jsonString ->
      mockMvc
        .post("/api/shelfview/v1/books/${book.id}/highlights") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isBadRequest() }
        }
    }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID, sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given user without book access when creating highlight then returns forbidden`() {
    val book = createBookWithMedia()

    mockMvc
      .post("/api/shelfview/v1/books/${book.id}/highlights") {
        contentType = MediaType.APPLICATION_JSON
        content = highlightJson()
      }.andExpect {
        status { isForbidden() }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID, sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given existing highlight without book access when updating highlight then returns forbidden`() {
    val book = createBookWithMedia()
    val highlight =
      ShelfViewHighlight(
        id = "highlight-no-access",
        userId = USER_1_ID,
        bookId = book.id,
        locatorJson = textLocatorJson(),
        selectorJson = textRangeSelectorJson(),
        contentFingerprint = CONTENT_FINGERPRINT,
        color = "YELLOW",
      )
    readerStateDao.saveHighlight(highlight)

    mockMvc
      .put("/api/shelfview/v1/highlights/${highlight.id}") {
        contentType = MediaType.APPLICATION_JSON
        content = highlightJson(color = "BLUE")
      }.andExpect {
        status { isForbidden() }
      }
  }

  private fun createBookWithMedia(): Book {
    val createdSeries = seriesLifecycle.createSeries(makeSeries(name = "series", libraryId = library.id))
    seriesLifecycle.addBooks(createdSeries, listOf(makeBook("Book One.txt", libraryId = library.id)))

    val book = bookRepository.findAll().first { it.libraryId == library.id }.copy(fileHash = "highlight-hash")
    bookRepository.update(book)
    mediaRepository.findById(book.id).let {
      mediaRepository.update(
        it.copy(
          status = Media.Status.READY,
          mediaType = TalesMediaType.TXT.type,
          pageCount = 2,
          pages =
            listOf(
              BookPage("001.txt", "text/plain"),
              BookPage("002.txt", "text/plain"),
            ),
        ),
      )
    }

    return book
  }

  private fun highlightJson(
    locator: String = textLocatorJson(),
    selector: String = textRangeSelectorJson(),
    color: String? = "YELLOW",
    note: String? = "Important",
    textQuote: String? = "Opening text",
  ) = """
    {
      "locator": $locator,
      "selector": $selector,
      "color": ${jsonString(color)},
      "note": ${jsonString(note)},
      "textQuote": ${jsonString(textQuote)}
    }
    """.trimIndent()

  private fun textLocatorJson(paragraph: Int = 0) =
    """
    {
      "schemaVersion": 1,
      "kind": "TEXT",
      "paragraph": $paragraph,
      "characterOffset": 0,
      "progression": 0,
      "normalizationVersion": 1,
      "contentFingerprint": "$CONTENT_FINGERPRINT"
    }
    """.trimIndent()

  private fun pageLocatorJson() =
    """
    {
      "schemaVersion": 1,
      "kind": "PAGE",
      "page": 0,
      "offset": 0,
      "contentFingerprint": "$CONTENT_FINGERPRINT"
    }
    """.trimIndent()

  private fun mixedTextLocatorJson() =
    """
    {
      "schemaVersion": 1,
      "kind": "MIXED",
      "itemId": "opaque-item",
      "itemIndex": 1,
      "itemLocator": {
        "schemaVersion": 1,
        "kind": "TEXT",
        "paragraph": 0,
        "characterOffset": 0,
        "progression": 0,
        "normalizationVersion": 1,
        "contentFingerprint": "$CONTENT_FINGERPRINT:opaque-item"
      },
      "contentFingerprint": "$CONTENT_FINGERPRINT"
    }
    """.trimIndent()

  private fun textRangeSelectorJson(
    startOffset: Int = 0,
    endOffset: Int = 12,
    normalizationVersion: Int = 1,
  ) = """
    {
      "schemaVersion": 1,
      "kind": "TEXT_RANGE",
      "startParagraph": 0,
      "startCharacterOffset": $startOffset,
      "endParagraph": 0,
      "endCharacterOffset": $endOffset,
      "normalizationVersion": $normalizationVersion
    }
    """.trimIndent()

  private fun mixedTextRangeSelectorJson(itemId: String = "opaque-item") =
    """
    {
      "schemaVersion": 1,
      "kind": "MIXED_TEXT_RANGE",
      "itemId": "$itemId",
      "itemIndex": 1,
      "itemSelector": ${textRangeSelectorJson()}
    }
    """.trimIndent()

  private fun jsonString(value: String?): String = value?.let { "\"$it\"" } ?: "null"

  private companion object {
    const val USER_1_ID = "shelfview-highlight-user-1"
    const val USER_2_ID = "shelfview-highlight-user-2"
    const val CONTENT_FINGERPRINT = "tales-file-hash:highlight-hash"
  }
}
