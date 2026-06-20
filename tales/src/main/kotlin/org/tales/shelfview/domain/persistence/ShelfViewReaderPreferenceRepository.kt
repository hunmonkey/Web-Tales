package org.tales.shelfview.domain.persistence

import org.tales.shelfview.domain.ShelfViewReaderPreference

interface ShelfViewReaderPreferenceRepository {
  fun findPreferenceOrNull(
    userId: String,
    scopeType: String,
    scopeId: String,
    format: String,
  ): ShelfViewReaderPreference?

  fun findPreferencesByUserId(userId: String): Collection<ShelfViewReaderPreference>

  fun savePreference(preference: ShelfViewReaderPreference)

  fun deletePreference(
    userId: String,
    scopeType: String,
    scopeId: String,
    format: String,
  )

  fun deletePreferencesByUserId(userId: String)

  fun deleteAllPreferences()
}
