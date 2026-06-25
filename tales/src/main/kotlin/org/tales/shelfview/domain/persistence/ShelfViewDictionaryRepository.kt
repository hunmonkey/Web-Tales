package org.tales.shelfview.domain.persistence

import org.tales.shelfview.domain.ShelfViewDictionaryEntry
import org.tales.shelfview.domain.ShelfViewDictionaryLookupEntry
import org.tales.shelfview.domain.ShelfViewDictionaryProvider
import org.tales.shelfview.domain.ShelfViewDictionaryProviderSummary

interface ShelfViewDictionaryRepository {
  fun findProviderSummaries(userId: String): Collection<ShelfViewDictionaryProviderSummary>

  fun findLookupEntries(
    userId: String,
    normalizedTerm: String,
  ): Collection<ShelfViewDictionaryLookupEntry>

  fun saveLocalPack(
    provider: ShelfViewDictionaryProvider,
    entries: Collection<ShelfViewDictionaryEntry>,
  )

  fun deleteProvider(
    providerId: String,
    userId: String,
  ): Boolean

  fun deleteProvidersByUserId(userId: String)

  fun deleteAllProviders()
}
