package org.tales.shelfview.interfaces.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.tales.domain.model.AuthenticationActivity
import org.tales.domain.persistence.AuthenticationActivityRepository
import org.tales.interfaces.api.rest.WithMockCustomUser
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ShelfViewAuthenticationActivityControllerTest(
  @Autowired private val authenticationActivityRepository: AuthenticationActivityRepository,
  @Autowired private val mockMvc: MockMvc,
) {
  @Test
  @WithMockCustomUser(roles = ["ADMIN"])
  fun `given activity records when an admin deletes them then the table is emptied`() {
    authenticationActivityRepository.insert(
      AuthenticationActivity(email = "activity-delete@example.org", success = true, source = "Password"),
    )
    assertThat(authenticationActivityRepository.findAll(Pageable.unpaged()).totalElements).isGreaterThan(0)

    mockMvc
      .delete("/api/shelfview/v1/authentication-activity")
      .andExpect { status { isNoContent() } }

    assertThat(authenticationActivityRepository.findAll(Pageable.unpaged()).totalElements).isZero()
  }

  @Test
  @WithMockCustomUser
  fun `given a non-admin user when deleting activity then it is denied`() {
    authenticationActivityRepository.insert(
      AuthenticationActivity(email = "activity-keep@example.org", success = true, source = "Password"),
    )

    mockMvc
      .delete("/api/shelfview/v1/authentication-activity")
      .andExpect { status { isForbidden() } }

    assertThat(authenticationActivityRepository.findAll(Pageable.unpaged()).totalElements).isGreaterThan(0)

    // Leave the table as this test found it for the suites that share the DB.
    authenticationActivityRepository.deleteOlderThan(LocalDateTime.now().plusDays(1))
  }
}
