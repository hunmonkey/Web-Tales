package org.tales.shelfview.interfaces.api

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
import org.tales.domain.model.TalesUser
import org.tales.domain.persistence.TalesUserRepository
import org.tales.interfaces.api.rest.WithMockCustomUser
import org.tales.shelfview.domain.ShelfViewDictionaryEntry
import org.tales.shelfview.domain.ShelfViewDictionaryProvider
import org.tales.shelfview.domain.ShelfViewDictionaryProviderKind
import org.tales.shelfview.domain.persistence.ShelfViewDictionaryRepository

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ShelfViewDictionaryControllerTest(
  @Autowired private val userRepository: TalesUserRepository,
  @Autowired private val dictionaryRepository: ShelfViewDictionaryRepository,
  @Autowired private val mockMvc: MockMvc,
) {
  private val user1 = TalesUser("shelfview-dictionary-user1@example.org", "", id = USER_1_ID)
  private val user2 = TalesUser("shelfview-dictionary-user2@example.org", "", id = USER_2_ID)

  @BeforeAll
  fun setup() {
    userRepository.insert(user1)
    userRepository.insert(user2)
  }

  @AfterEach
  fun cleanup() {
    dictionaryRepository.deleteAllProviders()
  }

  @AfterAll
  fun teardown() {
    userRepository.delete(USER_1_ID)
    userRepository.delete(USER_2_ID)
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given no dictionary providers when listing then returns empty list`() {
    mockMvc
      .get("/api/shelfview/v1/dictionary/providers")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(0) }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given local pack when importing and looking up then provider entries are server synced`() {
    mockMvc
      .post("/api/shelfview/v1/dictionary/providers/local-pack") {
        contentType = MediaType.APPLICATION_JSON
        content = localPackJson()
      }.andExpect {
        status { isCreated() }
        jsonPath("$.id") { exists() }
        jsonPath("$.kind") { value("LOCAL_PACK") }
        jsonPath("$.label") { value("Test Dictionary") }
        jsonPath("$.sourceName") { value("Test Dictionary") }
        jsonPath("$.licenseName") { value("First-party test data") }
        jsonPath("$.entryCount") { value(2) }
      }

    val providerId =
      dictionaryRepository
        .findProviderSummaries(USER_1_ID)
        .first()
        .provider.id

    mockMvc
      .get("/api/shelfview/v1/dictionary/providers")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(1) }
        jsonPath("${'$'}[0].id") { value(providerId) }
        jsonPath("${'$'}[0].entryCount") { value(2) }
      }

    mockMvc
      .get("/api/shelfview/v1/dictionary/lookup") {
        param("query", "  첫   번째  ")
      }.andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(1) }
        jsonPath("${'$'}[0].kind") { value("LOCAL_PACK") }
        jsonPath("${'$'}[0].providerId") { value(providerId) }
        jsonPath("${'$'}[0].providerLabel") { value("Test Dictionary") }
        jsonPath("${'$'}[0].term") { value("첫 번째") }
        jsonPath("${'$'}[0].definition") { value("서버에 저장된 뜻") }
        jsonPath("${'$'}[0].sourceName") { value("Entry Source") }
      }

    mockMvc
      .delete("/api/shelfview/v1/dictionary/providers/$providerId")
      .andExpect {
        status { isNoContent() }
      }

    mockMvc
      .get("/api/shelfview/v1/dictionary/lookup") {
        param("query", "첫 번째")
      }.andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(0) }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_2_ID)
  fun `given provider owned by another user when listing lookup or deleting then it is isolated`() {
    val provider =
      ShelfViewDictionaryProvider(
        id = "dictionary-user1-provider",
        userId = USER_1_ID,
        kind = ShelfViewDictionaryProviderKind.LOCAL_PACK,
        label = "Private Dictionary",
        sourceName = "Private Dictionary",
        licenseName = "First-party test data",
      )
    dictionaryRepository.saveLocalPack(
      provider,
      listOf(
        ShelfViewDictionaryEntry(
          providerId = provider.id,
          term = "비밀",
          normalizedTerm = "비밀",
          definition = "다른 사용자는 볼 수 없는 뜻",
        ),
      ),
    )

    mockMvc
      .get("/api/shelfview/v1/dictionary/providers")
      .andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(0) }
      }

    mockMvc
      .get("/api/shelfview/v1/dictionary/lookup") {
        param("query", "비밀")
      }.andExpect {
        status { isOk() }
        jsonPath("$.length()") { value(0) }
      }

    mockMvc
      .delete("/api/shelfview/v1/dictionary/providers/${provider.id}")
      .andExpect {
        status { isNotFound() }
      }
  }

  @Test
  @WithMockCustomUser(id = USER_1_ID)
  fun `given invalid local pack when importing then returns bad request`() {
    val invalidPacks =
      listOf(
        """{"schemaVersion":2,"metadata":{"sourceName":"Bad","licenseName":"Test"},"entries":[{"term":"a","definition":"b"}]}""",
        """{"schemaVersion":1,"entries":[{"term":"a","definition":"b"}]}""",
        """{"schemaVersion":1,"metadata":{"sourceName":"Bad"},"entries":[{"term":"a","definition":"b"}]}""",
        """{"schemaVersion":1,"metadata":{"sourceName":"Bad","licenseName":"Test"},"entries":[]}""",
        """{"schemaVersion":1,"metadata":{"sourceName":"Bad","licenseName":"Test"},"entries":[{"term":" ","definition":"b"}]}""",
        """{"schemaVersion":1,"metadata":{"sourceName":"Bad","licenseName":"Test"},"entries":[{"term":"a","definition":" "}]}""",
      )

    invalidPacks.forEach { request ->
      mockMvc
        .post("/api/shelfview/v1/dictionary/providers/local-pack") {
          contentType = MediaType.APPLICATION_JSON
          content = request
        }.andExpect {
          status { isBadRequest() }
        }
    }
  }

  private fun localPackJson() =
    """
    {
      "schemaVersion": 1,
      "metadata": {
        "sourceName": "Test Dictionary",
        "sourceUrl": "https://example.test/dictionary",
        "licenseName": "First-party test data"
      },
      "entries": [
        {
          "term": "첫 번째",
          "definition": "서버에 저장된 뜻",
          "sourceName": "Entry Source"
        },
        {
          "term": "두 번째",
          "definition": "두 번째 뜻"
        }
      ]
    }
    """.trimIndent()

  private companion object {
    const val USER_1_ID = "shelfview-dictionary-user-1"
    const val USER_2_ID = "shelfview-dictionary-user-2"
  }
}
