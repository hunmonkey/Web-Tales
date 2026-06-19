package org.gotson.komga.shelfview.interfaces.api

import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.BookPage
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.makeBook
import org.gotson.komga.domain.model.makeLibrary
import org.gotson.komga.domain.model.makeSeries
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.KomgaUserRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.domain.service.LibraryLifecycle
import org.gotson.komga.domain.service.SeriesLifecycle
import org.gotson.komga.interfaces.api.rest.WithMockCustomUser
import org.gotson.komga.shelfview.domain.ShelfViewBookmark
import org.gotson.komga.shelfview.infrastructure.jooq.ShelfViewReaderStateDao
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.gotson.komga.domain.model.MediaType as KomgaMediaType

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ShelfViewBookmarkControllerTest(
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val mediaRepository: MediaRepository,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val userRepository: KomgaUserRepository,
  @Autowired private val readerStateDao: ShelfViewReaderStateDao,
  @Autowired private val mockMvc: MockMvc,
) {
  private val library = makeLibrary(id = "shelfview-bookmark-library")
  private val user1 = KomgaUser("shelfview-bookmark-user1@example.org", "", id = USER_1_ID)
  private val user2 = KomgaUser("shelfview-bookmark-user2@example.org", "", id = USER_2_ID)

  @BeforeAll
  fun `setup library and users`() {
    libraryRepository.insert(library)
    userRepository.insert(user1)
    userRepository.insert(user2)
  }

  @AfterEach
  fun `clear repository`() {
    readerStateDao.deleteAllBookmarks()
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
  fun `given no bookmarks when listing bookmarks then returns empty list`() {
    val book = createBookWithMedia()

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/bookmarks")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(0) }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given bookmark when creating updating and deleting then bookmark changes are persisted`() {
    val book = createBookWithMedia()

    mockMvc
      .post("/api/shelfview/v1/books/${book.id}/bookmarks") {
        contentType = MediaType.APPLICATION_JSON
        content = bookmarkJson(page = 0, label = "Start", note = "Opening page")
      }.andExpect {
        status { isCreated() }
        jsonPath("$.id") { exists() }
        jsonPath("$.bookId") { value(book.id) }
        jsonPath("$.locator.kind") { value("PAGE") }
        jsonPath("$.locator.page") { value(0) }
        jsonPath("$.label") { value("Start") }
        jsonPath("$.note") { value("Opening page") }
        jsonPath("$.createdAt") { exists() }
        jsonPath("$.updatedAt") { exists() }
      }

    val bookmarkId = readerStateDao.findBookmarks(book.id, USER_1_ID).first().id

    mockMvc
      .post("/api/shelfview/v1/books/${book.id}/bookmarks") {
        contentType = MediaType.APPLICATION_JSON
        content = bookmarkJson(page = 1, label = "Second", note = null)
      }.andExpect {
        status { isCreated() }
      }

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/bookmarks")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(2) }
        jsonPath("${'$'}[0].label") { value("Start") }
        jsonPath("${'$'}[1].label") { value("Second") }
      }

    mockMvc
      .put("/api/shelfview/v1/bookmarks/$bookmarkId") {
        contentType = MediaType.APPLICATION_JSON
        content = bookmarkJson(page = 1, label = "Updated", note = "Changed")
      }.andExpect {
        status { isOk() }
        jsonPath("$.id") { value(bookmarkId) }
        jsonPath("$.locator.page") { value(1) }
        jsonPath("$.label") { value("Updated") }
        jsonPath("$.note") { value("Changed") }
      }

    mockMvc
      .delete("/api/shelfview/v1/bookmarks/$bookmarkId")
      .andExpect {
        status { isNoContent() }
      }

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/bookmarks")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(1) }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given mixed locator when creating bookmark then it can be fetched`() {
    val book = createBookWithMedia()

    mockMvc
      .post("/api/shelfview/v1/books/${book.id}/bookmarks") {
        contentType = MediaType.APPLICATION_JSON
        content = mixedBookmarkJson(label = "Archive image")
      }.andExpect {
        status { isCreated() }
        jsonPath("$.locator.kind") { value("MIXED") }
        jsonPath("$.locator.itemId") { value("opaque-item") }
        jsonPath("$.locator.itemIndex") { value(1) }
        jsonPath("$.locator.itemLocator.kind") { value("PAGE") }
        jsonPath("$.locator.itemLocator.page") { value(0) }
        jsonPath("$.label") { value("Archive image") }
      }

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/bookmarks")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(1) }
        jsonPath("${'$'}[0].locator.kind") { value("MIXED") }
        jsonPath("${'$'}[0].locator.itemId") { value("opaque-item") }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_2_ID)
  fun `given bookmark saved by one user when another user reads or changes bookmark then it is isolated`() {
    val book = createBookWithMedia()
    val bookmark =
      ShelfViewBookmark(
        id = "bookmark-user1",
        userId = USER_1_ID,
        bookId = book.id,
        locatorJson = pageLocatorJson(page = 0),
        label = "Private",
      )
    readerStateDao.saveBookmark(bookmark)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/bookmarks")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(0) }
      }

    mockMvc
      .put("/api/shelfview/v1/bookmarks/${bookmark.id}") {
        contentType = MediaType.APPLICATION_JSON
        content = bookmarkJson(page = 1, label = "Other", note = null)
      }.andExpect {
        status { isNotFound() }
      }

    mockMvc
      .delete("/api/shelfview/v1/bookmarks/${bookmark.id}")
      .andExpect {
        status { isNotFound() }
      }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      """{"label":"No locator"}""",
      """{"locator":{"schemaVersion":2,"kind":"PAGE","page":0,"offset":0,"contentFingerprint":"komga-file-hash:bookmark-hash"},"label":"Bad"}""",
      """{"locator":{"schemaVersion":1,"kind":"PAGE","page":-1,"offset":0,"contentFingerprint":"komga-file-hash:bookmark-hash"},"label":"Bad"}""",
      """{"locator":{"schemaVersion":1,"kind":"PAGE","page":2,"offset":0,"contentFingerprint":"komga-file-hash:bookmark-hash"},"label":"Bad"}""",
      """{"locator":{"schemaVersion":1,"kind":"PAGE","page":0,"offset":2,"contentFingerprint":"komga-file-hash:bookmark-hash"},"label":"Bad"}""",
      """{"locator":{"schemaVersion":1,"kind":"PAGE","page":0,"offset":0,"contentFingerprint":"stale"},"label":"Bad"}""",
      """{"locator":{"schemaVersion":1,"kind":"TEXT","paragraph":0,"characterOffset":0,"progression":1.5,"normalizationVersion":1,"contentFingerprint":"komga-file-hash:bookmark-hash"},"label":"Bad"}""",
      """{"locator":{"schemaVersion":1,"kind":"TEXT","paragraph":0,"characterOffset":0,"progression":0.5,"normalizationVersion":2,"contentFingerprint":"komga-file-hash:bookmark-hash"},"label":"Bad"}""",
      """{"locator":{"schemaVersion":1,"kind":"MIXED","itemId":"opaque","itemIndex":-1,"itemLocator":{"schemaVersion":1,"kind":"PAGE","page":0,"offset":0,"contentFingerprint":"komga-file-hash:bookmark-hash:item"},"contentFingerprint":"komga-file-hash:bookmark-hash"},"label":"Bad"}""",
      """{"locator":{"schemaVersion":1,"kind":"MIXED","itemId":"opaque","itemIndex":0,"itemLocator":{"schemaVersion":1,"kind":"UNKNOWN","contentFingerprint":"komga-file-hash:bookmark-hash:item"},"contentFingerprint":"komga-file-hash:bookmark-hash"},"label":"Bad"}""",
    ],
  )
  @WithMockCustomUser(id = USER_1_ID)
  fun `given invalid locator when creating bookmark then returns bad request`(jsonString: String) {
    val book = createBookWithMedia()

    mockMvc
      .post("/api/shelfview/v1/books/${book.id}/bookmarks") {
        contentType = MediaType.APPLICATION_JSON
        content = jsonString
      }.andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID, sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given user without book access when creating bookmark then returns forbidden`() {
    val book = createBookWithMedia()

    mockMvc
      .post("/api/shelfview/v1/books/${book.id}/bookmarks") {
        contentType = MediaType.APPLICATION_JSON
        content = bookmarkJson(page = 0, label = "Hidden", note = null)
      }.andExpect {
        status { isForbidden() }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID, sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given existing bookmark without book access when updating bookmark then returns forbidden`() {
    val book = createBookWithMedia()
    val bookmark =
      ShelfViewBookmark(
        id = "bookmark-no-access",
        userId = USER_1_ID,
        bookId = book.id,
        locatorJson = pageLocatorJson(page = 0),
      )
    readerStateDao.saveBookmark(bookmark)

    mockMvc
      .put("/api/shelfview/v1/bookmarks/${bookmark.id}") {
        contentType = MediaType.APPLICATION_JSON
        content = bookmarkJson(page = 1, label = "Hidden", note = null)
      }.andExpect {
        status { isForbidden() }
      }
  }

  private fun createBookWithMedia(): Book {
    val createdSeries = seriesLifecycle.createSeries(makeSeries(name = "series", libraryId = library.id))
    seriesLifecycle.addBooks(createdSeries, listOf(makeBook("Book One.cbz", libraryId = library.id)))

    val book = bookRepository.findAll().first { it.libraryId == library.id }.copy(fileHash = "bookmark-hash")
    bookRepository.update(book)
    mediaRepository.findById(book.id).let {
      mediaRepository.update(
        it.copy(
          status = Media.Status.READY,
          mediaType = KomgaMediaType.ZIP.type,
          pageCount = 2,
          pages =
            listOf(
              BookPage("001.jpg", "image/jpeg"),
              BookPage("002.jpg", "image/jpeg"),
            ),
        ),
      )
    }

    return book
  }

  private fun bookmarkJson(
    page: Int,
    label: String,
    note: String?,
  ) = """
    {
      "locator": ${pageLocatorJson(page)},
      "label": "$label",
      "note": ${note?.let { "\"$it\"" } ?: "null"}
    }
    """.trimIndent()

  private fun mixedBookmarkJson(label: String) =
    """
    {
      "locator": {
        "schemaVersion": 1,
        "kind": "MIXED",
        "itemId": "opaque-item",
        "itemIndex": 1,
        "itemLocator": {
          "schemaVersion": 1,
          "kind": "PAGE",
          "page": 0,
          "offset": 0,
          "contentFingerprint": "$CONTENT_FINGERPRINT:opaque-item"
        },
        "contentFingerprint": "$CONTENT_FINGERPRINT"
      },
      "label": "$label",
      "note": null
    }
    """.trimIndent()

  private fun pageLocatorJson(page: Int) =
    """
    {
      "schemaVersion": 1,
      "kind": "PAGE",
      "page": $page,
      "offset": 0,
      "contentFingerprint": "$CONTENT_FINGERPRINT"
    }
    """.trimIndent()

  private companion object {
    const val USER_1_ID = "shelfview-bookmark-user-1"
    const val USER_2_ID = "shelfview-bookmark-user-2"
    const val CONTENT_FINGERPRINT = "komga-file-hash:bookmark-hash"
  }
}
