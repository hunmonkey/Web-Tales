package org.tales.interfaces.api.rest.dto

import org.tales.domain.model.PageHashUnknown

data class PageHashUnknownDto(
  val hash: String,
  val size: Long?,
  val matchCount: Int,
)

fun PageHashUnknown.toDto() =
  PageHashUnknownDto(
    hash = hash,
    size = size,
    matchCount = matchCount,
  )
