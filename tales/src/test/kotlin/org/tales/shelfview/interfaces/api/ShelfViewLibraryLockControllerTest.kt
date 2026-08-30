package org.tales.shelfview.interfaces.api

import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterAll
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
import org.tales.domain.model.TalesUser
import org.tales.domain.model.UserRoles
import org.tales.domain.model.makeLibrary
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.TalesUserRepository
import org.tales.domain.service.LibraryLifecycle
import org.tales.interfaces.api.rest.WithMockCustomUser
import org.tales.shelfview.domain.persistence.ShelfViewLibraryAccessRepository

private const val ADMIN_ID = "shelfview-lock-admin"
private const val LIMITED_ID = "shelfview-lock-limited"

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ShelfViewLibraryLockControllerTest(
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val userRepository: TalesUserRepository,
  @Autowired private val accessRepository: ShelfViewLibraryAccessRepository,
  @Autowired private val mockMvc: MockMvc,
) {
  private val libraryOpen = makeLibrary(name = "Open", id = "shelfview-lock-open")
  private val libraryLocked = makeLibrary(name = "Locked", id = "shelfview-lock-locked")
  private val libraryThrottled = makeLibrary(name = "Throttled", id = "shelfview-lock-throttled")
  private val admin = TalesUser("shelfview-lock-admin@example.org", "", roles = setOf(UserRoles.ADMIN), id = ADMIN_ID)
  private val limitedUser =
    TalesUser(
      "shelfview-lock-limited@example.org",
      "",
      sharedAllLibraries = false,
      sharedLibrariesIds = setOf(libraryOpen.id),
      id = LIMITED_ID,
    )

  @BeforeAll
  fun `setup libraries and users`() {
    libraryRepository.insert(libraryOpen)
    libraryRepository.insert(libraryLocked)
    libraryRepository.insert(libraryThrottled)
    userRepository.insert(admin)
    userRepository.insert(limitedUser)
  }

  @AfterAll
  fun teardown() {
    accessRepository.deletePin(libraryLocked.id)
    accessRepository.deletePin(libraryThrottled.id)
    userRepository.delete(ADMIN_ID)
    userRepository.delete(LIMITED_ID)
    libraryRepository.findAll().forEach { libraryLifecycle.deleteLibrary(it) }
  }

  private fun pinJson(pin: String) = """{"pin":"$pin"}"""

  // Sessions are Spring Session entries keyed by the TALES-SESSION cookie, so
  // "the same browser session" in these tests means replaying that cookie.
  private fun sessionCookie(result: org.springframework.test.web.servlet.MvcResult): Cookie = requireNotNull(result.response.getCookie("TALES-SESSION")) { "response carries no session cookie" }

  private fun setPin(
    libraryId: String,
    pin: String,
  ): Cookie {
    val result =
      mockMvc
        .put("/api/shelfview/v1/library-locks/$libraryId") {
          contentType = MediaType.APPLICATION_JSON
          content = pinJson(pin)
        }.andExpect { status { isNoContent() } }
        .andReturn()
    return sessionCookie(result)
  }

  @Test
  @WithMockCustomUser(id = ADMIN_ID, roles = ["ADMIN"])
  fun `given a PIN when browsing without unlocking then the library is hidden even from the admin`() {
    setPin(libraryLocked.id, "1234")
    try {
      // A fresh session has unlocked nothing - the standard libraries API must
      // not list the locked library, and fetching it directly is forbidden.
      mockMvc
        .get("/api/v1/libraries")
        .andExpect {
          status { isOk() }
          jsonPath("$[?(@.id == '${libraryLocked.id}')]") { isEmpty() }
          jsonPath("$[?(@.id == '${libraryOpen.id}')]") { isNotEmpty() }
        }

      mockMvc
        .get("/api/v1/libraries/${libraryLocked.id}")
        .andExpect { status { isForbidden() } }

      // The lock list is where the name is allowed to show.
      mockMvc
        .get("/api/shelfview/v1/library-locks")
        .andExpect {
          status { isOk() }
          jsonPath("$.length()") { value(1) }
          jsonPath("$[0].libraryId") { value(libraryLocked.id) }
          jsonPath("$[0].name") { value("Locked") }
          jsonPath("$[0].unlocked") { value(false) }
        }
    } finally {
      accessRepository.deletePin(libraryLocked.id)
    }
  }

  @Test
  @WithMockCustomUser(id = ADMIN_ID, roles = ["ADMIN"])
  fun `given a PIN when unlocking then the library is visible for that session only until relocked`() {
    setPin(libraryLocked.id, "1234")
    try {
      mockMvc
        .post("/api/shelfview/v1/library-locks/${libraryLocked.id}/unlock") {
          contentType = MediaType.APPLICATION_JSON
          content = pinJson("wrong-pin")
        }.andExpect { status { isForbidden() } }

      val cookie =
        sessionCookie(
          mockMvc
            .post("/api/shelfview/v1/library-locks/${libraryLocked.id}/unlock") {
              contentType = MediaType.APPLICATION_JSON
              content = pinJson("1234")
            }.andExpect { status { isNoContent() } }
            .andReturn(),
        )

      mockMvc
        .get("/api/shelfview/v1/library-locks") { cookie(cookie) }
        .andExpect {
          status { isOk() }
          jsonPath("$[0].unlocked") { value(true) }
        }

      mockMvc
        .get("/api/v1/libraries") { cookie(cookie) }
        .andExpect {
          status { isOk() }
          jsonPath("$[?(@.id == '${libraryLocked.id}')]") { isNotEmpty() }
        }

      // Another session stays locked.
      mockMvc
        .get("/api/v1/libraries")
        .andExpect {
          status { isOk() }
          jsonPath("$[?(@.id == '${libraryLocked.id}')]") { isEmpty() }
        }

      mockMvc
        .delete("/api/shelfview/v1/library-locks/${libraryLocked.id}/unlock") { cookie(cookie) }
        .andExpect { status { isNoContent() } }

      mockMvc
        .get("/api/v1/libraries") { cookie(cookie) }
        .andExpect {
          status { isOk() }
          jsonPath("$[?(@.id == '${libraryLocked.id}')]") { isEmpty() }
        }
    } finally {
      accessRepository.deletePin(libraryLocked.id)
    }
  }

  @Test
  @WithMockCustomUser(id = ADMIN_ID, roles = ["ADMIN"])
  fun `given a PIN when setting it then the setter's own session is unlocked and removal reopens the library`() {
    val cookie = setPin(libraryLocked.id, "1234")
    try {
      mockMvc
        .get("/api/v1/libraries") { cookie(cookie) }
        .andExpect {
          status { isOk() }
          jsonPath("$[?(@.id == '${libraryLocked.id}')]") { isNotEmpty() }
        }

      mockMvc
        .delete("/api/shelfview/v1/library-locks/${libraryLocked.id}") { cookie(cookie) }
        .andExpect { status { isNoContent() } }

      // No PIN row any more: open for a completely fresh session too.
      mockMvc
        .get("/api/v1/libraries")
        .andExpect {
          status { isOk() }
          jsonPath("$[?(@.id == '${libraryLocked.id}')]") { isNotEmpty() }
        }
    } finally {
      accessRepository.deletePin(libraryLocked.id)
    }
  }

  @Test
  @WithMockCustomUser(id = LIMITED_ID, sharedAllLibraries = false, sharedLibraries = ["shelfview-lock-open"])
  fun `given a user without base access when unlocking then 404 reveals nothing`() {
    setPinAsRepository(libraryLocked.id)
    try {
      mockMvc
        .post("/api/shelfview/v1/library-locks/${libraryLocked.id}/unlock") {
          contentType = MediaType.APPLICATION_JSON
          content = pinJson("1234")
        }.andExpect { status { isNotFound() } }

      mockMvc
        .get("/api/shelfview/v1/library-locks")
        .andExpect {
          status { isOk() }
          jsonPath("$.length()") { value(0) }
        }
    } finally {
      accessRepository.deletePin(libraryLocked.id)
    }
  }

  @Test
  @WithMockCustomUser(id = ADMIN_ID, roles = ["ADMIN"])
  fun `given repeated wrong PINs when unlocking then attempts are throttled`() {
    setPin(libraryThrottled.id, "1234")
    try {
      repeat(5) {
        mockMvc
          .post("/api/shelfview/v1/library-locks/${libraryThrottled.id}/unlock") {
            contentType = MediaType.APPLICATION_JSON
            content = pinJson("wrong-pin")
          }.andExpect { status { isForbidden() } }
      }

      // Even the right PIN is rejected while throttled.
      mockMvc
        .post("/api/shelfview/v1/library-locks/${libraryThrottled.id}/unlock") {
          contentType = MediaType.APPLICATION_JSON
          content = pinJson("1234")
        }.andExpect { status { isTooManyRequests() } }
    } finally {
      accessRepository.deletePin(libraryThrottled.id)
    }
  }

  @Test
  @WithMockCustomUser(id = LIMITED_ID, sharedAllLibraries = false, sharedLibraries = ["shelfview-lock-open"])
  fun `given a non-admin user when setting or removing a PIN then it is denied`() {
    mockMvc
      .put("/api/shelfview/v1/library-locks/${libraryOpen.id}") {
        contentType = MediaType.APPLICATION_JSON
        content = pinJson("1234")
      }.andExpect { status { isForbidden() } }

    mockMvc
      .delete("/api/shelfview/v1/library-locks/${libraryOpen.id}")
      .andExpect { status { isForbidden() } }
  }

  @Test
  @WithMockCustomUser(id = ADMIN_ID, roles = ["ADMIN"])
  fun `given a short PIN when setting it then it is rejected`() {
    mockMvc
      .put("/api/shelfview/v1/library-locks/${libraryOpen.id}") {
        contentType = MediaType.APPLICATION_JSON
        content = pinJson("123")
      }.andExpect { status { isBadRequest() } }
  }

  // Direct repository write: the admin endpoint would unlock the writing
  // session, and this test needs a PIN the acting user has never unlocked.
  private fun setPinAsRepository(libraryId: String) {
    accessRepository.savePinHash(libraryId, "{noop}unused")
  }
}
