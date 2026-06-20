package org.tales.domain.persistence

import org.tales.domain.model.TransientBook

interface TransientBookRepository {
  fun findByIdOrNull(transientBookId: String): TransientBook?

  fun save(transientBook: TransientBook)

  fun save(transientBooks: Collection<TransientBook>)
}
