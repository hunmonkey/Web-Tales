package org.tales.interfaces.api.persistence

import org.tales.interfaces.api.rest.dto.TachiyomiReadProgressDto
import org.tales.interfaces.api.rest.dto.TachiyomiReadProgressV2Dto

interface ReadProgressDtoRepository {
  fun findProgressV2BySeries(
    seriesId: String,
    userId: String,
  ): TachiyomiReadProgressV2Dto

  fun findProgressByReadList(
    readListId: String,
    userId: String,
  ): TachiyomiReadProgressDto
}
