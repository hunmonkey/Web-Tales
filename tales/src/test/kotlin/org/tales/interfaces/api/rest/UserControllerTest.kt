package org.tales.interfaces.api.rest

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasItem
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.tales.domain.model.AgeRestriction
import org.tales.domain.model.AllowExclude
import org.tales.domain.model.AuthenticationActivity
import org.tales.domain.model.ContentRestrictions
import org.tales.domain.model.TalesUser
import org.tales.domain.model.UserRoles
import org.tales.domain.model.makeLibrary
import org.tales.domain.persistence.AuthenticationActivityRepository
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.TalesUserRepository
import org.tales.domain.service.LibraryLifecycle
import org.tales.domain.service.TalesUserLifecycle
import org.tales.interfaces.api.rest.dto.AllowExcludeDto
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@ActiveProfiles("test")
class UserControllerTest(
  @Autowired private val mockMvc: MockMvc,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val userRepository: TalesUserRepository,
  @Autowired private val authenticationActivityRepository: AuthenticationActivityRepository,
) {
  @Autowired
  private lateinit var userLifecycle: TalesUserLifecycle

  private val admin = TalesUser("admin@example.org", "", id = "admin")

  @BeforeAll
  fun setup() {
    libraryRepository.insert(makeLibrary(id = "1"))
    libraryRepository.insert(makeLibrary(id = "2"))
    userRepository.insert(admin)
  }

  @AfterAll
  fun teardown() {
    userRepository.findAll().forEach {
      userLifecycle.deleteUser(it)
    }
    libraryRepository.findAll().forEach {
      libraryLifecycle.deleteLibrary(it)
    }
  }

  @AfterEach
  fun deleteUsers() {
    userRepository
      .findAll()
      .filterNot { it.email == admin.email }
      .forEach { userLifecycle.deleteUser(it) }
  }

  @Nested
  inner class Create {
    @ParameterizedTest
    @ValueSource(strings = ["user", "user@domain"])
    @WithMockCustomUser(roles = ["ADMIN"])
    fun `when creating a user with invalid email then returns bad request`(email: String) {
      // language=JSON
      val jsonString = """{"email":"$email","password":"password"}"""

      mockMvc
        .post("/api/v2/users") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isBadRequest() }
        }
    }

    @Test
    @WithMockCustomUser(roles = ["ADMIN"])
    fun `when creating a user with limited fields then returns created user`() {
      // language=JSON
      val jsonString =
        """
          {
            "email":"newuser@example.org",
            "password":"password"
        }
        """.trimIndent()

      mockMvc
        .post("/api/v2/users") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isCreated() }
          jsonPath("$.roles") { value(hasItem("USER")) }
          jsonPath("$.sharedAllLibraries") { value(true) }
          jsonPath("$.sharedLibrariesId") { doesNotExist() }
          jsonPath("$.labelsAllow") { isEmpty() }
          jsonPath("$.labelsExclude") { isEmpty() }
          jsonPath("$.ageRestriction") { doesNotExist() }
        }
    }
  }

  @Test
  @WithMockCustomUser(roles = ["ADMIN"])
  fun `when creating a user with all fields then returns created user`() {
    // language=JSON
    val jsonString =
      """
      {
        "email":"newuser@example.org",
        "password":"password",
        "roles": ["${UserRoles.FILE_DOWNLOAD.name}"],
        "ageRestriction": {
          "age": 5,
          "restriction": "${AllowExcludeDto.NONE}"
        },
        "labelsAllow": ["allowTag"],
        "labelsExclude": ["excludeTag"],
        "sharedLibraries": {
          "all": "false",
          "libraryIds" : ["1", "157"]
        }
      }
      """.trimIndent()

    mockMvc
      .post("/api/v2/users") {
        contentType = MediaType.APPLICATION_JSON
        content = jsonString
      }.andExpect {
        status { isCreated() }
        jsonPath("$.roles") { value(allOf(hasItem("USER"), hasItem(UserRoles.FILE_DOWNLOAD.name))) }
        jsonPath("$.labelsAllow") { value(hasItem("allowtag")) }
        jsonPath("$.labelsExclude") { value(hasItem("excludetag")) }
        jsonPath("$.ageRestriction") { doesNotExist() }
        jsonPath("$.sharedAllLibraries") { value(false) }
        jsonPath("$.sharedLibrariesIds") { value(hasItem("1")) }
      }
  }

  @Test
  @WithMockCustomUser(roles = ["ADMIN"])
  fun `when creating a user with some fields then returns created user`() {
    // language=JSON
    val jsonString =
      """
      {
        "email":"newuser@example.org",
        "password":"password",
        "roles": ["${UserRoles.FILE_DOWNLOAD.name}"],
        "ageRestriction": {
          "age": 5,
          "restriction": "${AllowExcludeDto.ALLOW_ONLY}"
        },
        "labelsAllow": ["allowTag"],
        "labelsExclude": ["excludeTag"],
        "sharedLibraries": {
          "all": "false",
          "libraryIds" : ["1", "157"]
        }
      }
      """.trimIndent()

    mockMvc
      .post("/api/v2/users") {
        contentType = MediaType.APPLICATION_JSON
        content = jsonString
      }.andExpect {
        status { isCreated() }
        jsonPath("$.roles") { value(allOf(hasItem("USER"), hasItem(UserRoles.FILE_DOWNLOAD.name))) }
        jsonPath("$.labelsAllow") { value(hasItem("allowtag")) }
        jsonPath("$.labelsExclude") { value(hasItem("excludetag")) }
        jsonPath("$.ageRestriction.age") { value(5) }
        jsonPath("$.ageRestriction.restriction") { value(AllowExclude.ALLOW_ONLY.name) }
        jsonPath("$.sharedAllLibraries") { value(false) }
        jsonPath("$.sharedLibrariesIds") { value(hasItem("1")) }
      }
  }

  @Nested
  inner class Update {
    @Test
    @WithMockCustomUser(id = "admin", roles = ["ADMIN"])
    fun `given user without roles when updating roles then roles are updated`() {
      val user = TalesUser("user@example.org", "", id = "user")
      userLifecycle.createUser(user)

      // language=JSON
      val jsonString =
        """
        {
          "roles": ["${UserRoles.FILE_DOWNLOAD.name}","${UserRoles.PAGE_STREAMING.name}"]
        }
        """.trimIndent()

      mockMvc
        .patch("/api/v2/users/${user.id}") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isNoContent() }
        }

      with(userRepository.findByIdOrNull(user.id)) {
        assertThat(this).isNotNull
        assertThat(this!!.roles).containsExactlyInAnyOrder(UserRoles.PAGE_STREAMING, UserRoles.FILE_DOWNLOAD)
      }
    }

    @Test
    @WithMockCustomUser(id = "admin", roles = ["ADMIN"])
    fun `given user with roles when updating roles then roles are updated`() {
      val user = TalesUser("user@example.org", "", id = "user")
      userLifecycle.createUser(user)

      // language=JSON
      val jsonString =
        """
        {
          "roles": []
        }
        """.trimIndent()

      mockMvc
        .patch("/api/v2/users/${user.id}") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isNoContent() }
        }

      with(userRepository.findByIdOrNull(user.id)) {
        assertThat(this).isNotNull
        assertThat(this!!.roles).isEmpty()
      }
    }

    @Test
    @WithMockCustomUser(id = "admin", roles = ["ADMIN"])
    fun `given user with library restrictions when updating available libraries then they are updated`() {
      val user = TalesUser("user@example.org", "", sharedLibrariesIds = setOf("1"), sharedAllLibraries = false, id = "user")
      userLifecycle.createUser(user)

      // language=JSON
      val jsonString =
        """
        {
          "sharedLibraries": {
            "all": "false",
            "libraryIds" : ["1", "2"]
          }
        }
        """.trimIndent()

      mockMvc
        .patch("/api/v2/users/${user.id}") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isNoContent() }
        }

      with(userRepository.findByIdOrNull(user.id)) {
        assertThat(this).isNotNull
        assertThat(this!!.sharedAllLibraries).isFalse
        assertThat(this.sharedLibrariesIds).containsExactlyInAnyOrder("1", "2")
      }
    }

    @Test
    @WithMockCustomUser(id = "admin", roles = ["ADMIN"])
    fun `given user without library restrictions when restricting libraries then they restrictions are updated`() {
      val user = TalesUser("user@example.org", "", sharedAllLibraries = true, id = "user")
      userLifecycle.createUser(user)

      // language=JSON
      val jsonString =
        """
        {
          "sharedLibraries": {
            "all": "false",
            "libraryIds" : ["2"]
          }
        }
        """.trimIndent()

      mockMvc
        .patch("/api/v2/users/${user.id}") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isNoContent() }
        }

      with(userRepository.findByIdOrNull(user.id)) {
        assertThat(this).isNotNull
        assertThat(this!!.sharedAllLibraries).isFalse
        assertThat(this.sharedLibrariesIds).containsExactlyInAnyOrder("2")
      }
    }

    @Test
    @WithMockCustomUser(id = "admin", roles = ["ADMIN"])
    fun `given user with library restrictions when removing restrictions then the restrictions are updated`() {
      val user = TalesUser("user@example.org", "", sharedLibrariesIds = setOf("2"), sharedAllLibraries = false, id = "user")
      userLifecycle.createUser(user)

      // language=JSON
      val jsonString =
        """
        {
          "sharedLibraries": {
            "all": "true",
            "libraryIds": []
          }
        }
        """.trimIndent()

      mockMvc
        .patch("/api/v2/users/${user.id}") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isNoContent() }
        }

      with(userRepository.findByIdOrNull(user.id)) {
        assertThat(this).isNotNull
        assertThat(this!!.sharedAllLibraries).isTrue
        assertThat(this.sharedLibrariesIds).isEmpty()
      }
    }

    @Test
    @WithMockCustomUser(id = "admin", roles = ["ADMIN"])
    fun `given user without labels restrictions when adding restrictions then restrictions are updated`() {
      val user = TalesUser("user@example.org", "", id = "user")
      userLifecycle.createUser(user)

      // language=JSON
      val jsonString =
        """
        {
          "labelsAllow": ["cute", "kids"],
          "labelsExclude": ["adult"]
        }
        """.trimIndent()

      mockMvc
        .patch("/api/v2/users/${user.id}") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isNoContent() }
        }

      with(userRepository.findByIdOrNull(user.id)) {
        assertThat(this).isNotNull
        assertThat(this!!.restrictions.labelsAllow).containsExactlyInAnyOrder("cute", "kids")
        assertThat(this.restrictions.labelsExclude).containsOnly("adult")
      }
    }

    @Test
    @WithMockCustomUser(id = "admin", roles = ["ADMIN"])
    fun `given user with labels restrictions when removing restrictions then restrictions are updated`() {
      val user =
        TalesUser(
          "user@example.org",
          "",
          restrictions =
            ContentRestrictions(
              labelsAllow = setOf("kids", "cute"),
              labelsExclude = setOf("adult"),
            ),
          id = "user",
        )
      userLifecycle.createUser(user)

      // language=JSON
      val jsonString =
        """
        {
          "labelsAllow": [],
          "labelsExclude": null
        }
        """.trimIndent()

      mockMvc
        .patch("/api/v2/users/${user.id}") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isNoContent() }
        }

      with(userRepository.findByIdOrNull(user.id)) {
        assertThat(this).isNotNull
        assertThat(this!!.restrictions.labelsAllow).isEmpty()
        assertThat(this.restrictions.labelsExclude).isEmpty()
      }
    }

    @Test
    @WithMockCustomUser(id = "admin", roles = ["ADMIN"])
    fun `given user without age restriction when adding restrictions then restrictions are updated`() {
      val user = TalesUser("user@example.org", "", id = "user")
      userLifecycle.createUser(user)

      // language=JSON
      val jsonString =
        """
        {
          "ageRestriction": {
            "age": 12,
            "restriction": "ALLOW_ONLY"
          }
        }
        """.trimIndent()

      mockMvc
        .patch("/api/v2/users/${user.id}") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isNoContent() }
        }

      with(userRepository.findByIdOrNull(user.id)) {
        assertThat(this).isNotNull
        assertThat(this!!.restrictions.ageRestriction).isNotNull
        assertThat(this.restrictions.ageRestriction!!.age).isEqualTo(12)
        assertThat(this.restrictions.ageRestriction!!.restriction).isEqualTo(AllowExclude.ALLOW_ONLY)
      }
    }

    @Test
    @WithMockCustomUser(id = "admin", roles = ["ADMIN"])
    fun `given user without age restriction when adding incorrect restrictions then bad request`() {
      val user = TalesUser("user@example.org", "", id = "user")
      userLifecycle.createUser(user)

      // language=JSON
      val jsonString =
        """
        {
          "ageRestriction": {
            "age": -12,
            "restriction": "ALLOW_ONLY"
          }
        }
        """.trimIndent()

      mockMvc
        .patch("/api/v2/users/${user.id}") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isBadRequest() }
        }
    }

    @Test
    @WithMockCustomUser(id = "admin", roles = ["ADMIN"])
    fun `given user with age restriction when removing restriction then restrictions are updated`() {
      val user =
        TalesUser(
          "user@example.org",
          "",
          restrictions =
            ContentRestrictions(
              ageRestriction = AgeRestriction(12, AllowExclude.ALLOW_ONLY),
            ),
          id = "user",
        )
      userLifecycle.createUser(user)

      // language=JSON
      val jsonString =
        """
        {
          "ageRestriction": null
        }
        """.trimIndent()

      mockMvc
        .patch("/api/v2/users/${user.id}") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isNoContent() }
        }

      with(userRepository.findByIdOrNull(user.id)) {
        assertThat(this).isNotNull
        assertThat(this!!.restrictions.ageRestriction).isNull()
      }
    }

    @Test
    @WithMockCustomUser(id = "admin", roles = ["ADMIN"])
    fun `given user with age restriction when changing restriction then restrictions are updated`() {
      val user =
        TalesUser(
          "user@example.org",
          "",
          restrictions =
            ContentRestrictions(
              ageRestriction = AgeRestriction(12, AllowExclude.ALLOW_ONLY),
            ),
          id = "user",
        )
      userLifecycle.createUser(user)

      // language=JSON
      val jsonString =
        """
        {
          "ageRestriction": {
            "age": 16,
            "restriction": "EXCLUDE"
          }
        }
        """.trimIndent()

      mockMvc
        .patch("/api/v2/users/${user.id}") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isNoContent() }
        }

      with(userRepository.findByIdOrNull(user.id)) {
        assertThat(this).isNotNull
        assertThat(this!!.restrictions.ageRestriction).isNotNull
        assertThat(this.restrictions.ageRestriction!!.age).isEqualTo(16)
        assertThat(this.restrictions.ageRestriction!!.restriction).isEqualTo(AllowExclude.EXCLUDE)
      }
    }
  }

  @Nested
  inner class AuthenticationActivity {
    @Test
    @WithMockCustomUser(roles = ["ADMIN"])
    fun `given users when listing latest authentication activity then returns latest activity by user id`() {
      val user1 = userLifecycle.createUser(TalesUser("activity1@example.org", "", id = "activity1"))
      val user2 = userLifecycle.createUser(TalesUser("activity2@example.org", "", id = "activity2"))
      userLifecycle.createUser(TalesUser("activity3@example.org", "", id = "activity3"))

      authenticationActivityRepository.insert(
        AuthenticationActivity(
          userId = user1.id,
          email = user1.email,
          success = false,
          error = "older",
          dateTime = LocalDateTime.parse("2026-06-25T10:00:00"),
          source = "older",
        ),
      )
      authenticationActivityRepository.insert(
        AuthenticationActivity(
          userId = user1.id,
          email = user1.email,
          success = true,
          dateTime = LocalDateTime.parse("2026-06-25T11:00:00"),
          source = "latest",
        ),
      )
      authenticationActivityRepository.insert(
        AuthenticationActivity(
          email = user2.email,
          success = true,
          dateTime = LocalDateTime.parse("2026-06-25T12:00:00"),
          source = "email-only",
        ),
      )

      mockMvc
        .get("/api/v2/users/authentication-activity/latest-by-user")
        .andExpect {
          status { isOk() }
          jsonPath("$.activity1.success") { value(true) }
          jsonPath("$.activity1.source") { value("latest") }
          jsonPath("$.activity2.success") { value(true) }
          jsonPath("$.activity2.source") { value("email-only") }
          jsonPath("$.activity3") { doesNotExist() }
        }
    }

    @Test
    @WithMockCustomUser
    fun `given non admin user when listing latest authentication activity by user then returns forbidden`() {
      mockMvc
        .get("/api/v2/users/authentication-activity/latest-by-user")
        .andExpect {
          status { isForbidden() }
        }
    }
  }

  @Nested
  inner class ApiKey {
    @AfterEach
    fun cleanup() {
      userRepository.deleteApiKeyByUserId(admin.id)
    }

    @Test
    @WithMockCustomUser(id = "admin")
    fun `given user when creating API key then it is returned in plain text`() {
      // language=JSON
      val jsonString =
        """
        {
          "comment": "test api key"
        }
        """.trimIndent()

      mockMvc
        .post("/api/v2/users/me/api-keys") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isOk() }
          jsonPath("$.userId") { value(admin.id) }
          jsonPath("$.key") { value(MatchesPattern(Regex("""[^*]+""").toPattern())) }
          jsonPath("$.comment") { value("test api key") }
        }

      with(userRepository.findApiKeyByUserId(admin.id)) {
        assertThat(this).hasSize(1)
        with(this.first()) {
          assertThat(this.userId).isEqualTo(admin.id)
          assertThat(this.comment).isEqualTo("test api key")
        }
      }

      mockMvc
        .get("/api/v2/users/me/api-keys")
        .andExpect {
          status { isOk() }
          jsonPath("$.length()") { value(1) }
          jsonPath("$[0].userId") { value(admin.id) }
          jsonPath("$[0].key") { value(MatchesPattern(Regex("""[*]+""").toPattern())) }
          jsonPath("$[0].comment") { value("test api key") }
        }
    }

    @Test
    @WithMockCustomUser(id = "admin")
    fun `given user when creating API key without comment then returns bad request`() {
      // language=JSON
      val jsonString =
        """
        {
          "comment": ""
        }
        """.trimIndent()

      mockMvc
        .post("/api/v2/users/me/api-keys") {
          contentType = MediaType.APPLICATION_JSON
          content = jsonString
        }.andExpect {
          status { isBadRequest() }
        }
    }

    @Test
    @WithMockCustomUser(id = "admin")
    fun `given user with api key when deleting API key then it is deleted`() {
      val apiKey = userLifecycle.createApiKey(admin, "test")!!

      mockMvc
        .delete("/api/v2/users/me/api-keys/${apiKey.id}")
        .andExpect {
          status { isNoContent() }
        }

      assertThat(userRepository.findApiKeyByUserId(admin.id)).isEmpty()
    }

    @Test
    @WithMockCustomUser(id = "admin")
    fun `given user with api key when deleting different API key ID then returns bad request`() {
      userLifecycle.createApiKey(admin, "test")!!

      mockMvc
        .delete("/api/v2/users/me/api-keys/abc123")
        .andExpect {
          status { isNotFound() }
        }

      assertThat(userRepository.findApiKeyByUserId(admin.id)).isNotEmpty()
    }
  }
}
