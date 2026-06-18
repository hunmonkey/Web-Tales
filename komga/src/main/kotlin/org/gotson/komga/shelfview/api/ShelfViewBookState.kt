package org.gotson.komga.shelfview.api

import org.gotson.komga.domain.model.Media
import org.gotson.komga.interfaces.api.rest.dto.BookDto

internal fun shelfViewContentFingerprint(
  book: BookDto,
  media: Media,
): String =
  if (book.fileHash.isNotBlank()) {
    "komga-file-hash:${book.fileHash}"
  } else {
    "komga-file-state:${book.id}:${book.sizeBytes}:${book.fileLastModified}:${media.lastModifiedDate}:${shelfViewPageCount(media)}"
  }

internal fun shelfViewPageCount(media: Media): Int = media.pageCount.takeIf { it > 0 } ?: media.pages.size
