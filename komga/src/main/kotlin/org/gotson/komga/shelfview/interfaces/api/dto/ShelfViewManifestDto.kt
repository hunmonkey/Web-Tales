package org.gotson.komga.shelfview.interfaces.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import org.gotson.komga.shelfview.domain.ShelfViewPublicationKind
import org.gotson.komga.shelfview.domain.ShelfViewSourceFormat

data class ShelfViewManifestDto(
  val schemaVersion: Int = 1,
  val bookId: String,
  val title: String,
  val kind: ShelfViewPublicationKind,
  val sourceFormat: ShelfViewSourceFormat,
  val contentFingerprint: String,
  val pageCount: Int? = null,
  val text: ShelfViewTextDto? = null,
  val pages: ShelfViewPagesDto? = null,
  val epub: ShelfViewEpubDto? = null,
  val items: List<ShelfViewManifestItemDto> = emptyList(),
  @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
  val contents: List<ShelfViewManifestContentsItemDto> = emptyList(),
  val capabilities: ShelfViewCapabilitiesDto,
)

data class ShelfViewManifestContentsItemDto(
  val label: String,
  val locator: JsonNode,
  val level: Int? = null,
)

data class ShelfViewManifestItemDto(
  val itemId: String,
  val index: Int,
  val kind: ShelfViewPublicationKind,
  val label: String,
  val contentFingerprint: String,
  val sourceFormat: ShelfViewSourceFormat? = null,
  val pageCount: Int? = null,
  val text: ShelfViewTextDto? = null,
  val pages: ShelfViewPagesDto? = null,
)

data class ShelfViewTextDto(
  val endpoint: String,
  val detectedEncoding: String? = null,
  val normalizationVersion: Int,
)

data class ShelfViewPagesDto(
  val endpointTemplate: String,
  val thumbnailEndpointTemplate: String? = null,
)

data class ShelfViewEpubDto(
  val webpubManifestEndpoint: String,
  val positionsEndpoint: String,
)

data class ShelfViewCapabilitiesDto(
  val search: Boolean,
  val textSelection: Boolean,
  val doublePage: Boolean,
  val continuousScroll: Boolean,
  val rtl: Boolean,
  val imageFilters: Boolean,
)
