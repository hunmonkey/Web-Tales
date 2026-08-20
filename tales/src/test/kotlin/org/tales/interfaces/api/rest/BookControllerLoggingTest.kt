package org.tales.interfaces.api.rest

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.tales.application.tasks.TaskEmitter
import org.tales.captureLogs
import org.tales.domain.model.Book
import org.tales.domain.model.CopyMode
import org.tales.domain.model.Media
import org.tales.domain.model.TalesUser
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.domain.service.BookLifecycle
import org.tales.infrastructure.security.TalesPrincipal
import org.tales.interfaces.api.rest.dto.BookImportBatchDto
import org.tales.interfaces.api.rest.dto.BookImportDto
import java.net.URL
import java.nio.file.NoSuchFileException
import java.time.LocalDateTime

class BookControllerLoggingTest {
  @Test
  fun `import enqueue failure log omits source file and raw database message`() {
    val taskEmitter = mockk<TaskEmitter>()
    every {
      taskEmitter.importBook(
        sourceFile = SENSITIVE_SOURCE_FILE,
        seriesId = SERIES_ID,
        copyMode = CopyMode.COPY,
        destinationName = null,
        upgradeBookId = null,
        priority = any(),
      )
    } throws DataAccessException(SENSITIVE_SQL_MESSAGE)
    val controller = controller(taskEmitter = taskEmitter)

    val logs =
      captureLogs(BookController::class.java) {
        controller.importBooks(
          BookImportBatchDto(
            books = listOf(BookImportDto(sourceFile = SENSITIVE_SOURCE_FILE, seriesId = SERIES_ID)),
            copyMode = CopyMode.COPY,
          ),
        )
      }.rendered

    assertThat(logs)
      .contains("seriesId=$SERIES_ID", "errorType=DataAccessException")
      .doesNotContain(SENSITIVE_SOURCE_FILE, SENSITIVE_SQL_MESSAGE)
  }

  @Test
  fun `missing page log omits book path and rendered exception`() {
    val book =
      Book(
        name = "private-book",
        url = URL("file:/SV1274_PRIVATE/book.cbz"),
        fileLastModified = LocalDateTime.now(),
        id = BOOK_ID,
      )
    val bookRepository = mockk<BookRepository>()
    val mediaRepository = mockk<MediaRepository>()
    val bookLifecycle = mockk<BookLifecycle>()
    val request = mockk<WebRequest>()
    every { bookRepository.findByIdOrNull(BOOK_ID) } returns book
    every { mediaRepository.findById(BOOK_ID) } returns Media(bookId = BOOK_ID)
    every { request.checkNotModified(any<Long>()) } returns false
    every { bookLifecycle.getBookPage(book, 1, null, 300) } throws NoSuchFileException(SENSITIVE_BOOK_PATH)
    val controller =
      controller(
        bookLifecycle = bookLifecycle,
        bookRepository = bookRepository,
        mediaRepository = mediaRepository,
      )

    val logs =
      captureLogs(BookController::class.java) {
        assertThatThrownBy {
          controller.getBookPageThumbnailByNumber(
            principal = TalesPrincipal(TalesUser(email = "reader@example.org", password = "", id = "reader-id")),
            request = request,
            bookId = BOOK_ID,
            pageNumber = 1,
          )
        }.isInstanceOf(ResponseStatusException::class.java)
      }.rendered

    assertThat(logs)
      .contains("bookId=$BOOK_ID", "errorType=NoSuchFileException")
      .doesNotContain(SENSITIVE_BOOK_PATH, "Book(name=", "NoSuchFileException:")
  }

  private fun controller(
    taskEmitter: TaskEmitter = mockk(relaxed = true),
    bookLifecycle: BookLifecycle = mockk(relaxed = true),
    bookRepository: BookRepository = mockk(relaxed = true),
    mediaRepository: MediaRepository = mockk(relaxed = true),
  ): BookController =
    BookController(
      taskEmitter = taskEmitter,
      bookAnalyzer = mockk(relaxed = true),
      bookLifecycle = bookLifecycle,
      bookRepository = bookRepository,
      bookMetadataRepository = mockk(relaxed = true),
      mediaRepository = mediaRepository,
      bookDtoRepository = mockk(relaxed = true),
      readListRepository = mockk(relaxed = true),
      contentDetector = mockk(relaxed = true),
      imageAnalyzer = mockk(relaxed = true),
      eventPublisher = mockk(relaxed = true),
      thumbnailBookRepository = mockk(relaxed = true),
      webPubGenerator = mockk(relaxed = true),
      contentRestrictionChecker = mockk(relaxed = true),
      commonBookController = mockk(relaxed = true),
    )

  private companion object {
    const val BOOK_ID = "book-id"
    const val SERIES_ID = "series-id"
    const val SENSITIVE_SOURCE_FILE = "G:\\SV1274_PRIVATE\\source.cbz"
    const val SENSITIVE_BOOK_PATH = "G:\\SV1274_PRIVATE\\book.cbz"
    const val SENSITIVE_SQL_MESSAGE = "SQL [insert into PRIVATE_TASK values (SV1274_SECRET)]"
  }
}
