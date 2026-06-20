package org.tales.interfaces.api.rest.dto

import org.tales.domain.model.CopyMode

data class BookImportBatchDto(
  val books: List<BookImportDto> = emptyList(),
  val copyMode: CopyMode,
)

data class BookImportDto(
  val sourceFile: String,
  val seriesId: String,
  val upgradeBookId: String? = null,
  val destinationName: String? = null,
)
