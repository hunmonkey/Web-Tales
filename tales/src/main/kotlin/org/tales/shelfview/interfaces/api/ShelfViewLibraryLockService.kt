package org.tales.shelfview.interfaces.api

import jakarta.servlet.http.HttpSession
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.tales.domain.model.TalesUser
import org.tales.domain.persistence.LibraryRepository
import org.tales.shelfview.domain.persistence.ShelfViewLibraryAccessRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Session-scoped library PIN locks (ADR-0022). A library with a PIN is served
 * to a session as if the user had no access to it, until the session unlocks
 * it; the unlock lives in the HTTP session, so logout or expiry relocks.
 */
@Component
class ShelfViewLibraryLockService(
  private val accessRepository: ShelfViewLibraryAccessRepository,
  private val libraryRepository: LibraryRepository,
  private val passwordEncoder: PasswordEncoder,
) {
  private data class Failures(
    val count: Int,
    val lastAt: Long,
  )

  // Keyed per user and library (SV-1300): one person's wrong guesses must not
  // lock everyone else out, and a fresh session must not buy fresh attempts.
  private val failuresByUserAndLibrary = ConcurrentHashMap<Pair<String, String>, Failures>()

  fun unlockedIds(session: HttpSession?): Set<String> {
    @Suppress("UNCHECKED_CAST")
    return session?.getAttribute(SESSION_ATTRIBUTE) as? Set<String> ?: emptySet()
  }

  fun lockedIdsFor(session: HttpSession?): Set<String> = accessRepository.findLockedLibraryIds() - unlockedIds(session)

  /**
   * The user this request is served as: identical when nothing is locked,
   * otherwise a copy whose authorized libraries exclude the locked ones. The
   * authorized set is materialized for all-access users because a null filter
   * means "everything" throughout the query layer.
   */
  fun effectiveUser(
    user: TalesUser,
    session: HttpSession?,
  ): TalesUser {
    val locked = lockedIdsFor(session)
    if (locked.isEmpty()) return user

    val base =
      if (user.canAccessAllLibraries())
        libraryRepository.findAll().map { it.id }.toSet()
      else
        user.sharedLibrariesIds

    return user.copy(
      sharedAllLibraries = false,
      sharedLibrariesIds = base - locked,
      lockedLibraryIds = locked,
    )
  }

  /**
   * After [THROTTLE_AFTER_FAILURES] wrong guesses the user waits
   * [THROTTLE_BASE_MS], and every further wrong guess doubles the wait up to
   * [THROTTLE_MAX_MS]. The count only resets on a correct PIN or a PIN change,
   * so waiting out the window never buys a fresh batch of attempts.
   *
   * The attempt is counted before the PIN is compared, in the same atomic step
   * as the wait check: bcrypt takes long enough that parallel requests would
   * otherwise all pass the check before the first failure was recorded.
   */
  fun unlock(
    libraryId: String,
    pin: String,
    session: HttpSession,
    userId: String,
  ): UnlockResult {
    val hash = accessRepository.findPinHashOrNull(libraryId) ?: return UnlockResult.WRONG_PIN

    var waiting = false
    failuresByUserAndLibrary.compute(userId to libraryId) { _, previous ->
      if (previous != null && isWaiting(previous)) {
        waiting = true
        previous
      } else {
        Failures((previous?.count ?: 0) + 1, System.currentTimeMillis())
      }
    }
    if (waiting) return UnlockResult.THROTTLED
    if (!passwordEncoder.matches(pin, hash)) return UnlockResult.WRONG_PIN

    failuresByUserAndLibrary.remove(userId to libraryId)
    session.setAttribute(SESSION_ATTRIBUTE, unlockedIds(session) + libraryId)
    return UnlockResult.UNLOCKED
  }

  fun relock(
    libraryId: String,
    session: HttpSession?,
  ) {
    session?.setAttribute(SESSION_ATTRIBUTE, unlockedIds(session) - libraryId)
  }

  fun setPin(
    libraryId: String,
    pin: String,
    session: HttpSession,
  ) {
    accessRepository.savePinHash(libraryId, passwordEncoder.encode(pin))
    forgetFailures(libraryId)
    // The setter obviously knows the PIN; do not lock them out mid-session.
    session.setAttribute(SESSION_ATTRIBUTE, unlockedIds(session) + libraryId)
  }

  fun removePin(
    libraryId: String,
    session: HttpSession?,
  ) {
    accessRepository.deletePin(libraryId)
    forgetFailures(libraryId)
    relock(libraryId, session)
  }

  private fun isWaiting(failures: Failures): Boolean = failures.count >= THROTTLE_AFTER_FAILURES && System.currentTimeMillis() - failures.lastAt < lockoutMs(failures.count)

  private fun lockoutMs(count: Int): Long = (THROTTLE_BASE_MS shl minOf(count - THROTTLE_AFTER_FAILURES, 7)).coerceAtMost(THROTTLE_MAX_MS)

  private fun forgetFailures(libraryId: String) {
    failuresByUserAndLibrary.keys.removeIf { it.second == libraryId }
  }

  enum class UnlockResult { UNLOCKED, WRONG_PIN, THROTTLED }

  companion object {
    const val SESSION_ATTRIBUTE = "SHELFVIEW_UNLOCKED_LIBRARY_IDS"
    const val THROTTLE_AFTER_FAILURES = 5
    const val THROTTLE_BASE_MS = 30_000L
    const val THROTTLE_MAX_MS = 3_600_000L
  }
}
