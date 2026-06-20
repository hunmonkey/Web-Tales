package org.tales.interfaces.sse.dto

data class BookImportSseDto(
  val bookId: String?,
  val sourceFile: String,
  val success: Boolean,
  val message: String? = null,
)
