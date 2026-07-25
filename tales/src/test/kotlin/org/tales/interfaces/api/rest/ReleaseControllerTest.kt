package org.tales.interfaces.api.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class ReleaseControllerTest {
  @Test
  fun `given GitHub returning not found when fetching releases then an empty list is returned`() {
    // given
    val controller = controllerRespondingWith(HttpStatus.NOT_FOUND, """{"message":"Not Found"}""")

    // when
    val releases = controller.fetchGitHubReleases()

    // then
    assertThat(releases).isEmpty()
  }

  @Test
  fun `given GitHub being unreachable when fetching releases then an empty list is returned`() {
    // given
    val webClientBuilder =
      WebClient
        .builder()
        .exchangeFunction { Mono.error(java.net.ConnectException("connection refused")) }
    val controller = ReleaseController(webClientBuilder)

    // when
    val releases = controller.fetchGitHubReleases()

    // then
    assertThat(releases).isEmpty()
  }

  @Test
  fun `given GitHub returning releases when fetching releases then they are returned`() {
    // given
    val body =
      """
      [{
        "html_url": "https://github.com/hunmonkey/Web-Tales/releases/tag/1.24.4",
        "tag_name": "1.24.4",
        "published_at": "2026-07-25T05:00:00Z",
        "body": "release notes",
        "prerelease": false
      }]
      """.trimIndent()
    val controller = controllerRespondingWith(HttpStatus.OK, body)

    // when
    val releases = controller.fetchGitHubReleases()

    // then
    assertThat(releases).hasSize(1)
    assertThat(releases.first().tagName).isEqualTo("1.24.4")
  }

  private fun controllerRespondingWith(
    status: HttpStatus,
    body: String,
  ): ReleaseController {
    val webClientBuilder =
      WebClient.builder().exchangeFunction {
        Mono.just(
          ClientResponse
            .create(status)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(body)
            .build(),
        )
      }
    return ReleaseController(webClientBuilder)
  }
}
