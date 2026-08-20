package org.tales.interfaces.api.rest

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import org.tales.captureLogs
import org.tales.domain.model.Book
import org.tales.domain.model.Media
import org.tales.domain.model.TransientBook
import org.tales.domain.persistence.TransientBookRepository
import org.tales.domain.service.BookAnalyzer
import org.tales.domain.service.TransientBookLifecycle
import java.net.URL
import java.nio.file.NoSuchFileException
import java.time.LocalDateTime

class TransientBooksControllerLoggingTest {
  @Test
  fun `missing transient page log omits file path and rendered exception`() {
    val transientBook =
      TransientBook(
        book =
          Book(
            name = "private-book",
            url = URL("file:/SV1274_PRIVATE/book.cbz"),
            fileLastModified = LocalDateTime.now(),
            id = BOOK_ID,
          ),
        media = Media(bookId = BOOK_ID),
      )
    val repository = mockk<TransientBookRepository>()
    val lifecycle = mockk<TransientBookLifecycle>()
    every { repository.findByIdOrNull(TRANSIENT_BOOK_ID) } returns transientBook
    every { lifecycle.getBookPage(transientBook, 1) } throws NoSuchFileException(SENSITIVE_BOOK_PATH)
    val controller = TransientBooksController(lifecycle, repository, mockk<BookAnalyzer>(relaxed = true))

    val logs =
      captureLogs(TransientBooksController::class.java) {
        assertThatThrownBy { controller.getPageByTransientBookId(TRANSIENT_BOOK_ID, 1) }
          .isInstanceOf(ResponseStatusException::class.java)
      }.rendered

    assertThat(logs)
      .contains("bookId=$BOOK_ID", "errorType=NoSuchFileException")
      .doesNotContain(SENSITIVE_BOOK_PATH, "NoSuchFileException:")
  }

  private companion object {
    const val BOOK_ID = "book-id"
    const val TRANSIENT_BOOK_ID = "transient-book-id"
    const val SENSITIVE_BOOK_PATH = "G:\\SV1274_PRIVATE\\book.cbz"
  }
}
