package org.gotson.komga.shelfview.api

import jakarta.validation.constraints.Positive
import org.gotson.komga.domain.model.Book
import org.gotson.komga.shelfview.api.dto.ShelfViewTextContentDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import org.springframework.web.server.ResponseStatusException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

@Component
@ConfigurationProperties(prefix = "shelfview.text")
@Validated
class ShelfViewTextProperties {
  @Positive
  var maxBytes: Long = 5L * 1024L * 1024L

  @Positive
  var maxLineLength: Int = 20_000
}

@Component
class ShelfViewTextContentProvider(
  private val properties: ShelfViewTextProperties,
) {
  fun maxBytes(): Long = properties.maxBytes

  fun read(
    book: Book,
    contentFingerprint: String,
    requestedEncoding: String?,
  ): ShelfViewTextContentDto {
    val size = sizeOf(book.path)
    if (size > properties.maxBytes) {
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_TEXT_TOO_LARGE")
    }

    val bytes = readBytes(book.path)
    return readBytes(
      bookId = book.id,
      contentFingerprint = contentFingerprint,
      bytes = bytes,
      requestedEncoding = requestedEncoding,
    )
  }

  fun readBytes(
    bookId: String,
    contentFingerprint: String,
    bytes: ByteArray,
    requestedEncoding: String?,
  ): ShelfViewTextContentDto {
    if (bytes.size > properties.maxBytes) {
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_TEXT_TOO_LARGE")
    }

    val decoded = decode(bytes, requestedEncoding)
    val normalized = normalize(decoded.text)
    ensureLineLength(normalized)

    return ShelfViewTextContentDto(
      bookId = bookId,
      contentFingerprint = contentFingerprint,
      encoding = decoded.encoding,
      normalizationVersion = SHELFVIEW_TEXT_NORMALIZATION_VERSION,
      paragraphs = toParagraphs(normalized),
    )
  }

  private fun sizeOf(path: Path): Long =
    try {
      Files.size(path)
    } catch (ex: NoSuchFileException) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "SHELFVIEW_TEXT_FILE_NOT_FOUND", ex)
    } catch (ex: AccessDeniedException) {
      throw ResponseStatusException(HttpStatus.FORBIDDEN, "SHELFVIEW_TEXT_FILE_NOT_READABLE", ex)
    }

  private fun readBytes(path: Path): ByteArray =
    try {
      Files.readAllBytes(path)
    } catch (ex: NoSuchFileException) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "SHELFVIEW_TEXT_FILE_NOT_FOUND", ex)
    } catch (ex: AccessDeniedException) {
      throw ResponseStatusException(HttpStatus.FORBIDDEN, "SHELFVIEW_TEXT_FILE_NOT_READABLE", ex)
    }

  private fun decode(
    bytes: ByteArray,
    requestedEncoding: String?,
  ): DecodedText {
    val requested = requestedEncoding?.let { normalizeEncoding(it) }
    if (requested != null) return DecodedText(decodeStrict(bytes.stripUtf8Bom(), charsetFor(requested)), requested)

    if (bytes.hasUtf8Bom()) return DecodedText(decodeStrict(bytes.stripUtf8Bom(), Charsets.UTF_8), UTF_8)

    return try {
      DecodedText(decodeStrict(bytes, Charsets.UTF_8), UTF_8)
    } catch (_: CharacterCodingException) {
      try {
        DecodedText(decodeStrict(bytes, EUC_KR_CHARSET), EUC_KR)
      } catch (_: CharacterCodingException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_TEXT_ENCODING_UNREADABLE")
      }
    }
  }

  private fun normalizeEncoding(value: String): String =
    when (value.trim().lowercase().replace("_", "-")) {
      "utf-8", "utf8" -> UTF_8
      "euc-kr", "euckr" -> EUC_KR
      else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_TEXT_UNSUPPORTED_ENCODING")
    }

  private fun charsetFor(encoding: String): Charset =
    when (encoding) {
      UTF_8 -> Charsets.UTF_8
      EUC_KR -> EUC_KR_CHARSET
      else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_TEXT_UNSUPPORTED_ENCODING")
    }

  private fun decodeStrict(
    bytes: ByteArray,
    charset: Charset,
  ): String =
    charset
      .newDecoder()
      .onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT)
      .decode(ByteBuffer.wrap(bytes))
      .toString()
      .removePrefix("\uFEFF")

  private fun normalize(text: String): String =
    text
      .replace("\r\n", "\n")
      .replace('\r', '\n')

  private fun ensureLineLength(text: String) {
    if (text.lineSequence().any { it.length > properties.maxLineLength }) {
      throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_TEXT_LINE_TOO_LONG")
    }
  }

  private fun toParagraphs(text: String): List<String> =
    text
      .trim('\n')
      .split(PARAGRAPH_BREAK)
      .map { it.trim('\n') }
      .filter { it.isNotBlank() }

  private fun ByteArray.hasUtf8Bom(): Boolean =
    size >= 3 &&
      this[0] == UTF_8_BOM[0] &&
      this[1] == UTF_8_BOM[1] &&
      this[2] == UTF_8_BOM[2]

  private fun ByteArray.stripUtf8Bom(): ByteArray = if (hasUtf8Bom()) copyOfRange(UTF_8_BOM.size, size) else this

  private data class DecodedText(
    val text: String,
    val encoding: String,
  )

  private companion object {
    const val UTF_8 = "utf-8"
    const val EUC_KR = "euc-kr"

    val EUC_KR_CHARSET: Charset = Charset.forName("EUC-KR")
    val PARAGRAPH_BREAK = Regex("\n[ \t]*\n+")
    val UTF_8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
  }
}
