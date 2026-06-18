package org.gotson.komga.shelfview.api

import org.gotson.komga.domain.model.BookPage
import org.gotson.komga.domain.model.Dimension
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.MediaType
import org.gotson.komga.domain.model.makeBook
import org.gotson.komga.domain.model.makeLibrary
import org.gotson.komga.domain.model.makeSeries
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.MediaRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.gotson.komga.domain.service.LibraryLifecycle
import org.gotson.komga.domain.service.SeriesLifecycle
import org.gotson.komga.interfaces.api.rest.WithMockCustomUser
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ShelfViewManifestControllerTest(
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val mediaRepository: MediaRepository,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val mockMvc: MockMvc,
) {
  private val library = makeLibrary(id = "1")

  @BeforeAll
  fun `setup library`() {
    libraryRepository.insert(library)
  }

  @AfterAll
  fun teardown() {
    libraryRepository.findAll().forEach {
      libraryLifecycle.deleteLibrary(it)
    }
  }

  @AfterEach
  fun `clear repository`() {
    seriesLifecycle.deleteMany(seriesRepository.findAll())
  }

  @Test
  @WithMockCustomUser
  fun `given fixed page book when getting manifest then returns fixed page response`() {
    val book = createBookWithMedia(MediaType.ZIP.type, "Book One.cbz")

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isOk() }
        jsonPath("$.schemaVersion") { value(1) }
        jsonPath("$.bookId") { value(book.id) }
        jsonPath("$.kind") { value("FIXED_PAGE_SEQUENCE") }
        jsonPath("$.sourceFormat") { value("CBZ") }
        jsonPath("$.pageCount") { value(2) }
        jsonPath("$.pages.endpointTemplate") { value("/api/v1/books/${book.id}/pages/{page}?zero_based=true") }
        jsonPath("$.capabilities.search") { value(false) }
        jsonPath("$.capabilities.doublePage") { value(true) }
        jsonPath("$.capabilities.imageFilters") { value(true) }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given pdf book when getting manifest then returns pdf source format`() {
    val book = createBookWithMedia(MediaType.PDF.type, "Book One.pdf")

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isOk() }
        jsonPath("$.sourceFormat") { value("PDF") }
        jsonPath("$.kind") { value("FIXED_PAGE_SEQUENCE") }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given epub book when getting manifest then returns bad request`() {
    val book = createBookWithMedia(MediaType.EPUB.type, "Book One.epub")

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser(sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given user without library access when getting manifest then returns forbidden`() {
    val book = createBookWithMedia(MediaType.ZIP.type, "Book One.cbz")

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/manifest")
      .andExpect {
        status { isForbidden() }
      }
  }

  private fun createBookWithMedia(
    mediaType: String,
    bookName: String,
  ) =
    makeSeries(name = "series", libraryId = library.id).let { series ->
      val created = seriesLifecycle.createSeries(series)
      seriesLifecycle.addBooks(created, listOf(makeBook(bookName, libraryId = library.id)))

      bookRepository.findAll().first().also { book ->
        mediaRepository.findById(book.id).let {
          mediaRepository.update(
            it.copy(
              status = Media.Status.READY,
              mediaType = mediaType,
              pages = listOf(
                BookPage("001.jpg", "image/jpeg", Dimension(800, 1200), fileSize = 1024),
                BookPage("002.jpg", "image/jpeg", Dimension(800, 1200), fileSize = 2048),
              ),
            ),
          )
        }
      }
    }
}
