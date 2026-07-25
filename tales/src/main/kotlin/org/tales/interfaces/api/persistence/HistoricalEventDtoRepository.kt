package org.tales.interfaces.api.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.tales.interfaces.api.rest.dto.HistoricalEventDto

interface HistoricalEventDtoRepository {
  fun findAll(pageable: Pageable): Page<HistoricalEventDto>
}
