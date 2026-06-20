package org.tales.interfaces.api.rest.dto

import org.tales.domain.model.ApiKey
import org.tales.language.toUTCZoned
import java.time.ZonedDateTime

data class ApiKeyDto(
  val id: String,
  val userId: String,
  val key: String,
  val comment: String,
  val createdDate: ZonedDateTime,
  val lastModifiedDate: ZonedDateTime,
)

fun ApiKey.toDto() =
  ApiKeyDto(
    id = id,
    userId = userId,
    key = key,
    comment = comment,
    createdDate = createdDate.toUTCZoned(),
    lastModifiedDate = createdDate.toUTCZoned(),
  )

fun ApiKeyDto.redacted() = copy(key = "*".repeat(6))
