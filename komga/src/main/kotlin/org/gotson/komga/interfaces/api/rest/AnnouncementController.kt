package org.gotson.komga.interfaces.api.rest

import com.github.benmanes.caffeine.cache.Caffeine
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.gotson.komga.domain.persistence.KomgaUserRepository
import org.gotson.komga.infrastructure.openapi.OpenApiConfiguration
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.interfaces.api.rest.dto.JsonFeedDto
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("api/v1/announcements", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = OpenApiConfiguration.TagNames.ANNOUNCEMENTS)
class AnnouncementController(
  private val userRepository: KomgaUserRepository,
) {
  private val cache =
    Caffeine
      .newBuilder()
      .expireAfterAccess(1, TimeUnit.HOURS)
      .build<String, JsonFeedDto?>()

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Retrieve announcements")
  fun getAnnouncements(
    @AuthenticationPrincipal principal: KomgaPrincipal,
  ): JsonFeedDto =
    cache
      .get("announcements") { fetchWebsiteAnnouncements() }
      ?.let { feed ->
        val read = userRepository.findAnnouncementIdsReadByUserId(principal.user.id)
        feed.copy(items = feed.items.map { item -> item.copy(talesExtension = JsonFeedDto.TalesExtensionDto(read.contains(item.id))) })
      }
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Mark announcements as read")
  fun markAnnouncementsRead(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestBody announcementIds: Set<String>,
  ) {
    userRepository.saveAnnouncementIdsRead(principal.user, announcementIds)
  }

  fun fetchWebsiteAnnouncements(): JsonFeedDto? =
    JsonFeedDto(
      version = "https://jsonfeed.org/version/1",
      title = "Tales announcements",
      homePageUrl = "https://github.com/hunmonkey/Web-Tales",
      description = "Latest Tales announcements",
      items = emptyList(),
    )
}
