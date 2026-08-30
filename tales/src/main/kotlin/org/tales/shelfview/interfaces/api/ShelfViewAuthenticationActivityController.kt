package org.tales.shelfview.interfaces.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.tales.domain.persistence.AuthenticationActivityRepository
import java.time.LocalDateTime

@RestController
@RequestMapping("api/shelfview/v1/authentication-activity", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ShelfView")
class ShelfViewAuthenticationActivityController(
  private val authenticationActivityRepository: AuthenticationActivityRepository,
) {
  @Operation(summary = "Delete every user's authentication activity records")
  @DeleteMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteAll() {
    // Reuses the retention cleanup's repository method instead of adding a
    // deleteAll to the upstream repository. The bound is a day ahead, not
    // "now": LoginListener stamps records with local server time while the
    // daily cleanup compares in UTC, so a plain now() in either zone could
    // leave the newest records - the very ones being cleared - behind.
    authenticationActivityRepository.deleteOlderThan(LocalDateTime.now().plusDays(1))
  }
}
