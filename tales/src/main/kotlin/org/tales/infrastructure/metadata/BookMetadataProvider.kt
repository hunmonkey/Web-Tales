package org.tales.infrastructure.metadata

import org.tales.domain.model.BookMetadataPatch
import org.tales.domain.model.BookMetadataPatchCapability
import org.tales.domain.model.BookWithMedia

interface BookMetadataProvider : MetadataProvider {
  val capabilities: Set<BookMetadataPatchCapability>

  fun getBookMetadataFromBook(book: BookWithMedia): BookMetadataPatch?
}
