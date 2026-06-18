package org.gotson.komga.shelfview.infrastructure.jooq

import org.gotson.komga.infrastructure.jooq.SplitDslDaoBase
import org.gotson.komga.jooq.main.Tables
import org.gotson.komga.language.toCurrentTimeZone
import org.gotson.komga.language.toUTC
import org.gotson.komga.shelfview.domain.ShelfViewBookmark
import org.gotson.komga.shelfview.domain.ShelfViewReaderPosition
import org.gotson.komga.shelfview.domain.ShelfViewReaderPreference
import org.gotson.komga.shelfview.domain.persistence.ShelfViewBookmarkRepository
import org.gotson.komga.shelfview.domain.persistence.ShelfViewReaderPositionRepository
import org.gotson.komga.shelfview.domain.persistence.ShelfViewReaderPreferenceRepository
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class ShelfViewReaderStateDao(
  dslRW: DSLContext,
  @Qualifier("dslContextRO") dslRO: DSLContext,
) : SplitDslDaoBase(dslRW, dslRO),
  ShelfViewReaderPositionRepository,
  ShelfViewBookmarkRepository,
  ShelfViewReaderPreferenceRepository {
  private val position = Tables.SHELFVIEW_READER_POSITION
  private val bookmark = Tables.SHELFVIEW_BOOKMARK
  private val preference = Tables.SHELFVIEW_READER_PREFERENCE

  override fun findPositionOrNull(
    bookId: String,
    userId: String,
  ): ShelfViewReaderPosition? =
    dslRO
      .selectFrom(position)
      .where(position.BOOK_ID.eq(bookId))
      .and(position.USER_ID.eq(userId))
      .fetchOne()
      ?.toPosition()

  @Transactional
  override fun savePosition(position: ShelfViewReaderPosition) {
    val updatedAt = LocalDateTime.now(ZoneId.of("Z"))

    dslRW
      .insertInto(
        this.position,
        this.position.USER_ID,
        this.position.BOOK_ID,
        this.position.LOCATOR_JSON,
        this.position.PROGRESSION,
        this.position.CONTENT_FINGERPRINT,
        this.position.UPDATED_AT,
      ).values(
        position.userId,
        position.bookId,
        position.locatorJson,
        position.progression,
        position.contentFingerprint,
        position.updatedAt.toUTC(),
      ).onDuplicateKeyUpdate()
      .set(this.position.LOCATOR_JSON, position.locatorJson)
      .set(this.position.PROGRESSION, position.progression)
      .set(this.position.CONTENT_FINGERPRINT, position.contentFingerprint)
      .set(this.position.UPDATED_AT, updatedAt)
      .execute()
  }

  @Transactional
  override fun deletePosition(
    bookId: String,
    userId: String,
  ) {
    dslRW
      .deleteFrom(position)
      .where(position.BOOK_ID.eq(bookId))
      .and(position.USER_ID.eq(userId))
      .execute()
  }

  @Transactional
  override fun deletePositionsByUserId(userId: String) {
    dslRW
      .deleteFrom(position)
      .where(position.USER_ID.eq(userId))
      .execute()
  }

  @Transactional
  override fun deletePositionsByBookId(bookId: String) {
    dslRW
      .deleteFrom(position)
      .where(position.BOOK_ID.eq(bookId))
      .execute()
  }

  @Transactional
  override fun deleteAllPositions() {
    dslRW.deleteFrom(position).execute()
  }

  override fun findBookmarks(
    bookId: String,
    userId: String,
  ): Collection<ShelfViewBookmark> =
    dslRO
      .selectFrom(bookmark)
      .where(bookmark.BOOK_ID.eq(bookId))
      .and(bookmark.USER_ID.eq(userId))
      .orderBy(bookmark.CREATED_AT.asc())
      .fetch { it.toBookmark() }

  override fun findBookmarkByIdAndUserIdOrNull(
    bookmarkId: String,
    userId: String,
  ): ShelfViewBookmark? =
    dslRO
      .selectFrom(bookmark)
      .where(bookmark.ID.eq(bookmarkId))
      .and(bookmark.USER_ID.eq(userId))
      .fetchOne()
      ?.toBookmark()

  @Transactional
  override fun saveBookmark(bookmark: ShelfViewBookmark) {
    val updatedAt = LocalDateTime.now(ZoneId.of("Z"))

    dslRW
      .insertInto(
        this.bookmark,
        this.bookmark.ID,
        this.bookmark.USER_ID,
        this.bookmark.BOOK_ID,
        this.bookmark.LOCATOR_JSON,
        this.bookmark.LABEL,
        this.bookmark.NOTE,
        this.bookmark.CREATED_AT,
        this.bookmark.UPDATED_AT,
      ).values(
        bookmark.id,
        bookmark.userId,
        bookmark.bookId,
        bookmark.locatorJson,
        bookmark.label,
        bookmark.note,
        bookmark.createdAt.toUTC(),
        bookmark.updatedAt.toUTC(),
      ).onDuplicateKeyUpdate()
      .set(this.bookmark.USER_ID, bookmark.userId)
      .set(this.bookmark.BOOK_ID, bookmark.bookId)
      .set(this.bookmark.LOCATOR_JSON, bookmark.locatorJson)
      .set(this.bookmark.LABEL, bookmark.label)
      .set(this.bookmark.NOTE, bookmark.note)
      .set(this.bookmark.UPDATED_AT, updatedAt)
      .execute()
  }

  @Transactional
  override fun deleteBookmark(
    bookmarkId: String,
    userId: String,
  ) {
    dslRW
      .deleteFrom(bookmark)
      .where(bookmark.ID.eq(bookmarkId))
      .and(bookmark.USER_ID.eq(userId))
      .execute()
  }

  @Transactional
  override fun deleteBookmarksByUserId(userId: String) {
    dslRW
      .deleteFrom(bookmark)
      .where(bookmark.USER_ID.eq(userId))
      .execute()
  }

  @Transactional
  override fun deleteBookmarksByBookId(bookId: String) {
    dslRW
      .deleteFrom(bookmark)
      .where(bookmark.BOOK_ID.eq(bookId))
      .execute()
  }

  @Transactional
  override fun deleteAllBookmarks() {
    dslRW.deleteFrom(bookmark).execute()
  }

  override fun findPreferenceOrNull(
    userId: String,
    scopeType: String,
    scopeId: String,
    format: String,
  ): ShelfViewReaderPreference? =
    dslRO
      .selectFrom(preference)
      .where(preference.USER_ID.eq(userId))
      .and(preference.SCOPE_TYPE.eq(scopeType))
      .and(preference.SCOPE_ID.eq(scopeId))
      .and(preference.FORMAT.eq(format))
      .fetchOne()
      ?.toPreference()

  override fun findPreferencesByUserId(userId: String): Collection<ShelfViewReaderPreference> =
    dslRO
      .selectFrom(preference)
      .where(preference.USER_ID.eq(userId))
      .fetch { it.toPreference() }

  @Transactional
  override fun savePreference(preference: ShelfViewReaderPreference) {
    val updatedAt = LocalDateTime.now(ZoneId.of("Z"))

    dslRW
      .insertInto(
        this.preference,
        this.preference.USER_ID,
        this.preference.SCOPE_TYPE,
        this.preference.SCOPE_ID,
        this.preference.FORMAT,
        this.preference.SETTINGS_JSON,
        this.preference.UPDATED_AT,
      ).values(
        preference.userId,
        preference.scopeType,
        preference.scopeId,
        preference.format,
        preference.settingsJson,
        preference.updatedAt.toUTC(),
      ).onDuplicateKeyUpdate()
      .set(this.preference.SETTINGS_JSON, preference.settingsJson)
      .set(this.preference.UPDATED_AT, updatedAt)
      .execute()
  }

  @Transactional
  override fun deletePreference(
    userId: String,
    scopeType: String,
    scopeId: String,
    format: String,
  ) {
    dslRW
      .deleteFrom(preference)
      .where(preference.USER_ID.eq(userId))
      .and(preference.SCOPE_TYPE.eq(scopeType))
      .and(preference.SCOPE_ID.eq(scopeId))
      .and(preference.FORMAT.eq(format))
      .execute()
  }

  @Transactional
  override fun deletePreferencesByUserId(userId: String) {
    dslRW
      .deleteFrom(preference)
      .where(preference.USER_ID.eq(userId))
      .execute()
  }

  @Transactional
  override fun deleteAllPreferences() {
    dslRW.deleteFrom(preference).execute()
  }

  private fun Record.toPosition() =
    ShelfViewReaderPosition(
      userId = get(position.USER_ID),
      bookId = get(position.BOOK_ID),
      locatorJson = get(position.LOCATOR_JSON),
      progression = get(position.PROGRESSION),
      contentFingerprint = get(position.CONTENT_FINGERPRINT),
      updatedAt = get(position.UPDATED_AT).toCurrentTimeZone(),
    )

  private fun Record.toBookmark() =
    ShelfViewBookmark(
      id = get(bookmark.ID),
      userId = get(bookmark.USER_ID),
      bookId = get(bookmark.BOOK_ID),
      locatorJson = get(bookmark.LOCATOR_JSON),
      label = get(bookmark.LABEL),
      note = get(bookmark.NOTE),
      createdAt = get(bookmark.CREATED_AT).toCurrentTimeZone(),
      updatedAt = get(bookmark.UPDATED_AT).toCurrentTimeZone(),
    )

  private fun Record.toPreference() =
    ShelfViewReaderPreference(
      userId = get(preference.USER_ID),
      scopeType = get(preference.SCOPE_TYPE),
      scopeId = get(preference.SCOPE_ID),
      format = get(preference.FORMAT),
      settingsJson = get(preference.SETTINGS_JSON),
      updatedAt = get(preference.UPDATED_AT).toCurrentTimeZone(),
    )
}
