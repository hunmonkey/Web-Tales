package org.tales.shelfview.infrastructure.jooq

import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.tales.infrastructure.jooq.SplitDslDaoBase
import org.tales.jooq.main.Tables
import org.tales.shelfview.domain.persistence.ShelfViewRemovedEntryRepository

@Component
class ShelfViewRemovedEntryDao(
  dslRW: DSLContext,
  @Qualifier("dslContextRO") dslRO: DSLContext,
) : SplitDslDaoBase(dslRW, dslRO),
  ShelfViewRemovedEntryRepository {
  private val removed = Tables.SHELFVIEW_REMOVED_ENTRY

  override fun findUrls(libraryId: String): Set<String> =
    dslRO
      .select(removed.URL)
      .from(removed)
      .where(removed.LIBRARY_ID.eq(libraryId))
      .fetchSet(removed.URL)

  @Transactional
  override fun add(
    libraryId: String,
    url: String,
  ) {
    dslRW
      .insertInto(removed, removed.LIBRARY_ID, removed.URL)
      .values(libraryId, url)
      .onDuplicateKeyIgnore()
      .execute()
  }

  @Transactional
  override fun deleteAll(libraryId: String) {
    dslRW
      .deleteFrom(removed)
      .where(removed.LIBRARY_ID.eq(libraryId))
      .execute()
  }
}
