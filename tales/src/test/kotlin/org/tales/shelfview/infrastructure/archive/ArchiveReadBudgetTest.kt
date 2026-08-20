package org.tales.shelfview.infrastructure.archive

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class ArchiveReadBudgetTest {
  @Test
  fun `given more entries than configured when recording entries then rejects archive`() {
    val properties = ShelfViewArchiveProperties().apply { maxEntries = 2 }
    val budget = ArchiveReadBudget(properties)

    budget.recordEntry()
    budget.recordEntry()

    assertThatThrownBy { budget.recordEntry() }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES")
  }

  @Test
  fun `given entries over cumulative byte budget when recording sizes then rejects archive`() {
    val properties = ShelfViewArchiveProperties().apply { maxTotalUncompressedBytes = 4 }
    val budget = ArchiveReadBudget(properties)

    budget.recordBytes(3)

    assertThatThrownBy { budget.recordBytes(2) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_TOTAL_TOO_LARGE")
  }

  @Test
  fun `given unknown entry over cumulative budget when reading then rejects during stream copy`() {
    val properties = ShelfViewArchiveProperties().apply { maxTotalUncompressedBytes = 4 }
    val budget = ArchiveReadBudget(properties)
    val input = ByteArrayInputStream(ByteArray(5))

    assertThatThrownBy { input.readArchiveBytes(10, "TEST_ENTRY_TOO_LARGE", budget = budget) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_TOTAL_TOO_LARGE")
  }

  @Test
  fun `given unknown size stream over byte limit when reading bytes then rejects at limit plus one`() {
    val input = ByteArrayInputStream(ByteArray(5))

    assertThatThrownBy { input.readArchiveBytes(4, "TEST_ENTRY_TOO_LARGE") }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("TEST_ENTRY_TOO_LARGE")
  }

  @Test
  fun `given unknown size stream over byte limit when scanning then rejects at limit plus one`() {
    val input = ByteArrayInputStream(ByteArray(5))

    assertThatThrownBy {
      input.withArchiveByteLimit(4, "TEST_ENTRY_TOO_LARGE").use { it.readBytes() }
    }.isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("TEST_ENTRY_TOO_LARGE")
  }

  @Test
  fun `given declared size over byte limit when reading then rejects before touching stream`() {
    var reads = 0
    val input =
      object : InputStream() {
        override fun read(): Int {
          reads += 1
          return 0
        }
      }

    assertThatThrownBy { input.readArchiveBytes(4, "TEST_ENTRY_TOO_LARGE", declaredSize = 5) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("TEST_ENTRY_TOO_LARGE")
    assertThat(reads).isZero()
  }

  @Test
  fun `given skipped bytes over scan limit when scanning then rejects at limit plus one`() {
    val input = ByteArrayInputStream(ByteArray(5)).withArchiveByteLimit(4, "TEST_ENTRY_TOO_LARGE")

    assertThatThrownBy { input.skip(5) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("TEST_ENTRY_TOO_LARGE")
  }

  @Test
  fun `given nonempty entry with zero compressed size when checking ratio then rejects archive`() {
    assertThatThrownBy { ensureArchiveCompressionRatio(1, 0, 100.0) }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_COMPRESSION_RATIO_TOO_HIGH")
  }

  @Test
  fun `given unknown declared size when extracting bytes then rejects before invoking writer`() {
    var writerInvoked = false

    assertThatThrownBy {
      extractArchiveBytes(null, 4, "TEST_ENTRY_TOO_LARGE") {
        writerInvoked = true
      }
    }.isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_ENTRY_SIZE_UNKNOWN")
    assertThat(writerInvoked).isFalse()
  }

  @Test
  fun `given extracted bytes exceed declared size then rejects without growing output`() {
    assertThatThrownBy {
      extractArchiveBytes(1, 4, "TEST_ENTRY_TOO_LARGE") { output ->
        output.write(byteArrayOf(1, 2))
      }
    }.isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("TEST_ENTRY_TOO_LARGE")
  }
}
