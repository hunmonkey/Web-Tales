package org.gotson.komga.shelfview.api

import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.BookPage
import org.gotson.komga.domain.model.Dimension
import org.gotson.komga.domain.model.Media
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
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import org.gotson.komga.domain.model.MediaType as KomgaMediaType

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@TestPropertySource(
  properties = [
    "shelfview.archive.max-image-bytes=2048",
    "shelfview.archive.max-image-width=64",
    "shelfview.archive.max-image-height=64",
    "shelfview.archive.max-image-pixels=4096",
  ],
)
class ShelfViewImageDirectoryPageControllerTest(
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val seriesLifecycle: SeriesLifecycle,
  @Autowired private val mediaRepository: MediaRepository,
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val bookRepository: BookRepository,
  @Autowired private val mockMvc: MockMvc,
) {
  private val library = makeLibrary(id = "shelfview-image-directory-page-library")

  @BeforeAll
  fun `setup library`() {
    libraryRepository.insert(library)
  }

  @AfterEach
  fun `clear repository`() {
    seriesLifecycle.deleteMany(seriesRepository.findAll())
  }

  @AfterAll
  fun teardown() {
    libraryRepository.findAll().forEach {
      libraryLifecycle.deleteLibrary(it)
    }
  }

  @Test
  @WithMockCustomUser
  fun `given image directory book when getting page then returns image bytes with cache headers`(
    @TempDir dir: Path,
  ) {
    val imageBytes = jpegBytes()
    Files.write(dir.resolve("001.jpg"), imageBytes)
    val book = createImageDirectoryBook(dir)

    val response =
      mockMvc
        .get("/api/shelfview/v1/books/${book.id}/image-directory/pages/0?zero_based=true")
        .andExpect {
          status { isOk() }
          header { string(HttpHeaders.CONTENT_TYPE, "image/jpeg") }
          header { exists(HttpHeaders.LAST_MODIFIED) }
          header { string(HttpHeaders.CACHE_CONTROL, "max-age=0, must-revalidate, private") }
          content { bytes(imageBytes) }
        }.andReturn()

    val lastModified = response.response.getHeader(HttpHeaders.LAST_MODIFIED)
    assertThat(lastModified).isNotBlank

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/image-directory/pages/0?zero_based=true") {
        header(HttpHeaders.IF_MODIFIED_SINCE, lastModified!!)
      }.andExpect {
        status { isNotModified() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given image directory page with wrong mime when getting page then returns bad request`(
    @TempDir dir: Path,
  ) {
    Files.writeString(dir.resolve("001.jpg"), "not an image", StandardCharsets.UTF_8)
    val book = createImageDirectoryBook(dir)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/image-directory/pages/1")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given image directory page over dimension limit when getting page then returns payload too large`(
    @TempDir dir: Path,
  ) {
    Files.write(dir.resolve("001.jpg"), jpegBytes(width = 65, height = 1))
    val book = createImageDirectoryBook(dir, dimension = Dimension(65, 1))

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/image-directory/pages/1")
      .andExpect {
        status { isPayloadTooLarge() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given image directory invalid page number when getting page then returns bad request`(
    @TempDir dir: Path,
  ) {
    Files.write(dir.resolve("001.jpg"), jpegBytes())
    val book = createImageDirectoryBook(dir)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/image-directory/pages/2")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser
  fun `given non image directory book when getting image directory page then returns bad request`(
    @TempDir dir: Path,
  ) {
    Files.write(dir.resolve("001.jpg"), jpegBytes())
    val book = createImageDirectoryBook(dir, mediaType = KomgaMediaType.ZIP.type)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/image-directory/pages/1")
      .andExpect {
        status { isBadRequest() }
      }
  }

  @Test
  @WithMockCustomUser(sharedAllLibraries = false, sharedLibraries = ["other"])
  fun `given user without library access when getting image directory page then returns forbidden`(
    @TempDir dir: Path,
  ) {
    Files.write(dir.resolve("001.jpg"), jpegBytes())
    val book = createImageDirectoryBook(dir)

    mockMvc
      .get("/api/shelfview/v1/books/${book.id}/image-directory/pages/1")
      .andExpect {
        status { isForbidden() }
      }
  }

  private fun createImageDirectoryBook(
    directory: Path,
    mediaType: String = KomgaMediaType.IMAGE_DIRECTORY.type,
    dimension: Dimension = Dimension(2, 2),
  ): Book {
    val createdSeries = seriesLifecycle.createSeries(makeSeries(name = "series", libraryId = library.id))
    seriesLifecycle.addBooks(
      createdSeries,
      listOf(
        makeBook(
          "Image Folder",
          libraryId = library.id,
          url = directory.toUri().toURL(),
        ),
      ),
    )

    val book =
      bookRepository
        .findAll()
        .first { it.libraryId == library.id && it.name == "Image Folder" }
        .copy(
          url = directory.toUri().toURL(),
          fileSize = 0,
          fileHash = IMAGE_DIRECTORY_HASH,
        )
    bookRepository.update(book)

    val pages =
      listOf(
        BookPage(
          fileName = "001.jpg",
          mediaType = "image/jpeg",
          dimension = dimension,
          fileSize = Files.size(directory.resolve("001.jpg")),
        ),
      )

    mediaRepository.findById(book.id).let {
      mediaRepository.update(
        it.copy(
          status = Media.Status.READY,
          mediaType = mediaType,
          pages = pages,
          pageCount = pages.size,
        ),
      )
    }

    return book
  }

  private fun jpegBytes(
    width: Int = 2,
    height: Int = 2,
  ): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    try {
      graphics.color = Color.WHITE
      graphics.fillRect(0, 0, width, height)
    } finally {
      graphics.dispose()
    }

    return ByteArrayOutputStream().use { output ->
      ImageIO.write(image, "JPEG", output)
      output.toByteArray()
    }
  }

  private companion object {
    const val IMAGE_DIRECTORY_HASH = "image-directory-page-hash"
  }
}
