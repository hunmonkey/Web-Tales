package org.tales.domain.persistence

import org.tales.domain.model.HistoricalEvent

interface HistoricalEventRepository {
  fun insert(event: HistoricalEvent)
}
