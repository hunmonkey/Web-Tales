package org.tales.domain.persistence

import org.tales.domain.model.SeriesMetadata

interface SeriesMetadataRepository {
  fun findById(seriesId: String): SeriesMetadata

  fun findByIdOrNull(seriesId: String): SeriesMetadata?

  fun insert(metadata: SeriesMetadata)

  fun update(metadata: SeriesMetadata)

  fun delete(seriesId: String)

  fun delete(seriesIds: Collection<String>)

  fun count(): Long
}
