package org.tales.infrastructure.web

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.stereotype.Component
import org.springframework.web.context.request.WebRequest

@Component
class SanitizedErrorAttributes : DefaultErrorAttributes() {
  override fun getErrorAttributes(
    webRequest: WebRequest,
    options: ErrorAttributeOptions,
  ): Map<String, Any> {
    val status =
      super
        .getErrorAttributes(webRequest, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STATUS))["status"]
        ?: 500
    return linkedMapOf(
      "status" to status,
      "code" to "HTTP_$status",
    )
  }
}
