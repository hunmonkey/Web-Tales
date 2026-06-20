package org.tales.infrastructure.metadata

import org.tales.domain.model.BookWithMedia
import org.tales.domain.model.SeriesMetadataPatch

interface SeriesMetadataFromBookProvider : MetadataProvider {
  val supportsAppendVolume: Boolean

  fun getSeriesMetadataFromBook(
    book: BookWithMedia,
    appendVolumeToTitle: Boolean,
  ): SeriesMetadataPatch?
}
