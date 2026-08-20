package org.tales.interfaces.scheduler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.tales.captureLogs
import org.tales.domain.model.TalesUser
import org.tales.domain.service.TalesUserLifecycle

class InitialUserControllerTest {
  @Test
  fun `creating an initial user never logs its plaintext password`() {
    val userLifecycle = mockk<TalesUserLifecycle>()
    val initialUser = TalesUser(email = "admin@example.org", password = SENSITIVE_PASSWORD, id = "initial-user-id")
    every { userLifecycle.countUsers() } returns 0L
    every { userLifecycle.createUser(initialUser) } returns initialUser

    val logs =
      captureLogs(InitialUserController::class.java) {
        InitialUserController(userLifecycle, listOf(initialUser)).createInitialUserOnStartupIfNoneExist()
      }.rendered

    assertThat(logs)
      .contains("Initial user created")
      .doesNotContain(SENSITIVE_PASSWORD)
    verify(exactly = 1) { userLifecycle.createUser(initialUser) }
  }

  private companion object {
    const val SENSITIVE_PASSWORD = "SV1274_PLAINTEXT_INITIAL_PASSWORD"
  }
}
