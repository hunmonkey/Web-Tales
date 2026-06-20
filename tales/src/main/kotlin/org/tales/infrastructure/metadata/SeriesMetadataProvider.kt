package org.tales.infrastructure.metadata

import org.tales.domain.model.Series
import org.tales.domain.model.SeriesMetadataPatch

interface SeriesMetadataProvider : MetadataProvider {
  fun getSeriesMetadata(series: Series): SeriesMetadataPatch?
}
