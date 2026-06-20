package org.tales.domain.persistence

import org.tales.domain.model.BookMetadata

interface BookMetadataRepository {
  fun findById(bookId: String): BookMetadata

  fun findByIdOrNull(bookId: String): BookMetadata?

  fun findAllByIds(bookIds: Collection<String>): Collection<BookMetadata>

  fun insert(metadata: BookMetadata)

  fun insert(metadatas: Collection<BookMetadata>)

  fun update(metadata: BookMetadata)

  fun update(metadatas: Collection<BookMetadata>)

  fun delete(bookId: String)

  fun delete(bookIds: Collection<String>)

  fun count(): Long
}
