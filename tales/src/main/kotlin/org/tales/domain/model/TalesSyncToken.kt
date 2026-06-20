package org.tales.domain.model

data class TalesSyncToken(
  val version: Int = 1,
  val rawKoboSyncToken: String = "",
  /**
   * Only if a sync is currently ongoing, else null.
   */
  val ongoingSyncPointId: String? = null,
  /**
   * The last successful SyncPoint ID.
   */
  val lastSuccessfulSyncPointId: String? = null,
)
