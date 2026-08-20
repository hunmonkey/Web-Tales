package org.tales.interfaces.api.rest

import jakarta.servlet.RequestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.tales.infrastructure.web.SanitizedErrorAttributes

@SpringBootTest
@AutoConfigureMockMvc
class ErrorResponseConfigurationTest(
  @Autowired private val environment: Environment,
  @Autowired private val errorAttributes: ErrorAttributes,
  @Autowired private val mockMvc: MockMvc,
) {
  @Test
  fun `production defaults exclude internal error details`() {
    assertThat(environment.getProperty("server.error.include-exception")).isEqualTo("false")
    assertThat(environment.getProperty("server.error.include-message")).isEqualTo("never")
    assertThat(environment.getProperty("server.error.include-path")).isEqualTo("never")
    assertThat(environment.getProperty("server.error.include-stacktrace")).isEqualTo("never")
    assertThat(environment.getProperty("server.error.include-binding-errors")).isEqualTo("never")
    assertThat(errorAttributes).isInstanceOf(SanitizedErrorAttributes::class.java)
  }

  @Test
  fun `error endpoint omits details even when every query flag is requested`() {
    val response =
      mockMvc
        .perform(
          get("/error")
            .queryParam("message", "true")
            .queryParam("trace", "true")
            .queryParam("errors", "true")
            .queryParam("path", "true")
            .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
            .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, SENSITIVE_PATH)
            .requestAttr(RequestDispatcher.ERROR_MESSAGE, SENSITIVE_PARSER_MESSAGE)
            .requestAttr(RequestDispatcher.ERROR_EXCEPTION, IllegalStateException("$SENSITIVE_SQL $SENSITIVE_PATH"))
            .accept(MediaType.APPLICATION_JSON),
        ).andExpect(status().isInternalServerError)
        .andReturn()
        .response.contentAsString

    assertThat(response).isEqualTo("""{"status":500,"code":"HTTP_500"}""")
    assertThat(response).doesNotContain(
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
