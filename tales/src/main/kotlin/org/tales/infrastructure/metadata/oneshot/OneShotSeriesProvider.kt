package org.tales.infrastructure.metadata.oneshot

import org.tales.domain.model.Library
import org.tales.domain.model.MetadataPatchTarget
import org.tales.domain.model.Series
import org.tales.domain.model.SeriesMetadata
import org.tales.domain.model.SeriesMetadataPatch
import org.tales.domain.persistence.BookMetadataRepository
import org.tales.domain.persistence.BookRepository
import org.tales.infrastructure.metadata.SeriesMetadataProvider
import org.springframework.stereotype.Service

@Service
class OneShotSeriesProvider(
  private val bookRepository: BookRepository,
  private val bookMetadataRepository: BookMetadataRepository,
) : SeriesMetadataProvider {
  override fun getSeriesMetadata(series: Series): SeriesMetadataPatch? {
    if (!series.oneshot) return null
    val bookMetadata = bookMetadataRepository.findById(bookRepository.findAllIdsBySeriesId(series.id).first())
    return SeriesMetadataPatch(
      bookMetadata.title,
      bookMetadata.title,
      SeriesMetadata.Status.ENDED,
      bookMetadata.summary,
      null,
      null,
      null,
      null,
      null,
      1,
      emptySet(),
    )
  }

  override fun shouldLibraryHandlePatch(
    library: Library,
    target: MetadataPatchTarget,
  ): Boolean = target == MetadataPatchTarget.SERIES
}
