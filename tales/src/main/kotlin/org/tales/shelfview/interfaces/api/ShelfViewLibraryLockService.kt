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

  private val failuresByLibrary = ConcurrentHashMap<String, Failures>()

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

  /** True when the PIN matches. Callers gate on [isThrottled] first. */
  fun unlock(
    libraryId: String,
    pin: String,
    session: HttpSession,
  ): Boolean {
    val hash = accessRepository.findPinHashOrNull(libraryId) ?: return false

    if (!passwordEncoder.matches(pin, hash)) {
      failuresByLibrary.compute(libraryId) { _, previous ->
        Failures((previous?.count ?: 0) + 1, System.currentTimeMillis())
      }
      return false
    }

    failuresByLibrary.remove(libraryId)
    session.setAttribute(SESSION_ATTRIBUTE, unlockedIds(session) + libraryId)
    return true
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
    failuresByLibrary.remove(libraryId)
    // The setter obviously knows the PIN; do not lock them out mid-session.
    session.setAttribute(SESSION_ATTRIBUTE, unlockedIds(session) + libraryId)
  }

  fun removePin(
    libraryId: String,
    session: HttpSession?,
  ) {
    accessRepository.deletePin(libraryId)
    failuresByLibrary.remove(libraryId)
    relock(libraryId, session)
  }

  fun isThrottled(libraryId: String): Boolean {
    val failures = failuresByLibrary[libraryId] ?: return false
    if (failures.count < THROTTLE_AFTER_FAILURES) return false
    if (System.currentTimeMillis() - failures.lastAt >= THROTTLE_WINDOW_MS) {
      failuresByLibrary.remove(libraryId)
      return false
    }
    return true
  }

  companion object {
    const val SESSION_ATTRIBUTE = "SHELFVIEW_UNLOCKED_LIBRARY_IDS"
    const val THROTTLE_AFTER_FAILURES = 5
    const val THROTTLE_WINDOW_MS = 30_000L
  }
}
