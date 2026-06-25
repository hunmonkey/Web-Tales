package org.tales.interfaces.api.rest

import io.micrometer.core.instrument.MeterRegistry
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.ReadListRepository
import org.tales.domain.persistence.SeriesCollectionRepository
import org.tales.domain.persistence.SeriesRepository
import org.tales.domain.persistence.SidecarRepository
import org.tales.domain.persistence.TalesUserRepository
import org.tales.infrastructure.openapi.OpenApiConfiguration.TagNames
import org.tales.interfaces.scheduler.METER_TASKS_EXECUTION
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("api/v2/metrics", produces = [MediaType.APPLICATION_JSON_VALUE])
class MetricsController(
  private val libraryRepository: LibraryRepository,
  private val bookRepository: BookRepository,
  private val seriesRepository: SeriesRepository,
  private val collectionRepository: SeriesCollectionRepository,
  private val readListRepository: ReadListRepository,
  private val sidecarRepository: SidecarRepository,
  private val userRepository: TalesUserRepository,
  private val meterRegistry: MeterRegistry,
) {
  @GetMapping("summary")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Retrieve server metrics summary", tags = [TagNames.MANAGEMENT])
  fun getSummary(): MetricsSummaryDto {
    val bookFileSizeByLibrary = bookRepository.getFilesizeGroupedByLibraryId()
    val sidecarsByLibrary = sidecarRepository.countGroupedByLibraryId()

    return MetricsSummaryDto(
      totals =
        MetricsTotalsDto(
          libraries = libraryRepository.count(),
          series = seriesRepository.count(),
          books = bookRepository.count(),
          users = userRepository.count(),
          collections = collectionRepository.count(),
          readlists = readListRepository.count(),
          sidecars = sidecarsByLibrary.values.sum(),
          booksFileSize = bookFileSizeByLibrary.values.fold(BigDecimal.ZERO, BigDecimal::add),
        ),
      perLibrary =
        MetricsPerLibraryDto(
          series = seriesRepository.countGroupedByLibraryId(),
          books = bookRepository.countGroupedByLibraryId(),
          sidecars = sidecarsByLibrary,
          booksFileSize = bookFileSizeByLibrary,
        ),
      tasks = getTaskMetrics(),
      readProgressIncluded = false,
    )
  }

  private fun getTaskMetrics(): MetricsTasksDto {
    val timers = meterRegistry.find(METER_TASKS_EXECUTION).timers()

    return MetricsTasksDto(
      executed =
        timers.associate { timer ->
          (timer.id.getTag("type") ?: "unknown") to timer.count().toDouble()
        },
      totalTimeSeconds =
        timers.associate { timer ->
          (timer.id.getTag("type") ?: "unknown") to timer.totalTime(TimeUnit.SECONDS)
        },
    )
  }
}

data class MetricsSummaryDto(
  val totals: MetricsTotalsDto,
  val perLibrary: MetricsPerLibraryDto,
  val tasks: MetricsTasksDto,
  val readProgressIncluded: Boolean,
)

data class MetricsTotalsDto(
  val libraries: Long,
  val series: Long,
  val books: Long,
  val users: Long,
  val collections: Long,
  val readlists: Long,
  val sidecars: Int,
  val booksFileSize: BigDecimal,
)

data class MetricsPerLibraryDto(
  val series: Map<String, Int>,
  val books: Map<String, Int>,
  val sidecars: Map<String, Int>,
  val booksFileSize: Map<String, BigDecimal>,
)

data class MetricsTasksDto(
  val executed: Map<String, Double>,
  val totalTimeSeconds: Map<String, Double>,
)
