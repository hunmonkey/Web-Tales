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
    // deleteAll to the upstream repository. Records are stored in UTC, but
    // rows written before SV-1308 carry local server time, so the bound is a
    // day ahead of either zone rather than "now".
    authenticationActivityRepository.deleteOlderThan(LocalDateTime.now().plusDays(1))
  }
}
