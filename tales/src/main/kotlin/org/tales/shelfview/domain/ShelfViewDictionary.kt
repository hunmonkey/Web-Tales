package org.tales.shelfview.domain

import com.github.f4b6a3.tsid.TsidCreator
import java.time.LocalDateTime

enum class ShelfViewDictionaryProviderKind {
  LOCAL_PACK,
}

data class ShelfViewDictionaryProvider(
  val userId: String,
  val kind: ShelfViewDictionaryProviderKind,
  val label: String,
  val enabled: Boolean = true,
  val sourceName: String,
  val sourceUrl: String? = null,
  val retrievedAt: String? = null,
  val licenseName: String,
  val licenseUrl: String? = null,
  val attribution: String? = null,
  val redistributionNotes: String? = null,
  val configJson: String? = null,
  val id: String = TsidCreator.getTsid256().toString(),
  val createdAt: LocalDateTime = LocalDateTime.now(),
  val updatedAt: LocalDateTime = createdAt,
)

data class ShelfViewDictionaryEntry(
  val providerId: String,
  val term: String,
  val normalizedTerm: String,
  val definition: String,
  val sourceName: String? = null,
  val sourceUrl: String? = null,
  val licenseName: String? = null,
  val id: String = TsidCreator.getTsid256().toString(),
  val createdAt: LocalDateTime = LocalDateTime.now(),
  val updatedAt: LocalDateTime = createdAt,
)

data class ShelfViewDictionaryProviderSummary(
  val provider: ShelfViewDictionaryProvider,
  val entryCount: Int,
)

data class ShelfViewDictionaryLookupEntry(
  val provider: ShelfViewDictionaryProvider,
  val entry: ShelfViewDictionaryEntry,
)
