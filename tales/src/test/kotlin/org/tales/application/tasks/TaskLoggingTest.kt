package org.tales.application.tasks

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Test
import org.tales.captureLogs
import org.tales.domain.model.CopyMode
import org.tales.domain.persistence.SeriesRepository

class TaskLoggingTest {
  @Test
  fun `import task toString omits source file and destination name`() {
    val task = importTask()

    assertThat(task.toString())
      .contains("ImportBook", "seriesId='$SERIES_ID'")
      .doesNotContain(SENSITIVE_SOURCE_FILE, SENSITIVE_DESTINATION_NAME)
  }

  @Test
  fun `task failure log omits import source and raw database message`() {
    val seriesRepository = mockk<SeriesRepository>()
    every { seriesRepository.findByIdOrNull(SERIES_ID) } throws DataAccessException(SENSITIVE_SQL_MESSAGE)
    val meterRegistry = SimpleMeterRegistry()
    val handler =
      TaskHandler(
        taskEmitter = mockk(relaxed = true),
        libraryRepository = mockk(relaxed = true),
        bookRepository = mockk(relaxed = true),
        seriesRepository = seriesRepository,
        libraryContentLifecycle = mockk(relaxed = true),
        bookLifecycle = mockk(relaxed = true),
        bookMetadataLifecycle = mockk(relaxed = true),
        seriesLifecycle = mockk(relaxed = true),
        seriesMetadataLifecycle = mockk(relaxed = true),
        localArtworkLifecycle = mockk(relaxed = true),
        bookImporter = mockk(relaxed = true),
        bookConverter = mockk(relaxed = true),
        bookPageEditor = mockk(relaxed = true),
        searchIndexLifecycle = mockk(relaxed = true),
        pageHashLifecycle = mockk(relaxed = true),
        meterRegistry = meterRegistry,
      )

    val logs =
      try {
        captureLogs(TaskHandler::class.java) { handler.handleTask(importTask()) }.rendered
      } finally {
        meterRegistry.close()
      }

    assertThat(logs)
      .contains("seriesId='$SERIES_ID'", "errorType=DataAccessException")
      .doesNotContain(SENSITIVE_SOURCE_FILE, SENSITIVE_DESTINATION_NAME, SENSITIVE_SQL_MESSAGE)
  }

  private fun importTask() =
    Task.ImportBook(
      sourceFile = SENSITIVE_SOURCE_FILE,
      seriesId = SERIES_ID,
      copyMode = CopyMode.COPY,
      destinationName = SENSITIVE_DESTINATION_NAME,
      upgradeBookId = null,
    )

  private companion object {
    const val SERIES_ID = "series-id"
    const val SENSITIVE_SOURCE_FILE = "G:\\SV1274_PRIVATE\\source.cbz"
    const val SENSITIVE_DESTINATION_NAME = "SV1274_PRIVATE_DESTINATION.cbz"
    const val SENSITIVE_SQL_MESSAGE = "SQL [select SV1274_PRIVATE_COLUMN from PRIVATE_TABLE]"
  }
}
