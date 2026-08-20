package org.tales.interfaces.api.rest

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import org.tales.infrastructure.openapi.OpenApiConfiguration
import org.tales.infrastructure.security.TalesPrincipal
import org.tales.interfaces.api.rest.dto.GithubReleaseDto
import org.tales.interfaces.api.rest.dto.ReleaseDto
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

private const val GITHUB_API = "https://api.github.com/repos/hunmonkey/Web-Tales/releases"

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("api/v1/releases", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = OpenApiConfiguration.TagNames.RELEASES)
class ReleaseController(
  webClientBuilder: WebClient.Builder,
) {
  private val webClient = webClientBuilder.baseUrl(GITHUB_API).build()

  private val cache =
    Caffeine
      .newBuilder()
      .expireAfterAccess(1, TimeUnit.HOURS)
      .build<String, List<GithubReleaseDto>>()

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "List releases")
  fun getReleases(
    @AuthenticationPrincipal principal: TalesPrincipal,
  ): List<ReleaseDto> =
    cache
      .get("releases") { fetchGitHubReleases() }
      ?.let { releases ->
        releases.mapIndexed { index, ghRel ->
          ReleaseDto(
            ghRel.tagName,
            ghRel.publishedAt,
            ghRel.htmlUrl,
            index == 0,
            ghRel.prerelease,
            ghRel.body,
          )
        }
      }
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

  fun fetchGitHubReleases(): List<GithubReleaseDto> =
    try {
      val response =
        webClient
          .get()
          .uri {
            it.queryParam("per_page", 20).build()
          }.retrieve()
          .toEntity(object : ParameterizedTypeReference<List<GithubReleaseDto>>() {})
          .block()
      response?.body ?: emptyList()
    } catch (e: RuntimeException) {
      // the repository may have no releases, be private, or GitHub may be unreachable: none of those are server errors
      logger.warn { "Could not retrieve releases: errorType=${e.javaClass.simpleName}" }
      emptyList()
    }
}
