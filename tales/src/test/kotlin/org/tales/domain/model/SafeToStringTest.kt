package org.tales.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.LocalDateTime

class SafeToStringTest {
  @Test
  fun `book toString contains only its opaque id`() {
    val book =
      Book(
        name = SENSITIVE_NAME,
        url = URL(SENSITIVE_BOOK_URL),
        fileLastModified = LocalDateTime.now(),
        fileHash = SENSITIVE_HASH,
        id = BOOK_ID,
      )

    assertThat(book.toString())
      .isEqualTo("Book(id='$BOOK_ID')")
      .doesNotContain(SENSITIVE_NAME, SENSITIVE_BOOK_URL, SENSITIVE_HASH)
  }

  @Test
  fun `library toString contains only its opaque id`() {
    val library = Library(name = SENSITIVE_NAME, root = URL(SENSITIVE_LIBRARY_ROOT), id = LIBRARY_ID)

    assertThat(library.toString())
      .isEqualTo("Library(id='$LIBRARY_ID')")
      .doesNotContain(SENSITIVE_NAME, SENSITIVE_LIBRARY_ROOT)
  }

  @Test
  fun `series toString contains only its opaque id`() {
    val series =
      Series(
        name = SENSITIVE_NAME,
        url = URL(SENSITIVE_SERIES_URL),
        fileLastModified = LocalDateTime.now(),
        id = SERIES_ID,
      )

    assertThat(series.toString())
      .isEqualTo("Series(id='$SERIES_ID')")
      .doesNotContain(SENSITIVE_NAME, SENSITIVE_SERIES_URL)
  }

  @Test
  fun `user toString contains only its opaque id`() {
    val user = TalesUser(email = SENSITIVE_EMAIL, password = SENSITIVE_PASSWORD, id = USER_ID)

    assertThat(user.toString())
      .isEqualTo("TalesUser(id='$USER_ID')")
      .doesNotContain(SENSITIVE_EMAIL, SENSITIVE_PASSWORD)
  }

  private companion object {
    const val BOOK_ID = "book-id"
    const val LIBRARY_ID = "library-id"
    const val SERIES_ID = "series-id"
    const val USER_ID = "user-id"
    const val SENSITIVE_EMAIL = "private-user@example.org"
    const val SENSITIVE_PASSWORD = "SV1274_PRIVATE_PASSWORD"
    const val SENSITIVE_NAME = "SV1274_PRIVATE_MEDIA_NAME"
    const val SENSITIVE_BOOK_URL = "file:/SV1274_PRIVATE/book.cbz"
    const val SENSITIVE_LIBRARY_ROOT = "file:/SV1274_PRIVATE/library"
    const val SENSITIVE_SERIES_URL = "file:/SV1274_PRIVATE/library/series"
    const val SENSITIVE_HASH = "SV1274_PRIVATE_FILE_HASH"
  }
}
