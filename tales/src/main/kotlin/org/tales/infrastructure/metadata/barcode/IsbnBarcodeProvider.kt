package org.tales.infrastructure.metadata.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.validator.routines.ISBNValidator
import org.springframework.stereotype.Service
import org.tales.domain.model.BookMetadataPatch
import org.tales.domain.model.BookMetadataPatchCapability
import org.tales.domain.model.BookWithMedia
import org.tales.domain.model.Library
import org.tales.domain.model.MediaProfile
import org.tales.domain.model.MetadataPatchTarget
import org.tales.domain.service.BookAnalyzer
import org.tales.infrastructure.metadata.BookMetadataProvider
import java.util.EnumSet
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

private const val PAGES_LAST = 3
private const val PAGES_FIRST = 3

@Service
class IsbnBarcodeProvider(
  private val bookAnalyzer: BookAnalyzer,
  private val validator: ISBNValidator,
) : BookMetadataProvider {
  private val hints =
    mapOf(
      DecodeHintType.POSSIBLE_FORMATS to EnumSet.of(BarcodeFormat.EAN_13),
      DecodeHintType.TRY_HARDER to true,
    )

  override val capabilities = setOf(BookMetadataPatchCapability.ISBN)

  override fun getBookMetadataFromBook(book: BookWithMedia): BookMetadataPatch? {
    if (book.media.profile == MediaProfile.EPUB) return null

    val pagesToTry =
      (1..book.media.pageCount).toList().let {
        (it.takeLast(PAGES_LAST).reversed() + it.take(PAGES_FIRST)).distinct()
      }

    for (p in pagesToTry) {
      try {
        val imageBytes = bookAnalyzer.getPageContent(book, p)
        ImageIO.read(imageBytes.inputStream())?.let { image ->
          val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
          val source = RGBLuminanceSource(image.width, image.height, pixels)
          val bitmap = BinaryBitmap(HybridBinarizer(source))

          val result =
            try {
              MultiFormatReader().decode(bitmap, hints)
            } catch (e: Exception) {
              null
            }

          if (result == null || result.text == null) {
            logger.debug { "Book page does not contain a barcode: bookId=${book.book.id}, page=$p" }
          } else {
            if (validator.isValid(result.text)) {
              logger.debug { "Book page contains a valid ISBN barcode: bookId=${book.book.id}, page=$p" }
              return BookMetadataPatch(isbn = validator.validate(result.text))
            } else {
              logger.debug { "Book page contains an invalid ISBN barcode: bookId=${book.book.id}, page=$p" }
            }
          }
        }
      } catch (e: Exception) {
        logger.error { "Error while processing barcode page: bookId=${book.book.id}, page=$p, errorType=${e.javaClass.simpleName}" }
      }
    }

    return null
  }

  override fun shouldLibraryHandlePatch(
    library: Library,
    target: MetadataPatchTarget,
  ): Boolean =
    when (target) {
      MetadataPatchTarget.BOOK -> library.importBarcodeIsbn
      else -> false
    }
}
