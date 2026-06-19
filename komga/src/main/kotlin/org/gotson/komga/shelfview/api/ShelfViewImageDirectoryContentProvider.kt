package org.gotson.komga.shelfview.api

import org.apache.commons.io.FilenameUtils
import org.gotson.komga.domain.model.Dimension
import org.gotson.komga.domain.model.Media
import org.gotson.komga.domain.model.MediaProfile
import org.gotson.komga.infrastructure.image.ImageAnalyzer
import org.gotson.komga.infrastructure.mediacontainer.ContentDetector
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.name

data class ShelfViewImageDirectoryPageContent(
  val pageNumber: Int,
  val fileName: String,
  val bytes: ByteArray,
  val mediaType: String,
)

@Component
class ShelfViewImageDirectoryContentProvider(
  private val archiveProperties: ShelfViewArchiveProperties,
  private val contentDetector: ContentDetector,
  private val imageAnalyzer: ImageAnalyzer,
) {
  fun readPage(
    directory: Path,
    media: Media,
    pageNumber: Int,
  ): ShelfViewImageDirectoryPageContent {
    if (media.profile != MediaProfile.IMAGE_DIRECTORY) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_IMAGE_DIRECTORY_UNSUPPORTED_MEDIA")
    }
    if (pageNumber <= 0 || pageNumber > shelfViewPageCount(media) || pageNumber > media.pages.size) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_PAGE_NUMBER_INVALID")
    }

    val page = media.pages[pageNumber - 1]
    val imagePath = resolvePagePath(directory, page.fileName)
    val bytes = readImageBytes(imagePath)
    val mediaType = contentDetector.detectMediaType(bytes.inputStream())
    if (!allowedImageMediaTypes(page.fileName).contains(mediaType)) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_IMAGE_MIME_MISMATCH")
    }

    val dimension =
      imageAnalyzer.getDimension(bytes.inputStream())
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_IMAGE_UNREADABLE")
    validateImageDimension(dimension)

    return ShelfViewImageDirectoryPageContent(
      pageNumber = pageNumber,
      fileName = page.fileName,
      bytes = bytes,
      mediaType = mediaType,
    )
  }

  private fun resolvePagePath(
    directory: Path,
    fileName: String,
  ): Path {
    if (fileName.isBlank() || fileName.contains('/') || fileName.contains('\\')) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_IMAGE_DIRECTORY_INVALID_PAGE")
    }

    val root = directory.toAbsolutePath().normalize()
    val imagePath = root.resolve(fileName).normalize()
    if (imagePath.parent != root || imagePath.name.startsWith(".")) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_IMAGE_DIRECTORY_INVALID_PAGE")
    }
    if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(imagePath, LinkOption.NOFOLLOW_LINKS)) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "SHELFVIEW_IMAGE_DIRECTORY_PAGE_NOT_FOUND")
    }

    return imagePath
  }

  private fun readImageBytes(path: Path): ByteArray {
    val fileSize = Files.size(path)
    if (fileSize > archiveProperties.maxImageBytes) {
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_IMAGE_TOO_LARGE")
    }

    Files.newInputStream(path).use { input ->
      val output = ByteArrayOutputStream()
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      var totalBytes = 0L

      while (true) {
        val read = input.read(buffer)
        if (read < 0) break

        totalBytes += read
        if (totalBytes > archiveProperties.maxImageBytes) {
          throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_IMAGE_TOO_LARGE")
        }
        output.write(buffer, 0, read)
      }

      return output.toByteArray()
    }
  }

  private fun validateImageDimension(dimension: Dimension) {
    if (dimension.width > archiveProperties.maxImageWidth || dimension.height > archiveProperties.maxImageHeight) {
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_IMAGE_DIMENSIONS_TOO_LARGE")
    }

    if (dimension.width.toLong() * dimension.height.toLong() > archiveProperties.maxImagePixels) {
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_IMAGE_PIXELS_TOO_LARGE")
    }
  }

  private fun allowedImageMediaTypes(fileName: String): Set<String> =
    when (FilenameUtils.getExtension(fileName).lowercase()) {
      "jpg", "jpeg" -> setOf("image/jpeg")
      "png" -> setOf("image/png")
      "webp" -> setOf("image/webp")
      "gif" -> setOf("image/gif")
      else -> emptySet()
    }

  private companion object {
    const val DEFAULT_BUFFER_SIZE = 8192
  }
}
