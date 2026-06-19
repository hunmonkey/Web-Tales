package org.gotson.komga.shelfview.interfaces.api.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.gotson.komga.language.toUTC
import org.gotson.komga.shelfview.domain.ShelfViewBookmark
import java.time.LocalDateTime

data class ShelfViewBookmarkDto(
  val id: String,
  val bookId: String,
  val locator: JsonNode,
  val label: String?,
  val note: String?,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val createdAt: LocalDateTime,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val updatedAt: LocalDateTime,
)

data class ShelfViewBookmarkUpdateDto(
  val locator: JsonNode?,
  val label: String? = null,
  val note: String? = null,
)

fun ShelfViewBookmark.toDto(mapper: ObjectMapper) =
  ShelfViewBookmarkDto(
    id = id,
    bookId = bookId,
    locator = mapper.readTree(locatorJson),
    label = label,
    note = note,
    createdAt = createdAt.toUTC(),
    updatedAt = updatedAt.toUTC(),
  )
