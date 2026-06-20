package org.gotson.komga.shelfview.interfaces.api.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.gotson.komga.language.toUTC
import org.gotson.komga.shelfview.domain.ShelfViewHighlight
import java.time.LocalDateTime

data class ShelfViewHighlightDto(
  val id: String,
  val bookId: String,
  val locator: JsonNode,
  val selector: JsonNode,
  val contentFingerprint: String,
  val color: String,
  val note: String?,
  val textQuote: String?,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val createdAt: LocalDateTime,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val updatedAt: LocalDateTime,
)

data class ShelfViewHighlightUpdateDto(
  val locator: JsonNode?,
  val selector: JsonNode?,
  val color: String? = null,
  val note: String? = null,
  val textQuote: String? = null,
)

fun ShelfViewHighlight.toDto(mapper: ObjectMapper) =
  ShelfViewHighlightDto(
    id = id,
    bookId = bookId,
    locator = mapper.readTree(locatorJson),
    selector = mapper.readTree(selectorJson),
    contentFingerprint = contentFingerprint,
    color = color,
    note = note,
    textQuote = textQuote,
    createdAt = createdAt.toUTC(),
    updatedAt = updatedAt.toUTC(),
  )
