package org.tales.interfaces.api.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.tales.domain.model.SearchContext
import org.tales.domain.model.SeriesSearch
import org.tales.interfaces.api.rest.dto.GroupCountDto
import org.tales.interfaces.api.rest.dto.SeriesDto

interface SeriesDtoRepository {
  fun findByIdOrNull(
    seriesId: String,
    userId: String,
  ): SeriesDto?

  fun findAll(
    pageable: Pageable,
  ): Page<SeriesDto>

  fun findAll(
    context: SearchContext,
    pageable: Pageable,
  ): Page<SeriesDto>

  fun findAll(
    search: SeriesSearch,
    context: SearchContext,
    pageable: Pageable,
  ): Page<SeriesDto>

  fun findAllRecentlyUpdated(
    search: SeriesSearch,
    context: SearchContext,
    pageable: Pageable,
  ): Page<SeriesDto>

  fun countByFirstCharacter(
    search: SeriesSearch,
    context: SearchContext,
  ): List<GroupCountDto>
}
