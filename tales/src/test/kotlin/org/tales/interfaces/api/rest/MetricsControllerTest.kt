package org.tales.interfaces.api.rest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.ReadListRepository
import org.tales.domain.persistence.SeriesCollectionRepository
import org.tales.domain.persistence.SeriesRepository
import org.tales.domain.persistence.SidecarRepository
import org.tales.domain.persistence.TalesUserRepository
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@ActiveProfiles("test")
class MetricsControllerTest(
  @Autowired private val mockMvc: MockMvc,
) {
  @MockkBean(relaxed = true)
  private lateinit var libraryRepository: LibraryRepository

  @MockkBean(relaxed = true)
  private lateinit var bookRepository: BookRepository

  @MockkBean(relaxed = true)
  private lateinit var seriesRepository: SeriesRepository

  @MockkBean(relaxed = true)
  private lateinit var collectionRepository: SeriesCollectionRepository

  @MockkBean(relaxed = true)
  private lateinit var readListRepository: ReadListRepository

  @MockkBean(relaxed = true)
  private lateinit var sidecarRepository: SidecarRepository

  @MockkBean(relaxed = true)
  private lateinit var userRepository: TalesUserRepository

  @BeforeEach
  fun setup() {
    every { libraryRepository.count() } returns 2
    every { seriesRepository.count() } returns 5
    every { bookRepository.count() } returns 12
    every { userRepository.count() } returns 3
    every { collectionRepository.count() } returns 4
    every { readListRepository.count() } returns 6
    every { seriesRepository.countGroupedByLibraryId() } returns mapOf("library1" to 2, "library2" to 3)
    every { bookRepository.countGroupedByLibraryId() } returns mapOf("library1" to 7, "library2" to 5)
    every { sidecarRepository.countGroupedByLibraryId() } returns mapOf("library1" to 1, "library2" to 2)
    every { bookRepository.getFilesizeGroupedByLibraryId() } returns
      mapOf(
        "library1" to BigDecimal("123.50"),
        "library2" to BigDecimal("200.25"),
      )
  }

  @Test
  @WithMockCustomUser(roles = ["ADMIN"])
  fun `given admin user when getting metrics summary then returns repository counts`() {
    mockMvc
      .get("/api/v2/metrics/summary")
      .andExpect {
        status { isOk() }
        jsonPath("$.totals.libraries") { value(2) }
        jsonPath("$.totals.series") { value(5) }
        jsonPath("$.totals.books") { value(12) }
        jsonPath("$.totals.users") { value(3) }
        jsonPath("$.totals.collections") { value(4) }
        jsonPath("$.totals.readlists") { value(6) }
        jsonPath("$.totals.sidecars") { value(3) }
        jsonPath("$.totals.booksFileSize") { value(323.75) }
        jsonPath("$.perLibrary.series.library1") { value(2) }
        jsonPath("$.perLibrary.books.library2") { value(5) }
        jsonPath("$.perLibrary.sidecars.library2") { value(2) }
        jsonPath("$.perLibrary.booksFileSize.library1") { value(123.50) }
        jsonPath("$.readProgressIncluded") { value(false) }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given non admin user when getting metrics summary then returns forbidden`() {
    mockMvc
      .get("/api/v2/metrics/summary")
      .andExpect {
        status { isForbidden() }
      }
  }
}
