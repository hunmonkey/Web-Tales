package org.tales.interfaces.api.rest.dto

import org.tales.domain.model.PageHashKnown
import org.tales.language.toUTC
import java.time.LocalDateTime

data class PageHashKnownDto(
  val hash: String,
  val size: Long?,
  val action: PageHashKnown.Action,
  val deleteCount: Int,
  val matchCount: Int,
  val created: LocalDateTime,
  val lastModified: LocalDateTime,
)

fun PageHashKnown.toDto() =
  PageHashKnownDto(
    hash = hash,
    size = size,
    action = action,
    deleteCount = deleteCount,
    matchCount = matchCount,
    created = createdDate.toUTC(),
    lastModified = lastModifiedDate.toUTC(),
  )
