package org.tales.shelfview.infrastructure.jooq

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.tales.domain.model.TalesUser
import org.tales.domain.model.makeBook
import org.tales.domain.model.makeLibrary
import org.tales.domain.model.makeSeries
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.SeriesRepository
import org.tales.domain.persistence.TalesUserRepository
import org.tales.infrastructure.jooq.offset
import org.tales.shelfview.domain.ShelfViewBookmark
import org.tales.shelfview.domain.ShelfViewHighlight
import org.tales.shelfview.domain.ShelfViewReaderPosition
import org.tales.shelfview.domain.ShelfViewReaderPreference
import java.time.LocalDateTime

@SpringBootTest
class ShelfViewReaderStateDaoTest(
  @Autowired private val shelfViewReaderStateDao: ShelfViewReaderStateDao,
  @Autowired private val userRepository: TalesUserRepository,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val libraryRepository: LibraryRepository,
) {
  private val library = makeLibrary()
  private val series = makeSeries("ShelfView Series")
  private val user1 = TalesUser("shelfview-user1@example.org", "")
  private val user2 = TalesUser("shelfview-user2@example.org", "")
  private val book1 = makeBook("ShelfView Book 1")
  private val book2 = makeBook("ShelfView Book 2")

  @BeforeAll
  fun setup() {
    libraryRepository.insert(library)
    seriesRepository.insert(series.copy(libraryId = library.id))
    userRepository.insert(user1)
    userRepository.insert(user2)
    bookRepository.insert(book1.copy(libraryId = library.id, seriesId = series.id))
    bookRepository.insert(book2.copy(libraryId = library.id, seriesId = series.id))
  }

  @AfterEach
  fun cleanup() {
    shelfViewReaderStateDao.deleteAllPreferences()
    shelfViewReaderStateDao.deleteAllHighlights()
    shelfViewReaderStateDao.deleteAllBookmarks()
    shelfViewReaderStateDao.deleteAllPositions()
  }

  @AfterAll
  fun tearDown() {
    userRepository.deleteAll()
    bookRepository.deleteAll()
    seriesRepository.deleteAll()
    libraryRepository.deleteAll()
  }

  @Test
  fun `given reader position when saving again then it is updated`() {
    val now = LocalDateTime.now()

    shelfViewReaderStateDao.savePosition(
      ShelfViewReaderPosition(
        userId = user1.id,
        bookId = book1.id,
        locatorJson = """{"type":"fixed-page","page":0}""",
        progression = 0.25F,
        contentFingerprint = "fingerprint-a",
      ),
    )

    val saved = shelfViewReaderStateDao.findPositionOrNull(book1.id, user1.id)

    assertThat(saved).isNotNull
    with(saved!!) {
      assertThat(locatorJson).isEqualTo("""{"type":"fixed-page","page":0}""")
      assertThat(progression).isEqualTo(0.25F)
      assertThat(contentFingerprint).isEqualTo("fingerprint-a")
      assertThat(updatedAt).isCloseTo(now, offset)
    }

    val modificationDate = LocalDateTime.now()

    shelfViewReaderStateDao.savePosition(
      saved.copy(
        locatorJson = """{"type":"fixed-page","page":4}""",
        progression = 0.75F,
        contentFingerprint = "fingerprint-b",
      ),
    )

    val updated = shelfViewReaderStateDao.findPositionOrNull(book1.id, user1.id)

    assertThat(updated).isNotNull
    with(updated!!) {
      assertThat(locatorJson).isEqualTo("""{"type":"fixed-page","page":4}""")
      assertThat(progression).isEqualTo(0.75F)
      assertThat(contentFingerprint).isEqualTo("fingerprint-b")
      assertThat(updatedAt).isCloseTo(modificationDate, offset)
    }
  }

  @Test
  fun `given bookmarks for several users when fetching book bookmarks then only matching user bookmarks are returned`() {
    val bookmark1 =
      ShelfViewBookmark(
        id = "bookmark-1",
        userId = user1.id,
        bookId = book1.id,
        locatorJson = """{"type":"fixed-page","page":1}""",
        label = "Start",
        note = "Opening page",
      )
    val bookmark2 =
      ShelfViewBookmark(
        id = "bookmark-2",
        userId = user2.id,
        bookId = book1.id,
        locatorJson = """{"type":"fixed-page","page":2}""",
        label = "Other user",
      )

    shelfViewReaderStateDao.saveBookmark(bookmark1)
    shelfViewReaderStateDao.saveBookmark(bookmark2)

    val fetched = shelfViewReaderStateDao.findBookmarks(book1.id, user1.id)

    assertThat(fetched).hasSize(1)
    with(fetched.first()) {
      assertThat(id).isEqualTo("bookmark-1")
      assertThat(label).isEqualTo("Start")
      assertThat(note).isEqualTo("Opening page")
      assertThat(createdAt).isCloseTo(bookmark1.createdAt, offset)
    }

    val modificationDate = LocalDateTime.now()

    shelfViewReaderStateDao.saveBookmark(
      bookmark1.copy(
        locatorJson = """{"type":"fixed-page","page":3}""",
        label = "Updated",
        note = null,
      ),
    )

    val updated = shelfViewReaderStateDao.findBookmarkByIdAndUserIdOrNull("bookmark-1", user1.id)

    assertThat(updated).isNotNull
    with(updated!!) {
      assertThat(locatorJson).isEqualTo("""{"type":"fixed-page","page":3}""")
      assertThat(label).isEqualTo("Updated")
      assertThat(note).isNull()
      assertThat(updatedAt).isCloseTo(modificationDate, offset)
    }

    shelfViewReaderStateDao.deleteBookmark("bookmark-1", user1.id)

    assertThat(shelfViewReaderStateDao.findBookmarks(book1.id, user1.id)).isEmpty()
    assertThat(shelfViewReaderStateDao.findBookmarks(book1.id, user2.id)).hasSize(1)
  }

  @Test
  fun `given highlights for several users when fetching book highlights then only matching user highlights are returned`() {
    val highlight1 =
      ShelfViewHighlight(
        id = "highlight-1",
        userId = user1.id,
        bookId = book1.id,
        locatorJson = textLocatorJson(paragraph = 0),
        selectorJson = textRangeSelectorJson(startOffset = 0, endOffset = 12),
        contentFingerprint = "fingerprint-a",
        color = "YELLOW",
        note = "Important",
        textQuote = "Opening text",
      )
    val highlight2 =
      ShelfViewHighlight(
        id = "highlight-2",
        userId = user2.id,
        bookId = book1.id,
        locatorJson = textLocatorJson(paragraph = 1),
        selectorJson = textRangeSelectorJson(paragraph = 1, startOffset = 0, endOffset = 8),
        contentFingerprint = "fingerprint-a",
        color = "GREEN",
      )

    shelfViewReaderStateDao.saveHighlight(highlight1)
    shelfViewReaderStateDao.saveHighlight(highlight2)

    val fetched = shelfViewReaderStateDao.findHighlights(book1.id, user1.id)

    assertThat(fetched).hasSize(1)
    with(fetched.first()) {
      assertThat(id).isEqualTo("highlight-1")
      assertThat(selectorJson).contains("TEXT_RANGE")
      assertThat(contentFingerprint).isEqualTo("fingerprint-a")
      assertThat(color).isEqualTo("YELLOW")
      assertThat(note).isEqualTo("Important")
      assertThat(textQuote).isEqualTo("Opening text")
      assertThat(createdAt).isCloseTo(highlight1.createdAt, offset)
    }

    val modificationDate = LocalDateTime.now()

    shelfViewReaderStateDao.saveHighlight(
      highlight1.copy(
        selectorJson = textRangeSelectorJson(startOffset = 3, endOffset = 18),
        contentFingerprint = "fingerprint-b",
        color = "BLUE",
        note = null,
        textQuote = "Updated text",
      ),
    )

    val updated = shelfViewReaderStateDao.findHighlightByIdAndUserIdOrNull("highlight-1", user1.id)

    assertThat(updated).isNotNull
    with(updated!!) {
      assertThat(selectorJson).contains("\"endCharacterOffset\": 18")
      assertThat(contentFingerprint).isEqualTo("fingerprint-b")
      assertThat(color).isEqualTo("BLUE")
      assertThat(note).isNull()
      assertThat(textQuote).isEqualTo("Updated text")
      assertThat(updatedAt).isCloseTo(modificationDate, offset)
    }

    shelfViewReaderStateDao.deleteHighlight("highlight-1", user1.id)

    assertThat(shelfViewReaderStateDao.findHighlights(book1.id, user1.id)).isEmpty()
    assertThat(shelfViewReaderStateDao.findHighlights(book1.id, user2.id)).hasSize(1)
  }

  @Test
  fun `given reader preference when saving same scope then settings are updated`() {
    shelfViewReaderStateDao.savePreference(
      ShelfViewReaderPreference(
        userId = user1.id,
        scopeType = "BOOK",
        scopeId = book1.id,
        format = "FIXED_PAGE_SEQUENCE",
        settingsJson = """{"spread":"single","filter":"none"}""",
      ),
    )
    shelfViewReaderStateDao.savePreference(
      ShelfViewReaderPreference(
        userId = user1.id,
        scopeType = "BOOK",
        scopeId = book2.id,
        format = "FIXED_PAGE_SEQUENCE",
        settingsJson = """{"spread":"double"}""",
      ),
    )

    val saved =
      shelfViewReaderStateDao.findPreferenceOrNull(
        userId = user1.id,
        scopeType = "BOOK",
        scopeId = book1.id,
        format = "FIXED_PAGE_SEQUENCE",
      )

    assertThat(saved).isNotNull
    assertThat(saved!!.settingsJson).isEqualTo("""{"spread":"single","filter":"none"}""")
    assertThat(shelfViewReaderStateDao.findPreferencesByUserId(user1.id)).hasSize(2)

    val modificationDate = LocalDateTime.now()

    shelfViewReaderStateDao.savePreference(
      saved.copy(settingsJson = """{"spread":"double","filter":"mono"}"""),
    )

    val updated =
      shelfViewReaderStateDao.findPreferenceOrNull(
        userId = user1.id,
        scopeType = "BOOK",
        scopeId = book1.id,
        format = "FIXED_PAGE_SEQUENCE",
      )

    assertThat(updated).isNotNull
    with(updated!!) {
      assertThat(settingsJson).isEqualTo("""{"spread":"double","filter":"mono"}""")
      assertThat(updatedAt).isCloseTo(modificationDate, offset)
    }

    shelfViewReaderStateDao.deletePreference(
      userId = user1.id,
      scopeType = "BOOK",
      scopeId = book1.id,
      format = "FIXED_PAGE_SEQUENCE",
    )

    assertThat(
      shelfViewReaderStateDao.findPreferenceOrNull(
        userId = user1.id,
        scopeType = "BOOK",
        scopeId = book1.id,
        format = "FIXED_PAGE_SEQUENCE",
      ),
    ).isNull()
    assertThat(shelfViewReaderStateDao.findPreferencesByUserId(user1.id)).hasSize(1)
  }

  private fun textLocatorJson(paragraph: Int): String =
    """
    {
      "schemaVersion": 1,
      "kind": "TEXT",
      "paragraph": $paragraph
    }
    """.trimIndent()

  private fun textRangeSelectorJson(
    paragraph: Int = 0,
    startOffset: Int,
    endOffset: Int,
  ): String =
    """
    {
      "schemaVersion": 1,
      "kind": "TEXT_RANGE",
      "startParagraph": $paragraph,
      "startCharacterOffset": $startOffset,
      "endParagraph": $paragraph,
      "endCharacterOffset": $endOffset,
      "normalizationVersion": 1
    }
    """.trimIndent()
}
