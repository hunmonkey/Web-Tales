package org.tales.shelfview.interfaces.api

import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.tales.domain.model.Book
import org.tales.domain.model.BookPage
import org.tales.domain.model.Media
import org.tales.domain.model.MediaFile
import org.tales.domain.model.ReadProgress
import org.tales.domain.model.TalesUser
import org.tales.domain.model.makeBook
import org.tales.domain.model.makeLibrary
import org.tales.domain.model.makeSeries
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.domain.persistence.ReadProgressRepository
import org.tales.domain.persistence.SeriesRepository
import org.tales.domain.persistence.TalesUserRepository
import org.tales.domain.service.LibraryLifecycle
import org.tales.domain.service.SeriesLifecycle
import org.tales.interfaces.api.rest.WithMockCustomUser
import org.tales.shelfview.domain.ShelfViewReaderPosition
import org.tales.shelfview.infrastructure.jooq.ShelfViewReaderStateDao
import org.tales.domain.model.MediaType as TalesMediaType

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ShelfViewPositionControllerTest(
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val mediaRepository: MediaRepository,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val userRepository: TalesUserRepository,
  @Autowired private val readProgressRepository: ReadProgressRepository,
  @Autowired private val readerStateDao: ShelfViewReaderStateDao,
  @Autowired private val mockMvc: MockMvc,
) {
  private val library = makeLibrary(id = "shelfview-position-library")
  private val user1 = TalesUser("shelfview-position-user1@example.org", "", id = USER_1_ID)
  private val user2 = TalesUser("shelfview-position-user2@example.org", "", id = USER_2_ID)

  @BeforeAll
  fun `setup library and users`() {
    libraryRepository.insert(library)
    userRepository.insert(user1)
    userRepository.insert(user2)
  }

  @AfterEach
  fun `clear repository`() {
    readerStateDao.deleteAllPositions()
    readProgressRepository.deleteAll()
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
  fun `given no saved position when getting position then returns no content`() {
    val book = createBookWithMedia()

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/position")
      .andExpect {
        status { isNoContent() }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given page locator when saving position then it can be fetched`() {
    val book = createBookWithMedia()

    mockMvc
      .put("/api/shelfview/v1/books/${book.id}/position") {
        contentType = MediaType.APPLICATION_JSON
        content = pagePositionJson(page = 0, progression = 0.5F)
      }.andExpect {
        status { isOk() }
        jsonPath("$.locator.schemaVersion") { value(1) }
        jsonPath("$.locator.kind") { value("PAGE") }
        jsonPath("$.locator.page") { value(0) }
        jsonPath("$.locator.offset") { value(0.0) }
        jsonPath("$.locator.contentFingerprint") { value(CONTENT_FINGERPRINT) }
        jsonPath("$.progression") { value(0.5) }
        jsonPath("$.contentFingerprint") { value(CONTENT_FINGERPRINT) }
        jsonPath("$.updatedAt") { exists() }
      }

    mockMvc
      .put("/api/shelfview/v1/books/${book.id}/position") {
        contentType = MediaType.APPLICATION_JSON
        content = pagePositionJson(page = 1, progression = 1.0F)
      }.andExpect {
        status { isOk() }
        jsonPath("$.locator.page") { value(1) }
        jsonPath("$.progression") { value(1.0) }
      }

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/position")
      .andExpect {
        status { isOk() }
        jsonPath("$.locator.page") { value(1) }
        jsonPath("$.progression") { value(1.0) }
      }

    val readProgress = readProgressRepository.findByBookIdAndUserIdOrNull(book.id, USER_1_ID)
    assertThat(readProgress).isNotNull
    assertThat(readProgress!!.page).isEqualTo(2)
    assertThat(readProgress.completed).isTrue()
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given existing Tales progress when getting fixed page position then returns page locator`() {
    val book = createBookWithMedia()
    readProgressRepository.save(ReadProgress(book.id, USER_1_ID, page = 2, completed = true))

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/position")
      .andExpect {
        status { isOk() }
        jsonPath("$.locator.schemaVersion") { value(1) }
        jsonPath("$.locator.kind") { value("PAGE") }
        jsonPath("$.locator.page") { value(1) }
        jsonPath("$.locator.offset") { value(0.0) }
        jsonPath("$.locator.contentFingerprint") { value(CONTENT_FINGERPRINT) }
        jsonPath("$.progression") { value(1.0) }
        jsonPath("$.contentFingerprint") { value(CONTENT_FINGERPRINT) }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given mixed zip Tales progress when getting position then returns no content`() {
    val book =
      createBookWithMedia(
        bookName = "Mixed.zip",
        files =
          listOf(
            MediaFile("001.jpg", "image/jpeg"),
            MediaFile("chapter.txt", "text/plain"),
          ),
      )
    readProgressRepository.save(ReadProgress(book.id, USER_1_ID, page = 1, completed = false))

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/position")
      .andExpect {
        status { isNoContent() }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given forged mixed locator for a fixed page book when saving position then it is rejected`() {
    val book = createBookWithMedia()

    mockMvc
      .put("/api/shelfview/v1/books/${book.id}/position") {
        contentType = MediaType.APPLICATION_JSON
        content = mixedPositionJson(progression = 0.75F)
      }.andExpect {
        status { isBadRequest() }
      }

    assertThat(readerStateDao.findPositionOrNull(book.id, USER_1_ID)).isNull()
    assertThat(readProgressRepository.findByBookIdAndUserIdOrNull(book.id, USER_1_ID)).isNull()
  }

  @Test
  @WithMockCustomUser(id = USER_2_ID)
  fun `given position saved by one user when another user gets position then returns no content`() {
    val book = createBookWithMedia()
    readerStateDao.savePosition(
      ShelfViewReaderPosition(
        userId = USER_1_ID,
        bookId = book.id,
        locatorJson =
          """
          {
            "schemaVersion": 1,
            "kind": "PAGE",
            "page": 0,
            "offset": 0,
            "contentFingerprint": "$CONTENT_FINGERPRINT"
          }
          """.trimIndent(),
        progression = 0.5F,
        contentFingerprint = CONTENT_FINGERPRINT,
      ),
    )

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/position")
      .andExpect {
        status { isNoContent() }
      }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      """{"progression":0.5}""",
      """{"locator":{"schemaVersion":2,"kind":"PAGE","page":0,"offset":0,"contentFingerprint":"tales-file-hash:position-hash"},"progression":0.5}""",
      """{"locator":{"schemaVersion":1,"kind":"PAGE","page":-1,"offset":0,"contentFingerprint":"tales-file-hash:position-hash"},"progression":0.5}""",
      """{"locator":{"schemaVersion":1,"kind":"PAGE","page":2,"offset":0,"contentFingerprint":"tales-file-hash:position-hash"},"progression":0.5}""",
      """{"locator":{"schemaVersion":1,"kind":"PAGE","page":0,"offset":1.5,"contentFingerprint":"tales-file-hash:position-hash"},"progression":0.5}""",
      """{"locator":{"schemaVersion":1,"kind":"PAGE","page":0,"offset":0,"contentFingerprint":"stale"},"progression":0.5}""",
      """{"locator":{"schemaVersion":1,"kind":"TEXT","paragraph":0,"characterOffset":0,"progression":1.5,"normalizationVersion":1,"contentFingerprint":"tales-file-hash:position-hash"},"progression":0.5}""",
      """{"locator":{"schemaVersion":1,"kind":"TEXT","paragraph":0,"characterOffset":0,"progression":0.5,"normalizationVersion":2,"contentFingerprint":"tales-file-hash:position-hash"},"progression":0.5}""",
      """{"locator":{"schemaVersion":1,"kind":"MIXED","itemId":"opaque","itemIndex":-1,"itemLocator":{"schemaVersion":1,"kind":"PAGE","page":0,"offset":0,"contentFingerprint":"tales-file-hash:position-hash:item"},"contentFingerprint":"tales-file-hash:position-hash"},"progression":0.5}""",
      """{"locator":{"schemaVersion":1,"kind":"MIXED","itemId":"opaque","itemIndex":0,"itemLocator":{"schemaVersion":1,"kind":"UNKNOWN","contentFingerprint":"tales-file-hash:position-hash:item"},"contentFingerprint":"tales-file-hash:position-hash"},"progression":0.5}""",
      """{"locator":{"schemaVersion":1,"kind":"UNKNOWN","contentFingerprint":"tales-file-hash:position-hash"},"progression":0.5}""",
      """{"locator":{"schemaVersion":1,"kind":"PAGE","page":0,"offset":0,"contentFingerprint":"tales-file-hash:position-hash"},"progression":1.5}""",
    ],
  )
  @WithMockCustomUser(id = USER_1_ID)
  fun `given invalid locator when saving position then returns bad request`(jsonString: String) {
    val book = createBookWithMedia()

    mockMvc
      .put("/api/shelfview/v1/books/${book.id}/position") {
        contentType = MediaType.APPLICATION_JSON
        content = jsonString
      }.andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID, sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given user without book access when saving position then returns forbidden`() {
    val book = createBookWithMedia()

    mockMvc
      .put("/api/shelfview/v1/books/${book.id}/position") {
        contentType = MediaType.APPLICATION_JSON
        content = pagePositionJson(page = 0, progression = 0.5F)
      }.andExpect {
        status { isForbidden() }
      }
  }

  private fun createBookWithMedia(
    bookName: String = "Book One.cbz",
    files: List<MediaFile> = emptyList(),
  ): Book {
    val createdSeries = seriesLifecycle.createSeries(makeSeries(name = "series", libraryId = library.id))
    seriesLifecycle.addBooks(createdSeries, listOf(makeBook(bookName, libraryId = library.id)))

    val book = bookRepository.findAll().first { it.libraryId == library.id }.copy(fileHash = "position-hash")
    bookRepository.update(book)
    mediaRepository.findById(book.id).let {
      mediaRepository.update(
        it.copy(
          status = Media.Status.READY,
          mediaType = TalesMediaType.ZIP.type,
          pageCount = 2,
          pages =
            listOf(
              BookPage("001.jpg", "image/jpeg"),
              BookPage("002.jpg", "image/jpeg"),
            ),
          files = files,
        ),
      )
    }

    return book
  }

  private fun pagePositionJson(
    page: Int,
    progression: Float,
  ) = """
    {
      "locator": {
        "schemaVersion": 1,
        "kind": "PAGE",
        "page": $page,
        "offset": 0,
        "contentFingerprint": "$CONTENT_FINGERPRINT"
      },
      "progression": $progression
    }
    """.trimIndent()

  private fun mixedPositionJson(progression: Float) =
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
      "progression": $progression
    }
    """.trimIndent()

  private companion object {
    const val USER_1_ID = "shelfview-position-user-1"
    const val USER_2_ID = "shelfview-position-user-2"
    const val CONTENT_FINGERPRINT = "tales-file-hash:position-hash"
  }
}
