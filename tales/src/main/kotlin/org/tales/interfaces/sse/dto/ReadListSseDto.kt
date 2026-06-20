package org.tales.interfaces.sse.dto

data class ReadListSseDto(
  val readListId: String,
  val bookIds: List<String>,
)
