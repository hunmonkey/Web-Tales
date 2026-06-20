package org.tales.infrastructure.jooq.main

import org.assertj.core.api.Assertions.assertThat
import org.tales.domain.model.TalesUser
import org.tales.domain.model.ReadProgress
import org.tales.domain.model.makeBook
import org.tales.domain.model.makeLibrary
import org.tales.domain.model.makeSeries
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.TalesUserRepository
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.SeriesRepository
import org.tales.infrastructure.jooq.offset
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest
class ReadProgressDaoTest(
  @Autowired private val readProgressDao: ReadProgressDao,
  @Autowired private val userRepository: TalesUserRepository,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val libraryRepository: LibraryRepository,
) {
  private val library = makeLibrary()
  private val series = makeSeries("Series")

  private val user1 = TalesUser("user1@example.org", "")
  private val user2 = TalesUser("user2@example.org", "")

  private val book1 = makeBook("Book1")
  private val book2 = makeBook("Book2")

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
  fun deleteReadProgress() {
    readProgressDao.deleteAll()
  }

  @AfterAll
  fun tearDown() {
    userRepository.deleteAll()
    bookRepository.deleteAll()
    seriesRepository.deleteAll()
    libraryRepository.deleteAll()
  }

  @Test
  fun `given book without user progress when saving progress then progress is saved`() {
    val now = LocalDateTime.now()

    readProgressDao.save(
      ReadProgress(
        book1.id,
        user1.id,
        5,
        false,
      ),
    )

    val readProgressList = readProgressDao.findAllByUserId(user1.id)

    assertThat(readProgressList).hasSize(1)
    with(readProgressList.first()) {
      assertThat(page).isEqualTo(5)
      assertThat(completed).isEqualTo(false)
      assertThat(bookId).isEqualTo(book1.id)
      assertThat(readDate).isCloseTo(now, offset)
      assertThat(createdDate)
        .isCloseTo(now, offset)
        .isEqualToIgnoringNanos(lastModifiedDate)
    }
  }

  @Test
  fun `given book with user progress when saving progress then progress is updated`() {
    readProgressDao.save(
      ReadProgress(
        book1.id,
        user1.id,
        5,
        false,
      ),
    )

    val modificationDate = LocalDateTime.now()
    val readDateInThePast = LocalDateTime.now().minusYears(1)

    readProgressDao.save(
      ReadProgress(
        book1.id,
        user1.id,
        10,
        true,
        readDate = readDateInThePast,
      ),
    )

    val readProgressList = readProgressDao.findAllByUserId(user1.id)

    assertThat(readProgressList).hasSize(1)
    with(readProgressList.first()) {
      assertThat(page).isEqualTo(10)
      assertThat(completed).isEqualTo(true)
      assertThat(bookId).isEqualTo(book1.id)
      assertThat(readDate).isEqualToIgnoringNanos(readDateInThePast)
      assertThat(createdDate)
        .isBefore(modificationDate)
        .isNotEqualTo(lastModifiedDate)
      assertThat(lastModifiedDate).isCloseTo(modificationDate, offset)
    }
  }
}
