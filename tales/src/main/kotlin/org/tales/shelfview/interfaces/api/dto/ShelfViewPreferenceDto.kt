package org.tales.shelfview.interfaces.api.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.tales.language.toUTC
import org.tales.shelfview.domain.ShelfViewReaderPreference
import java.time.LocalDateTime

data class ShelfViewPreferenceDto(
  val scopeType: String,
  val scopeId: String,
  val format: String,
  val settings: JsonNode,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val updatedAt: LocalDateTime,
)

data class ShelfViewPreferenceResponseDto(
  val scopeType: String,
  val scopeId: String,
  val format: String,
  val settings: JsonNode?,
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
  val updatedAt: LocalDateTime?,
  val effectiveSettings: JsonNode,
  val sources: List<ShelfViewPreferenceDto>,
)

data class ShelfViewPreferenceUpdateDto(
  val scopeType: String?,
  val scopeId: String?,
  val format: String?,
  val settings: JsonNode?,
)

fun ShelfViewReaderPreference.toDto(mapper: ObjectMapper) =
  ShelfViewPreferenceDto(
    scopeType = scopeType,
    scopeId = scopeId,
    format = format,
    settings = mapper.readTree(settingsJson),
    updatedAt = updatedAt.toUTC(),
  )
