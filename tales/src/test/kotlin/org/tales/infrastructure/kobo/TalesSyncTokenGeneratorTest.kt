package org.tales.infrastructure.kobo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.tales.domain.model.TalesSyncToken
import java.util.Base64

@SpringBootTest
class TalesSyncTokenGeneratorTest(
  @Autowired private val tokenGenerator: TalesSyncTokenGenerator,
) {
  private val base64Encoder by lazy { Base64.getEncoder().withoutPadding() }

  fun encodeToBase64(token: String): String = base64Encoder.encodeToString(token.toByteArray())

  @Test
  fun `given Kobo store token when getting token then it contains the kobo store token`() {
    // given
    val koboToken = "fake.token"

    // when
    val talesToken = tokenGenerator.fromBase64(koboToken)

    // then
    assertThat(talesToken.rawKoboSyncToken).isEqualTo(koboToken)
  }

  @Test
  fun `given calibre web token when getting token then it contains the kobo store token`() {
    // given
    val calibreWebToken =
      """
      {
        "data": {
          "archive_last_modified": -62135596800.0,
          "books_last_created": -62135596800.0,
          "books_last_modified": -62135596800.0,
          "raw_kobo_store_token": "fake.token",
          "reading_state_last_modified": -62135596800.0,
          "tags_last_modified": -62135596800.0
        },
        "version": "1-1-0"
      }
      """.trimIndent()

    // when
    val talesToken = tokenGenerator.fromBase64(encodeToBase64(calibreWebToken))

    // then
    assertThat(talesToken.rawKoboSyncToken).isEqualTo("fake.token")
  }

  @Test
  fun `given Tales token when getting token then it contains the kobo store token`() {
    // given
    val talesToken = TalesSyncToken(rawKoboSyncToken = "fake.token")

    // when
    val encodedToken = tokenGenerator.toBase64(talesToken)
    val decodedToken = tokenGenerator.fromBase64(encodedToken)

    // then
    assertThat(decodedToken.rawKoboSyncToken).isEqualTo("fake.token")
    assertThat(encodedToken).startsWith("TALES.")
  }

  @Test
  fun `given unidentified token when getting token then it is empty`() {
    // given
    val token = "unrecognized"

    // when
    val talesToken = tokenGenerator.fromBase64(token)

    // then
    assertThat(talesToken.rawKoboSyncToken).isEmpty()
  }
}
