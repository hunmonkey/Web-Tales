package org.tales.shelfview.interfaces.api

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.crypto.password.PasswordEncoder
import org.tales.shelfview.domain.persistence.ShelfViewLibraryAccessRepository
import org.tales.shelfview.interfaces.api.ShelfViewLibraryLockService.UnlockResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ShelfViewLibraryLockServiceTest {
  @Test
  fun `given parallel wrong PINs when unlocking then only the free attempts reach the PIN comparison`() {
    val accessRepository = mockk<ShelfViewLibraryAccessRepository> { every { findPinHashOrNull("library") } returns "hash" }
    val comparisons = AtomicInteger()
    // As slow as a bcrypt comparison, which is the window parallel requests used.
    val slowEncoder =
      object : PasswordEncoder {
        override fun encode(rawPassword: CharSequence?) = "hash"

        override fun matches(
          rawPassword: CharSequence?,
          encodedPassword: String?,
        ): Boolean {
          comparisons.incrementAndGet()
          Thread.sleep(200)
          return false
        }
      }
    val service = ShelfViewLibraryLockService(accessRepository, mockk(), slowEncoder)
    val start = CountDownLatch(1)
    val pool = Executors.newFixedThreadPool(20)

    try {
      val attempts =
        List(20) {
          pool.submit<UnlockResult> {
            start.await()
            service.unlock("library", "0000", MockHttpSession(), "user")
          }
        }
      start.countDown()
      val results = attempts.map { it.get(10, TimeUnit.SECONDS) }

      assertThat(comparisons.get()).isEqualTo(ShelfViewLibraryLockService.THROTTLE_AFTER_FAILURES)
      assertThat(results.count { it == UnlockResult.THROTTLED }).isEqualTo(20 - ShelfViewLibraryLockService.THROTTLE_AFTER_FAILURES)
    } finally {
      pool.shutdownNow()
    }
  }
}
