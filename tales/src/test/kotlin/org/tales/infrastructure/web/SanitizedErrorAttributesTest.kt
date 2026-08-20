package org.tales.infrastructure.web

import jakarta.servlet.RequestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest

class SanitizedErrorAttributesTest {
  @Test
  fun `all requested internal details are omitted from an error dispatch`() {
    val request = MockHttpServletRequest("GET", "/api/private/$SENSITIVE_PATH")
    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500)
    request.setAttribute(RequestDispatcher.ERROR_MESSAGE, SENSITIVE_PARSER_MESSAGE)
    request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, IllegalStateException("$SENSITIVE_SQL $SENSITIVE_PATH"))
    val includeEverything = ErrorAttributeOptions.of(*ErrorAttributeOptions.Include.entries.toTypedArray())

    val attributes = SanitizedErrorAttributes().getErrorAttributes(ServletWebRequest(request), includeEverything)

    assertThat(attributes).containsExactlyEntriesOf(
      linkedMapOf(
        "status" to 500,
        "code" to "HTTP_500",
      ),
    )
    assertThat(attributes.toString()).doesNotContain(
      SENSITIVE_PATH,
      SENSITIVE_SQL,
      SENSITIVE_PARSER_MESSAGE,
      "message",
      "exception",
      "trace",
      "errors",
      "path",
    )
  }

  private companion object {
    const val SENSITIVE_PARSER_MESSAGE = "parser failed on private content"
    const val SENSITIVE_PATH = "G:\\private-media\\secret.cbz"
    const val SENSITIVE_SQL = "select password_hash from user_secret"
  }
}
