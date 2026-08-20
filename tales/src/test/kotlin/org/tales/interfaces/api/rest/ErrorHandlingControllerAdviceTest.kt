package org.tales.interfaces.api.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.core.MethodParameter
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.tales.shelfview.infrastructure.archive.ArchiveLimitExceededException

@ExtendWith(OutputCaptureExtension::class)
class ErrorHandlingControllerAdviceTest {
  private val advice = ErrorHandlingControllerAdvice()
  private val mockMvc: MockMvc =
    MockMvcBuilders
      .standaloneSetup(SensitiveErrorTestController())
      .setControllerAdvice(advice)
      .build()

  @Test
  fun `archive limit maps to 413 without leaking archive details`() {
    val response =
      mockMvc
        .perform(get(TEST_PATH).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isPayloadTooLarge)
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andReturn()
        .response

    assertThat(response.contentAsString).isEqualTo("""{"code":"$ERROR_CODE"}""")
    assertThat(response.contentAsString).doesNotContain(
      SENSITIVE_ARCHIVE_NAME,
      SENSITIVE_ENTRY_NAME,
      SENSITIVE_CAUSE,
      ArchiveLimitExceededException::class.java.name,
    )
  }

  @Test
  fun `response status reason is not treated as a public code`() {
    mockMvc
      .perform(get(STABLE_STATUS_PATH).accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isPayloadTooLarge)
      .andExpect(content().json("""{"code":"HTTP_413"}"""))
  }

  @Test
  fun `response status message is replaced with an HTTP code`() {
    val body =
      mockMvc
        .perform(get(SENSITIVE_STATUS_PATH).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest)
        .andReturn()
        .response.contentAsString

    assertThat(body).isEqualTo("""{"code":"HTTP_400"}""")
    assertNoSensitiveDetails(body)
  }

  @Test
  fun `unexpected exception returns and logs only safe metadata`(output: CapturedOutput) {
    val body =
      mockMvc
        .perform(get(UNEXPECTED_PATH).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError)
        .andReturn()
        .response.contentAsString

    assertThat(body).isEqualTo("""{"code":"HTTP_500"}""")
    assertNoSensitiveDetails(body)
    val applicationErrorLog = output.all.lineSequence().single { it.contains("API request failed:") }
    assertNoSensitiveDetails(applicationErrorLog)
    assertThat(applicationErrorLog).contains("errorType=java.lang.IllegalStateException")
  }

  @Test
  fun `security exceptions retain their HTTP status without exposing messages`() {
    val forbiddenBody =
      mockMvc
        .perform(get(FORBIDDEN_PATH).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden)
        .andReturn()
        .response.contentAsString
    val unauthenticatedBody =
      mockMvc
        .perform(get(UNAUTHENTICATED_PATH).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized)
        .andReturn()
        .response.contentAsString

    assertThat(forbiddenBody).isEqualTo("""{"code":"HTTP_403"}""")
    assertThat(unauthenticatedBody).isEqualTo("""{"code":"HTTP_401"}""")
    assertNoSensitiveDetails(forbiddenBody + unauthenticatedBody)
  }

  @Test
  fun `framework error response retains its status without exposing resource details`() {
    val body =
      mockMvc
        .perform(get(MISSING_RESOURCE_PATH).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound)
        .andReturn()
        .response.contentAsString

    assertThat(body).isEqualTo("""{"code":"HTTP_404"}""")
    assertNoSensitiveDetails(body)
  }

  @Test
  fun `framework error response retains safe protocol headers`() {
    mockMvc
      .perform(get(METHOD_NOT_ALLOWED_PATH).accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isMethodNotAllowed)
      .andExpect { result -> assertThat(result.response.getHeader("Allow")).isEqualTo("GET") }
      .andExpect(content().json("""{"code":"HTTP_405"}"""))
  }

  @Test
  fun `unreadable request body remains a sanitized client error`() {
    mockMvc
      .perform(
        post(UNREADABLE_BODY_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .content("""{"value":"unterminated}"""),
      ).andExpect(status().isBadRequest)
      .andExpect(content().json("""{"code":"HTTP_400"}"""))
  }

  @Test
  fun `validation response uses a stable constraint code instead of its message`() {
    val bindingResult = BeanPropertyBindingResult(Any(), "request")
    bindingResult.addError(
      FieldError(
        "request",
        "name",
        null,
        false,
        arrayOf("NotBlank"),
        null,
        SENSITIVE_PARSER_MESSAGE,
      ),
    )

    val validationParameter =
      MethodParameter(
        SensitiveErrorTestController::class.java.getDeclaredMethod("validated", String::class.java),
        0,
      )
    val response = advice.onMethodArgumentNotValidException(MethodArgumentNotValidException(validationParameter, bindingResult))

    val json = jacksonObjectMapper().writeValueAsString(response)

    assertThat(json).isEqualTo("""{"violations":[{"fieldName":"name","code":"VALIDATION_NOTBLANK"}]}""")
    assertNoSensitiveDetails(json)
  }

  private fun assertNoSensitiveDetails(value: String) {
    assertThat(value).doesNotContain(
      SENSITIVE_ARCHIVE_NAME,
      SENSITIVE_ENTRY_NAME,
      SENSITIVE_CAUSE,
      SENSITIVE_BOOK,
      SENSITIVE_SQL,
      SENSITIVE_PARSER_MESSAGE,
      "\"message\"",
      "\"exception\"",
      "\"trace\"",
    )
  }

  @RestController
  private class SensitiveErrorTestController {
    @GetMapping(TEST_PATH)
    fun fail(): Nothing =
      throw ArchiveLimitExceededException(ERROR_CODE).apply {
        initCause(IllegalStateException(SENSITIVE_CAUSE))
      }

    @GetMapping(STABLE_STATUS_PATH)
    fun stableStatus(): Nothing = throw ResponseStatusException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_TEXT_TOO_LARGE")

    @GetMapping(SENSITIVE_STATUS_PATH)
    fun sensitiveStatus(): Nothing =
      throw ResponseStatusException(
        org.springframework.http.HttpStatus.BAD_REQUEST,
        "$SENSITIVE_BOOK $SENSITIVE_PARSER_MESSAGE",
      )

    @GetMapping(UNEXPECTED_PATH)
    fun unexpected(): Nothing = throw IllegalStateException("$SENSITIVE_SQL $SENSITIVE_CAUSE")

    @GetMapping(FORBIDDEN_PATH)
    fun forbidden(): Nothing = throw AccessDeniedException(SENSITIVE_CAUSE)

    @GetMapping(UNAUTHENTICATED_PATH)
    fun unauthenticated(): Nothing = throw BadCredentialsException(SENSITIVE_SQL)

    @GetMapping(MISSING_RESOURCE_PATH)
    fun missingResource(): Nothing = throw NoResourceFoundException(HttpMethod.GET, SENSITIVE_CAUSE)

    @GetMapping(METHOD_NOT_ALLOWED_PATH)
    fun methodNotAllowed(): Nothing = throw HttpRequestMethodNotSupportedException("POST", listOf("GET"))

    @PostMapping(UNREADABLE_BODY_PATH)
    fun unreadableBody(
      @RequestBody request: UnreadableRequest,
    ): String = request.value

    fun validated(value: String): String = value
  }

  private data class UnreadableRequest(
    val value: String,
  )

  private companion object {
    const val ERROR_CODE = "SHELFVIEW_ARCHIVE_ENTRY_TOO_LARGE"
    const val FORBIDDEN_PATH = "/forbidden-error-test"
    const val MISSING_RESOURCE_PATH = "/missing-resource-error-test"
    const val METHOD_NOT_ALLOWED_PATH = "/method-not-allowed-error-test"
    const val SENSITIVE_ARCHIVE_NAME = "private-library.cbz"
    const val SENSITIVE_ENTRY_NAME = "private-entry.jpg"
    const val SENSITIVE_CAUSE = "G:\\private-media\\private-library.cbz!private-entry.jpg"
    const val SENSITIVE_BOOK = "Book(url=file:/private-media/private-library.cbz, fileHash=secret)"
    const val SENSITIVE_SQL = "select password_hash from user_secret"
    const val SENSITIVE_PARSER_MESSAGE = "parser failed near private-entry.jpg"
    const val SENSITIVE_STATUS_PATH = "/sensitive-status-test"
    const val STABLE_STATUS_PATH = "/stable-status-test"
    const val TEST_PATH = "/archive-limit-test/$SENSITIVE_ARCHIVE_NAME/$SENSITIVE_ENTRY_NAME"
    const val UNAUTHENTICATED_PATH = "/unauthenticated-error-test"
    const val UNREADABLE_BODY_PATH = "/unreadable-body-error-test"
    const val UNEXPECTED_PATH = "/unexpected-error-test"
  }
}
