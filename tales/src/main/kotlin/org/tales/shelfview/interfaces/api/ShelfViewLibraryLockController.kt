package org.tales.shelfview.interfaces.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.model.TalesUser
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.TalesUserRepository
import org.tales.infrastructure.security.TalesPrincipal
import org.tales.shelfview.domain.persistence.ShelfViewLibraryAccessRepository
import org.tales.shelfview.interfaces.api.dto.ShelfViewLibraryLockDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewLibraryPinDto

/**
 * Library PIN locks (ADR-0022). The lock-state list and the unlock exchange
 * are the only places a locked library is allowed to show its name; every
 * content endpoint hides it through ShelfViewLibraryLockFilter.
 */
@RestController
@RequestMapping("api/shelfview/v1/library-locks", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ShelfView")
class ShelfViewLibraryLockController(
  private val lockService: ShelfViewLibraryLockService,
  private val accessRepository: ShelfViewLibraryAccessRepository,
  private val libraryRepository: LibraryRepository,
  private val userRepository: TalesUserRepository,
) {
  @Operation(summary = "List PIN-locked libraries visible to this user, with this session's unlock state")
  @GetMapping
  fun getLockStates(
    @AuthenticationPrincipal principal: TalesPrincipal,
    session: HttpSession,
  ): List<ShelfViewLibraryLockDto> {
    val baseUser = baseUser(principal)
    val unlocked = lockService.unlockedIds(session)

    return accessRepository
      .findLockedLibraryIds()
      .filter { baseUser.canAccessLibrary(it) }
      .mapNotNull { libraryId -> libraryRepository.findByIdOrNull(libraryId) }
      .sortedBy { it.name.lowercase() }
      .map { ShelfViewLibraryLockDto(it.id, it.name, it.id in unlocked) }
  }

  @Operation(summary = "Unlock a PIN-locked library for this session")
  @PostMapping("{libraryId}/unlock", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun unlock(
    @PathVariable libraryId: String,
    @Valid @RequestBody pinDto: ShelfViewLibraryPinDto,
    @AuthenticationPrincipal principal: TalesPrincipal,
    session: HttpSession,
  ) {
    // 404 for both "no such lock" and "not yours to see": a lock row must not
    // reveal a library the sharing rules already hide from this user.
    if (!baseUser(principal).canAccessLibrary(libraryId)) throw ResponseStatusException(HttpStatus.NOT_FOUND)
    if (accessRepository.findPinHashOrNull(libraryId) == null) throw ResponseStatusException(HttpStatus.NOT_FOUND)

    if (lockService.isThrottled(libraryId, principal.user.id)) throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "SHELFVIEW_LOCK_THROTTLED")
    if (!lockService.unlock(libraryId, pinDto.pin, session, principal.user.id)) throw ResponseStatusException(HttpStatus.FORBIDDEN, "SHELFVIEW_LOCK_PIN_MISMATCH")
  }

  @Operation(summary = "Relock a library for this session")
  @DeleteMapping("{libraryId}/unlock")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun relock(
    @PathVariable libraryId: String,
    session: HttpSession,
  ) {
    lockService.relock(libraryId, session)
  }

  @Operation(summary = "Set or replace a library's PIN")
  @PutMapping("{libraryId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun setPin(
    @PathVariable libraryId: String,
    @Valid @RequestBody pinDto: ShelfViewLibraryPinDto,
    session: HttpSession,
  ) {
    libraryRepository.findByIdOrNull(libraryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    lockService.setPin(libraryId, pinDto.pin, session)
  }

  @Operation(summary = "Remove a library's PIN, making it open again")
  @DeleteMapping("{libraryId}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun removePin(
    @PathVariable libraryId: String,
    session: HttpSession,
  ) {
    lockService.removePin(libraryId, session)
  }

  // The request principal already has the locks applied; base access (the
  // sharing rules alone) comes from the stored user.
  private fun baseUser(principal: TalesPrincipal): TalesUser = userRepository.findByIdOrNull(principal.user.id) ?: principal.user
}
