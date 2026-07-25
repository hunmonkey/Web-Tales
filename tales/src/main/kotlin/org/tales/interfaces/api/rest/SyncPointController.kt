package org.tales.interfaces.api.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.tales.domain.persistence.SyncPointRepository
import org.tales.infrastructure.openapi.OpenApiConfiguration
import org.tales.infrastructure.security.TalesPrincipal

@RestController
@RequestMapping("api/v1/syncpoints", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = OpenApiConfiguration.TagNames.SYNCPOINTS)
class SyncPointController(
  private val syncPointRepository: SyncPointRepository,
) {
  @DeleteMapping("me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete all sync points",
    description = "If an API Key ID is passed, deletes only the sync points associated with that API Key. Deleting sync points will allow a Kobo to sync from scratch upon the next sync.",
  )
  fun deleteSyncPointsForCurrentUser(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @RequestParam(required = false, name = "key_id") keyIds: Collection<String>?,
  ) {
    if (keyIds.isNullOrEmpty())
      syncPointRepository.deleteByUserId(principal.user.id)
    else
      syncPointRepository.deleteByUserIdAndApiKeyIds(principal.user.id, keyIds)
  }
}
