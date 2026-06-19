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
import org.gotson.komga.shelfview.domain.ShelfViewReaderPreference
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.gotson.komga.domain.model.MediaType as KomgaMediaType

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ShelfViewPreferenceControllerTest(
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
  private val library = makeLibrary(id = LIBRARY_ID)
  private val user1 = KomgaUser("shelfview-preference-user1@example.org", "", id = USER_1_ID)
  private val user2 = KomgaUser("shelfview-preference-user2@example.org", "", id = USER_2_ID)

  @BeforeAll
  fun `setup library and users`() {
    libraryRepository.insert(library)
    userRepository.insert(user1)
    userRepository.insert(user2)
  }

  @AfterEach
  fun `clear repository`() {
    readerStateDao.deleteAllPreferences()
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
  fun `given scoped preferences when getting book preference then returns effective settings in precedence order`() {
    val book = createBookWithMedia()
    readerStateDao.savePreference(preference("GLOBAL", "GLOBAL", """{"layout":"SINGLE_PAGE","pageFit":"HEIGHT"}"""))
    readerStateDao.savePreference(preference("LIBRARY", book.libraryId, """{"readingDirection":"RTL","pageFit":"WIDTH"}"""))
    readerStateDao.savePreference(preference("SERIES", book.seriesId, """{"layout":"DOUBLE_PAGE"}"""))
    readerStateDao.savePreference(preference("BOOK", book.id, """{"imageFilterPreset":"MONO_COMIC"}"""))

    mockMvc
      .get("/api/shelfview/v1/preferences") {
        param("scopeType", "BOOK")
        param("scopeId", book.id)
        param("format", FORMAT)
      }.andExpect {
        status { isOk() }
        jsonPath("$.scopeType") { value("BOOK") }
        jsonPath("$.scopeId") { value(book.id) }
        jsonPath("$.settings.imageFilterPreset") { value("MONO_COMIC") }
        jsonPath("$.effectiveSettings.layout") { value("DOUBLE_PAGE") }
        jsonPath("$.effectiveSettings.readingDirection") { value("RTL") }
        jsonPath("$.effectiveSettings.pageFit") { value("WIDTH") }
        jsonPath("$.effectiveSettings.imageFilterPreset") { value("MONO_COMIC") }
        jsonPath("$.sources.length()") { value(4) }
        jsonPath("${'$'}.sources[0].scopeType") { value("GLOBAL") }
        jsonPath("${'$'}.sources[1].scopeType") { value("LIBRARY") }
        jsonPath("${'$'}.sources[2].scopeType") { value("SERIES") }
        jsonPath("${'$'}.sources[3].scopeType") { value("BOOK") }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given supported scopes when saving preferences then they can be read back`() {
    val book = createBookWithMedia()

    listOf(
      "GLOBAL" to "GLOBAL",
      "LIBRARY" to book.libraryId,
      "SERIES" to book.seriesId,
      "BOOK" to book.id,
    ).forEach { (scopeType, scopeId) ->
      mockMvc
        .put("/api/shelfview/v1/preferences") {
          contentType = MediaType.APPLICATION_JSON
          content = preferenceJson(scopeType, scopeId, """{"layout":"DOUBLE_PAGE"}""")
        }.andExpect {
          status { isOk() }
          jsonPath("$.scopeType") { value(scopeType) }
          jsonPath("$.scopeId") { value(scopeId) }
          jsonPath("$.settings.layout") { value("DOUBLE_PAGE") }
        }
    }

    mockMvc
      .get("/api/shelfview/v1/preferences") {
        param("scopeType", "BOOK")
        param("scopeId", book.id)
        param("format", FORMAT)
      }.andExpect {
        status { isOk() }
        jsonPath("$.sources.length()") { value(4) }
        jsonPath("$.effectiveSettings.layout") { value("DOUBLE_PAGE") }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given existing preference when saving same scope then settings are replaced`() {
    val book = createBookWithMedia()

    mockMvc
      .put("/api/shelfview/v1/preferences") {
        contentType = MediaType.APPLICATION_JSON
        content = preferenceJson("BOOK", book.id, """{"layout":"DOUBLE_PAGE","readingDirection":"RTL"}""")
      }.andExpect {
        status { isOk() }
        jsonPath("$.settings.layout") { value("DOUBLE_PAGE") }
        jsonPath("$.settings.readingDirection") { value("RTL") }
      }

    mockMvc
      .put("/api/shelfview/v1/preferences") {
        contentType = MediaType.APPLICATION_JSON
        content = preferenceJson("BOOK", book.id, """{"pageFit":"WIDTH"}""")
      }.andExpect {
        status { isOk() }
        jsonPath("$.settings.pageFit") { value("WIDTH") }
        jsonPath("$.effectiveSettings.layout") { value("SINGLE_PAGE") }
        jsonPath("$.effectiveSettings.readingDirection") { value("LTR") }
        jsonPath("$.effectiveSettings.pageFit") { value("WIDTH") }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_2_ID)
  fun `given preference saved by one user when another user gets preference then it is isolated`() {
    val book = createBookWithMedia()
    readerStateDao.savePreference(preference("BOOK", book.id, """{"layout":"DOUBLE_PAGE"}"""))

    mockMvc
      .get("/api/shelfview/v1/preferences") {
        param("scopeType", "BOOK")
        param("scopeId", book.id)
        param("format", FORMAT)
      }.andExpect {
        status { isOk() }
        jsonPath("$.sources.length()") { value(0) }
        jsonPath("$.effectiveSettings.layout") { value("SINGLE_PAGE") }
        jsonPath("$.effectiveSettings.readingDirection") { value("LTR") }
        jsonPath("$.effectiveSettings.pageFit") { value("SCREEN") }
        jsonPath("$.effectiveSettings.imageFilterPreset") { value("ORIGINAL") }
      }
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      """{"scopeType":"UNKNOWN","scopeId":"GLOBAL","format":"FIXED_PAGE_SEQUENCE","settings":{"layout":"SINGLE_PAGE"}}""",
      """{"scopeType":"GLOBAL","scopeId":"wrong","format":"FIXED_PAGE_SEQUENCE","settings":{"layout":"SINGLE_PAGE"}}""",
      """{"scopeType":"GLOBAL","scopeId":"GLOBAL","format":"UNKNOWN","settings":{"layout":"SINGLE_PAGE"}}""",
      """{"scopeType":"GLOBAL","scopeId":"GLOBAL","format":"FIXED_PAGE_SEQUENCE","settings":[]}""",
      """{"scopeType":"GLOBAL","scopeId":"GLOBAL","format":"FIXED_PAGE_SEQUENCE","settings":{"unknown":"value"}}""",
      """{"scopeType":"GLOBAL","scopeId":"GLOBAL","format":"FIXED_PAGE_SEQUENCE","settings":{"layout":"BAD"}}""",
      """{"scopeType":"GLOBAL","scopeId":"GLOBAL","format":"FIXED_PAGE_SEQUENCE","settings":{"pageFit":3}}""",
    ],
  )
  @WithMockCustomUser(id = USER_1_ID)
  fun `given invalid preference request when saving preference then returns bad request`(jsonString: String) {
    mockMvc
      .put("/api/shelfview/v1/preferences") {
        contentType = MediaType.APPLICATION_JSON
        content = jsonString
      }.andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID, sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given user without book access when saving preference then returns forbidden`() {
    val book = createBookWithMedia()

    mockMvc
      .put("/api/shelfview/v1/preferences") {
        contentType = MediaType.APPLICATION_JSON
        content = preferenceJson("BOOK", book.id, """{"layout":"DOUBLE_PAGE"}""")
      }.andExpect {
        status { isForbidden() }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID, sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given user without library access when getting library preference then returns forbidden`() {
    mockMvc
      .get("/api/shelfview/v1/preferences") {
        param("scopeType", "LIBRARY")
        param("scopeId", LIBRARY_ID)
        param("format", FORMAT)
      }.andExpect {
        status { isForbidden() }
      }
  }

  private fun createBookWithMedia(): Book {
    val createdSeries = seriesLifecycle.createSeries(makeSeries(name = "series", libraryId = library.id))
    seriesLifecycle.addBooks(createdSeries, listOf(makeBook("Book One.cbz", libraryId = library.id)))

    val book = bookRepository.findAll().first { it.libraryId == library.id }
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

  private fun preference(
    scopeType: String,
    scopeId: String,
    settingsJson: String,
  ) = ShelfViewReaderPreference(
    userId = USER_1_ID,
    scopeType = scopeType,
    scopeId = scopeId,
    format = FORMAT,
    settingsJson = settingsJson,
  )

  private fun preferenceJson(
    scopeType: String,
    scopeId: String,
    settingsJson: String,
  ) = """
    {
      "scopeType": "$scopeType",
      "scopeId": "$scopeId",
      "format": "$FORMAT",
      "settings": $settingsJson
    }
    """.trimIndent()

  private companion object {
    const val LIBRARY_ID = "shelfview-preference-library"
    const val USER_1_ID = "shelfview-preference-user-1"
    const val USER_2_ID = "shelfview-preference-user-2"
    const val FORMAT = "FIXED_PAGE_SEQUENCE"
  }
}
