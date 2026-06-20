package org.tales.shelfview.interfaces.api

import org.tales.domain.model.Media
import org.tales.interfaces.api.rest.dto.BookDto

internal fun shelfViewContentFingerprint(
  book: BookDto,
  media: Media,
): String =
  if (book.fileHash.isNotBlank()) {
    "tales-file-hash:${book.fileHash}"
  } else {
    "tales-file-state:${book.id}:${book.sizeBytes}:${book.fileLastModified}:${media.lastModifiedDate}:${shelfViewPageCount(media)}"
  }

internal fun shelfViewPageCount(media: Media): Int = media.pageCount.takeIf { it > 0 } ?: media.pages.size
