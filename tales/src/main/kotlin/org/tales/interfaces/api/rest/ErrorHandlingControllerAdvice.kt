package org.tales.interfaces.api.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.ConstraintViolationException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException
import org.tales.shelfview.infrastructure.archive.ArchiveLimitExceededException
import java.util.Locale

private val logger = KotlinLogging.logger {}

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class ErrorHandlingControllerAdvice {
  @ExceptionHandler(ArchiveLimitExceededException::class)
  @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
  @ResponseBody
  fun onArchiveLimitExceededException(
    e: ArchiveLimitExceededException,
  ): ErrorCodeResponse = ErrorCodeResponse(code = e.reason)

  @ExceptionHandler(ResponseStatusException::class)
  @ResponseBody
  fun onResponseStatusException(e: ResponseStatusException): ResponseEntity<ErrorCodeResponse> = errorResponse(e.statusCode.value(), e.headers)

  @ExceptionHandler(AccessDeniedException::class)
  @ResponseBody
  fun onAccessDeniedException(): ResponseEntity<ErrorCodeResponse> = errorResponse(HttpStatus.FORBIDDEN.value())

  @ExceptionHandler(AuthenticationException::class)
  @ResponseBody
  fun onAuthenticationException(): ResponseEntity<ErrorCodeResponse> = errorResponse(HttpStatus.UNAUTHORIZED.value())

  @ExceptionHandler(ConstraintViolationException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  fun onConstraintValidationException(
    e: ConstraintViolationException,
  ): ValidationErrorResponse =
    ValidationErrorResponse(
      e.constraintViolations.map {
        Violation(
          fieldName = it.propertyPath.toString(),
          code = validationCode(it.constraintDescriptor.annotation.annotationClass.simpleName),
        )
      },
    )

  @ExceptionHandler(MethodArgumentNotValidException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  fun onMethodArgumentNotValidException(
    e: MethodArgumentNotValidException,
  ): ValidationErrorResponse =
    ValidationErrorResponse(
      e.bindingResult.fieldErrors.map { Violation(fieldName = it.field, code = validationCode(it.code)) },
    )

  @ExceptionHandler(HttpMessageNotReadableException::class)
  @ResponseBody
  fun onHttpMessageNotReadableException(): ResponseEntity<ErrorCodeResponse> = errorResponse(HttpStatus.BAD_REQUEST.value())

  @ExceptionHandler(Exception::class)
  @ResponseBody
  fun onUnexpectedException(e: Exception): ResponseEntity<ErrorCodeResponse> {
    val frameworkError = e as? ErrorResponse
    val status = frameworkError?.statusCode?.value() ?: HttpStatus.INTERNAL_SERVER_ERROR.value()
    if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
      logger.error { "API request failed: status=$status, errorType=${e.javaClass.name}" }
    }
    return errorResponse(status, frameworkError?.headers ?: HttpHeaders.EMPTY)
  }
}

data class ErrorCodeResponse(
  val code: String,
)

data class ValidationErrorResponse(
  val violations: List<Violation> = emptyList(),
)

data class Violation(
  val fieldName: String? = null,
  val code: String,
)

private val validationCodePattern = Regex("[^A-Z0-9_]")

private fun httpErrorCode(status: Int): String = "HTTP_$status"

private fun errorResponse(
  status: Int,
  headers: HttpHeaders = HttpHeaders.EMPTY,
): ResponseEntity<ErrorCodeResponse> = ResponseEntity.status(status).headers(headers).body(ErrorCodeResponse(code = httpErrorCode(status)))

private fun validationCode(name: String?): String = "VALIDATION_${name.orEmpty().uppercase(Locale.ROOT).replace(validationCodePattern, "_").ifBlank { "INVALID" }}"
