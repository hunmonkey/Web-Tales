package org.tales.interfaces.api

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.server.ResponseStatusException
import org.tales.captureLogs
import org.tales.domain.model.Book
import org.tales.domain.model.Media
import org.tales.domain.model.TalesUser
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.domain.service.BookLifecycle
import org.tales.infrastructure.security.TalesPrincipal
import java.net.URL
import java.nio.file.NoSuchFileException
import java.time.LocalDateTime

class CommonBookControllerLoggingTest {
  @Test
  fun `missing page log omits file path and rendered exception`() {
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
    val request = mockk<ServletWebRequest>()
    every { bookRepository.findByIdOrNull(BOOK_ID) } returns book
    every { mediaRepository.findById(BOOK_ID) } returns Media(bookId = BOOK_ID)
    every { request.checkNotModified(any<Long>()) } returns false
    every { bookLifecycle.getBookPage(book, 1, null, null) } throws NoSuchFileException(SENSITIVE_BOOK_PATH)
    val controller =
      CommonBookController(
        mediaRepository = mediaRepository,
        bookRepository = bookRepository,
        bookDtoRepository = mockk(relaxed = true),
        seriesMetadataRepository = mockk(relaxed = true),
        bookLifecycle = bookLifecycle,
        bookAnalyzer = mockk(relaxed = true),
        contentRestrictionChecker = mockk(relaxed = true),
        contentDetector = mockk(relaxed = true),
        readProgressRepository = mockk(relaxed = true),
      )

    val logs =
      captureLogs(CommonBookController::class.java) {
        assertThatThrownBy {
          controller.getBookPageInternal(
            bookId = BOOK_ID,
            pageNumber = 1,
            convertTo = null,
            request = request,
            principal = TalesPrincipal(TalesUser(email = "reader@example.org", password = "", id = "reader-id")),
            acceptHeaders = null,
          )
        }.isInstanceOf(ResponseStatusException::class.java)
      }.rendered

    assertThat(logs)
      .contains("bookId=$BOOK_ID", "errorType=NoSuchFileException")
      .doesNotContain(SENSITIVE_BOOK_PATH, "NoSuchFileException:")
  }

  private companion object {
    const val BOOK_ID = "book-id"
    const val SENSITIVE_BOOK_PATH = "G:\\SV1274_PRIVATE\\book.cbz"
  }
}
