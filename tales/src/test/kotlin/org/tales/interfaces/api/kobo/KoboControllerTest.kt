package org.tales.interfaces.api.kobo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.tales.domain.model.Media
import org.tales.domain.model.MediaType
import org.tales.domain.model.TalesUser
import org.tales.domain.model.UserRoles
import org.tales.domain.model.makeBook
import org.tales.domain.model.makeLibrary
import org.tales.domain.model.makeSeries
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.domain.persistence.SeriesRepository
import org.tales.domain.persistence.TalesUserRepository
import org.tales.domain.service.SeriesLifecycle
import org.tales.domain.service.TalesUserLifecycle
import org.tales.infrastructure.configuration.TalesSettingsProvider
import org.tales.infrastructure.kobo.KoboHeaders
import org.tales.infrastructure.kobo.TalesSyncTokenGenerator
import org.tales.infrastructure.web.WebServerEffectiveSettings
import java.util.stream.Stream

@SpringBootTest(properties = ["tales.kobo.sync-item-limit=1"])
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class KoboControllerTest(
  @Autowired private val mockMvc: MockMvc,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val userRepository: TalesUserRepository,
  @Autowired private val talesUserLifecycle: TalesUserLifecycle,
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val mediaRepository: MediaRepository,
  @Autowired private val talesSyncTokenGenerator: TalesSyncTokenGenerator,
  @Autowired private val talesSettingsProvider: TalesSettingsProvider,
  @Autowired private val serverSettings: WebServerEffectiveSettings,
) {
  private val library1 = makeLibrary()
  private val user1 =
    TalesUser(
      "user@example.org",
      "",
      roles = setOf(UserRoles.KOBO_SYNC),
    )
  private lateinit var apiKey: String

  @BeforeAll
  fun `setup library`() {
    libraryRepository.insert(library1)
    userRepository.insert(user1)
    apiKey = talesUserLifecycle.createApiKey(user1, "test")!!.key
  }

  @AfterAll
  fun teardown() {
    libraryRepository.deleteAll()
    talesUserLifecycle.deleteUser(user1)
  }

  @AfterEach
  fun `clear repository`() {
    seriesLifecycle.deleteMany(seriesRepository.findAll())
  }

  @Test
  fun `given user when syncing for the first time then books are synced`() {
    // given
    val book1 = makeBook("valid", libraryId = library1.id)
    val book2 = makeBook("valid", libraryId = library1.id)

    makeSeries(name = "series1", libraryId = library1.id).also { series ->
      seriesLifecycle.createSeries(series).let { created ->
        seriesLifecycle.addBooks(created, listOf(book1, book2))
      }
    }

    mediaRepository.findById(book1.id).let { media -> mediaRepository.update(media.copy(status = Media.Status.READY, mediaType = MediaType.EPUB.type)) }
    mediaRepository.findById(book2.id).let { media -> mediaRepository.update(media.copy(status = Media.Status.READY, mediaType = MediaType.EPUB.type)) }

    // first sync
    val mvcResult1 =
      mockMvc
        .get("/kobo/$apiKey/v1/library/sync")
        .andExpect {
          status { isOk() }
          jsonPath("$.length()") { value(1) }
          header { string(KoboHeaders.X_KOBO_SYNC, "continue") }
        }.andReturn()

    val syncToken1Base64 = mvcResult1.response.getHeaderValue(KoboHeaders.X_KOBO_SYNCTOKEN) as String
    val syncToken1 = talesSyncTokenGenerator.fromBase64(syncToken1Base64)
    assertThat(syncToken1.ongoingSyncPointId).isNotEmpty()
    assertThat(syncToken1.lastSuccessfulSyncPointId).isNull()

    // second sync
    val mvcResult2 =
      mockMvc
        .get("/kobo/$apiKey/v1/library/sync") {
          header(KoboHeaders.X_KOBO_SYNCTOKEN, syncToken1Base64)
        }.andExpect {
          status { isOk() }
          jsonPath("$.length()") { value(1) }
          header { doesNotExist(KoboHeaders.X_KOBO_SYNC) }
        }.andReturn()

    val syncToken2 = talesSyncTokenGenerator.fromBase64(mvcResult2.response.getHeaderValue(KoboHeaders.X_KOBO_SYNCTOKEN) as String)
    assertThat(syncToken2.ongoingSyncPointId).isNull()
    assertThat(syncToken2.lastSuccessfulSyncPointId).isEqualTo(syncToken1.ongoingSyncPointId)
  }

  @Test
  fun `given kobo proxy is enabled when requesting book cover for non-existent book then redirect response is returned`() {
    talesSettingsProvider.koboProxy = true

    try {
      mockMvc
        .get("/kobo/$apiKey/v1/books/nonexistent/thumbnail/800/800/false/image.jpg")
        .andExpect {
          status { isTemporaryRedirect() }
          header { string(HttpHeaders.LOCATION, "https://cdn.kobo.com/book-images/nonexistent/800/800/false/image.jpg") }
        }
    } finally {
      talesSettingsProvider.koboProxy = false
    }
  }

  @Nested
  inner class HostHeader(
    @param:Value($$"${server.port:#{null}}") private val configServerPort: Int?,
  ) {
    @ParameterizedTest
    @MethodSource("headers")
    fun `given partial host header when getting initialization then img urls are correct`(
      hostHeader: String,
      expected: String,
      extraHeaders: Map<String, String>?,
      koboPort: Int?,
    ) {
      // ServletWebServerInitializedEvent is not triggered during tests
      serverSettings.effectiveServerPort = configServerPort

      val oldPort = talesSettingsProvider.koboPort
      koboPort?.let { talesSettingsProvider.koboPort = it }

      try {
        mockMvc
          .get("/kobo/$apiKey/v1/initialization") {
            header(HttpHeaders.HOST, hostHeader)
            extraHeaders?.forEach { (h, v) -> header(h, v) }
          }.andExpect {
            jsonPath("Resources.image_host") { value(expected) }
          }.andReturn()
      } finally {
        talesSettingsProvider.koboPort = oldPort
      }
    }

    private fun headers(): Stream<Arguments> =
      Stream.of(
        Arguments.of("127.0.0.1", "http://127.0.0.1:$configServerPort", null, null),
        Arguments.of("localhost", "http://localhost:$configServerPort", null, null),
        Arguments.of(
          "127.0.0.1",
          "https://demo.tales.org",
          mapOf(
            "X-Forwarded-Proto" to "https",
            "X-Forwarded-Host" to "demo.tales.org",
          ),
          null,
        ),
        Arguments.of("127.0.0.1", "http://127.0.0.1:8085", null, 8085),
        Arguments.of("localhost", "http://localhost:8085", null, 8085),
        Arguments.of(
          "127.0.0.1",
          "https://demo.tales.org",
          mapOf(
            "X-Forwarded-Proto" to "https",
            "X-Forwarded-Host" to "demo.tales.org",
          ),
          8085,
        ),
      )
  }
}
