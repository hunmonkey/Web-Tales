package org.tales.shelfview.infrastructure.jooq

import org.tales.infrastructure.jooq.SplitDslDaoBase
import org.tales.jooq.main.Tables
import org.tales.language.toCurrentTimeZone
import org.tales.language.toUTC
import org.tales.shelfview.domain.ShelfViewBookmark
import org.tales.shelfview.domain.ShelfViewHighlight
import org.tales.shelfview.domain.ShelfViewReaderPosition
import org.tales.shelfview.domain.ShelfViewReaderPreference
import org.tales.shelfview.domain.persistence.ShelfViewBookmarkRepository
import org.tales.shelfview.domain.persistence.ShelfViewHighlightRepository
import org.tales.shelfview.domain.persistence.ShelfViewReaderPositionRepository
import org.tales.shelfview.domain.persistence.ShelfViewReaderPreferenceRepository
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
  ShelfViewHighlightRepository,
  ShelfViewReaderPreferenceRepository {
  private val position = Tables.SHELFVIEW_READER_POSITION
  private val bookmark = Tables.SHELFVIEW_BOOKMARK
  private val highlight = Tables.SHELFVIEW_HIGHLIGHT
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

  override fun findHighlights(
    bookId: String,
    userId: String,
  ): Collection<ShelfViewHighlight> =
    dslRO
      .selectFrom(highlight)
      .where(highlight.BOOK_ID.eq(bookId))
      .and(highlight.USER_ID.eq(userId))
      .orderBy(highlight.CREATED_AT.asc())
      .fetch { it.toHighlight() }

  override fun findHighlightByIdAndUserIdOrNull(
    highlightId: String,
    userId: String,
  ): ShelfViewHighlight? =
    dslRO
      .selectFrom(highlight)
      .where(highlight.ID.eq(highlightId))
      .and(highlight.USER_ID.eq(userId))
      .fetchOne()
      ?.toHighlight()

  @Transactional
  override fun saveHighlight(highlight: ShelfViewHighlight) {
    val updatedAt = LocalDateTime.now(ZoneId.of("Z"))

    dslRW
      .insertInto(
        this.highlight,
        this.highlight.ID,
        this.highlight.USER_ID,
        this.highlight.BOOK_ID,
        this.highlight.LOCATOR_JSON,
        this.highlight.SELECTOR_JSON,
        this.highlight.CONTENT_FINGERPRINT,
        this.highlight.COLOR,
        this.highlight.NOTE,
        this.highlight.TEXT_QUOTE,
        this.highlight.CREATED_AT,
        this.highlight.UPDATED_AT,
      ).values(
        highlight.id,
        highlight.userId,
        highlight.bookId,
        highlight.locatorJson,
        highlight.selectorJson,
        highlight.contentFingerprint,
        highlight.color,
        highlight.note,
        highlight.textQuote,
        highlight.createdAt.toUTC(),
        highlight.updatedAt.toUTC(),
      ).onDuplicateKeyUpdate()
      .set(this.highlight.USER_ID, highlight.userId)
      .set(this.highlight.BOOK_ID, highlight.bookId)
      .set(this.highlight.LOCATOR_JSON, highlight.locatorJson)
      .set(this.highlight.SELECTOR_JSON, highlight.selectorJson)
      .set(this.highlight.CONTENT_FINGERPRINT, highlight.contentFingerprint)
      .set(this.highlight.COLOR, highlight.color)
      .set(this.highlight.NOTE, highlight.note)
      .set(this.highlight.TEXT_QUOTE, highlight.textQuote)
      .set(this.highlight.UPDATED_AT, updatedAt)
      .execute()
  }

  @Transactional
  override fun deleteHighlight(
    highlightId: String,
    userId: String,
  ) {
    dslRW
      .deleteFrom(highlight)
      .where(highlight.ID.eq(highlightId))
      .and(highlight.USER_ID.eq(userId))
      .execute()
  }

  @Transactional
  override fun deleteHighlightsByUserId(userId: String) {
    dslRW
      .deleteFrom(highlight)
      .where(highlight.USER_ID.eq(userId))
      .execute()
  }

  @Transactional
  override fun deleteHighlightsByBookId(bookId: String) {
    dslRW
      .deleteFrom(highlight)
      .where(highlight.BOOK_ID.eq(bookId))
      .execute()
  }

  @Transactional
  override fun deleteAllHighlights() {
    dslRW.deleteFrom(highlight).execute()
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

  private fun Record.toHighlight() =
    ShelfViewHighlight(
      id = get(highlight.ID),
      userId = get(highlight.USER_ID),
      bookId = get(highlight.BOOK_ID),
      locatorJson = get(highlight.LOCATOR_JSON),
      selectorJson = get(highlight.SELECTOR_JSON),
      contentFingerprint = get(highlight.CONTENT_FINGERPRINT),
      color = get(highlight.COLOR),
      note = get(highlight.NOTE),
      textQuote = get(highlight.TEXT_QUOTE),
      createdAt = get(highlight.CREATED_AT).toCurrentTimeZone(),
      updatedAt = get(highlight.UPDATED_AT).toCurrentTimeZone(),
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
