package org.tales.interfaces.api.rest.dto

import org.tales.domain.model.PageHashMatch
import org.tales.infrastructure.web.toFilePath

data class PageHashMatchDto(
  val bookId: String,
  val url: String,
  val pageNumber: Int,
  val fileName: String,
  val fileSize: Long,
  val mediaType: String,
)

fun PageHashMatch.toDto() =
  PageHashMatchDto(
    bookId = bookId,
    url = url.toFilePath(),
    pageNumber = pageNumber,
    fileName = fileName,
    fileSize = fileSize,
    mediaType = mediaType,
  )
