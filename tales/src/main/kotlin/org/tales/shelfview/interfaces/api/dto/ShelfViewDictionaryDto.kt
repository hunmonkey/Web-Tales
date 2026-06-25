package org.tales.shelfview.interfaces.api.dto

import org.tales.shelfview.domain.ShelfViewDictionaryLookupEntry
import org.tales.shelfview.domain.ShelfViewDictionaryProviderSummary
import java.time.LocalDateTime

data class ShelfViewDictionaryPackMetadataDto(
  val sourceName: String? = null,
  val sourceUrl: String? = null,
  val retrievedAt: String? = null,
  val licenseName: String? = null,
  val licenseUrl: String? = null,
  val attribution: String? = null,
  val redistributionNotes: String? = null,
)

data class ShelfViewDictionaryEntryImportDto(
  val term: String? = null,
  val definition: String? = null,
  val sourceName: String? = null,
  val sourceUrl: String? = null,
  val licenseName: String? = null,
)

data class ShelfViewDictionaryLocalPackImportDto(
  val schemaVersion: Int? = null,
  val label: String? = null,
  val metadata: ShelfViewDictionaryPackMetadataDto? = null,
  val entries: List<ShelfViewDictionaryEntryImportDto>? = null,
)

data class ShelfViewDictionaryProviderDto(
  val id: String,
  val kind: String,
  val label: String,
  val enabled: Boolean,
  val sourceName: String,
  val sourceUrl: String?,
  val retrievedAt: String?,
  val licenseName: String,
  val licenseUrl: String?,
  val attribution: String?,
  val redistributionNotes: String?,
  val entryCount: Int,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
)

data class ShelfViewDictionaryLookupResultDto(
  val key: String,
  val kind: String,
  val providerId: String,
  val providerLabel: String,
  val term: String,
  val definition: String,
  val sourceName: String?,
  val sourceUrl: String?,
  val licenseName: String?,
)

fun ShelfViewDictionaryProviderSummary.toDto() =
  ShelfViewDictionaryProviderDto(
    id = provider.id,
    kind = provider.kind.name,
    label = provider.label,
    enabled = provider.enabled,
    sourceName = provider.sourceName,
    sourceUrl = provider.sourceUrl,
    retrievedAt = provider.retrievedAt,
    licenseName = provider.licenseName,
    licenseUrl = provider.licenseUrl,
    attribution = provider.attribution,
    redistributionNotes = provider.redistributionNotes,
    entryCount = entryCount,
    createdAt = provider.createdAt,
    updatedAt = provider.updatedAt,
  )

fun ShelfViewDictionaryLookupEntry.toDto() =
  ShelfViewDictionaryLookupResultDto(
    key = "server-${entry.id}",
    kind = provider.kind.name,
    providerId = provider.id,
    providerLabel = provider.label,
    term = entry.term,
    definition = entry.definition,
    sourceName = entry.sourceName ?: provider.sourceName,
    sourceUrl = entry.sourceUrl ?: provider.sourceUrl,
    licenseName = entry.licenseName ?: provider.licenseName,
  )
