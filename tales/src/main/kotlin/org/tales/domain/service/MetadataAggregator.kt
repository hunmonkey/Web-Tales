package org.tales.domain.service

import org.springframework.stereotype.Service
import org.tales.domain.model.BookMetadata
import org.tales.domain.model.BookMetadataAggregation

@Service
class MetadataAggregator {
  fun aggregate(metadatas: Collection<BookMetadata>): BookMetadataAggregation {
    val authors = metadatas.flatMap { it.authors }.distinctBy { "${it.role}__${it.name}" }
    val tags = metadatas.flatMap { it.tags }.toSet()
    val (summary, summaryNumber) =
      metadatas
        .sortedBy { it.numberSort }
        .find { it.summary.isNotBlank() }
        ?.let {
          it.summary to it.number
        } ?: ("" to "")
    val releaseDate = metadatas.mapNotNull { it.releaseDate }.minOrNull()

    return BookMetadataAggregation(authors = authors, tags = tags, releaseDate = releaseDate, summary = summary, summaryNumber = summaryNumber)
  }
}
