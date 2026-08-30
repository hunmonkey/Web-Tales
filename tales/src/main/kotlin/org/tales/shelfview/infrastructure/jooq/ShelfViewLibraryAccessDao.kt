package org.tales.shelfview.infrastructure.jooq

import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.tales.infrastructure.jooq.SplitDslDaoBase
import org.tales.jooq.main.Tables
import org.tales.shelfview.domain.persistence.ShelfViewLibraryAccessRepository
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class ShelfViewLibraryAccessDao(
  dslRW: DSLContext,
  @Qualifier("dslContextRO") dslRO: DSLContext,
) : SplitDslDaoBase(dslRW, dslRO),
  ShelfViewLibraryAccessRepository {
  private val access = Tables.SHELFVIEW_LIBRARY_ACCESS

  override fun findLockedLibraryIds(): Set<String> =
    dslRO
      .select(access.LIBRARY_ID)
      .from(access)
      .fetchSet(access.LIBRARY_ID)

  override fun findPinHashOrNull(libraryId: String): String? =
    dslRO
      .select(access.PIN_HASH)
      .from(access)
      .where(access.LIBRARY_ID.eq(libraryId))
      .fetchOne(access.PIN_HASH)

  @Transactional
  override fun savePinHash(
    libraryId: String,
    pinHash: String,
  ) {
    dslRW
      .insertInto(access, access.LIBRARY_ID, access.PIN_HASH, access.UPDATED_AT)
      .values(libraryId, pinHash, LocalDateTime.now(ZoneId.of("Z")))
      .onDuplicateKeyUpdate()
      .set(access.PIN_HASH, pinHash)
      .set(access.UPDATED_AT, LocalDateTime.now(ZoneId.of("Z")))
      .execute()
  }

  @Transactional
  override fun deletePin(libraryId: String) {
    dslRW
      .deleteFrom(access)
      .where(access.LIBRARY_ID.eq(libraryId))
      .execute()
  }
}
