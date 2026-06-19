package org.gotson.komga.shelfview.interfaces.api.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.gotson.komga.language.toUTC
import org.gotson.komga.shelfview.domain.ShelfViewReaderPosition
import java.time.LocalDateTime

data class ShelfViewPositionDto(
  val locator: JsonNode,
  val progression: Float,
  val contentFingerprint: String,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val updatedAt: LocalDateTime,
)

data class ShelfViewPositionUpdateDto(
  val locator: JsonNode?,
  val progression: Float?,
)

fun ShelfViewReaderPosition.toDto(mapper: ObjectMapper) =
  ShelfViewPositionDto(
    locator = mapper.readTree(locatorJson),
    progression = progression,
    contentFingerprint = contentFingerprint,
    updatedAt = updatedAt.toUTC(),
  )
