package org.tales.shelfview.interfaces.api

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.model.Book
import org.tales.domain.persistence.LibraryRepository
import org.tales.shelfview.interfaces.api.dto.ShelfViewTextContentDto
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SeekableByteChannel
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@Component
@ConfigurationProperties(prefix = "shelfview.text")
@Validated
class ShelfViewTextProperties {
  @Max(2_147_483_646)
  @Positive
  var maxBytes: Long = 5L * 1024L * 1024L

  @Positive
  var maxLineLength: Int = 20_000
}

@Component
class ShelfViewTextContentProvider(
  private val properties: ShelfViewTextProperties,
  private val libraryRepository: LibraryRepository,
) {
  fun maxBytes(): Long = properties.maxBytes

  fun read(
    book: Book,
    contentFingerprint: String,
    requestedEncoding: String?,
  ): ShelfViewTextContentDto {
    val library =
      libraryRepository.findByIdOrNull(book.libraryId)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "SHELFVIEW_TEXT_LIBRARY_NOT_FOUND")
    val bytes = readBytes(book.path, library.path)
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

  private fun readBytes(
    path: Path,
    libraryRoot: Path,
  ): ByteArray =
    try {
      openAuthorizedTextFile(path, libraryRoot).use { channel ->
        readShelfViewTextBytes(channel, properties.maxBytes)
      }
    } catch (ex: ResponseStatusException) {
      throw ex
    } catch (ex: NoSuchFileException) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "SHELFVIEW_TEXT_FILE_NOT_FOUND", ex)
    } catch (ex: AccessDeniedException) {
      throw ResponseStatusException(HttpStatus.FORBIDDEN, "SHELFVIEW_TEXT_FILE_NOT_READABLE", ex)
    } catch (ex: SecurityException) {
      throw ResponseStatusException(HttpStatus.FORBIDDEN, "SHELFVIEW_TEXT_FILE_NOT_READABLE", ex)
    } catch (ex: IOException) {
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

internal fun readShelfViewTextBytes(
  channel: SeekableByteChannel,
  maxBytes: Long,
): ByteArray {
  require(maxBytes in 1 until Int.MAX_VALUE) { "TXT byte limit must be lower than Int.MAX_VALUE" }
  if (channel.size() > maxBytes) {
    throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_TEXT_TOO_LARGE")
  }

  val sizeAgnosticChannel =
    object : ReadableByteChannel {
      override fun read(destination: ByteBuffer): Int = channel.read(destination)

      override fun isOpen(): Boolean = channel.isOpen

      override fun close() = channel.close()
    }
  val bytes = Channels.newInputStream(sizeAgnosticChannel).readNBytes((maxBytes + 1).toInt())
  if (bytes.size.toLong() > maxBytes) {
    throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "SHELFVIEW_TEXT_TOO_LARGE")
  }
  return bytes
}

private fun openAuthorizedTextFile(
  path: Path,
  libraryRoot: Path,
): SeekableByteChannel {
  val realRoot = libraryRoot.toRealPath()
  if (!Files.isDirectory(realRoot, LinkOption.NOFOLLOW_LINKS)) textPathForbidden()

  val requestedPath = path.toAbsolutePath().normalize()
  if (Files.isSymbolicLink(requestedPath)) textPathForbidden()
  val realPath = requestedPath.toRealPath()
  if (realPath == realRoot || !realPath.startsWith(realRoot)) textPathForbidden()
  if (!Files.isRegularFile(realPath, LinkOption.NOFOLLOW_LINKS)) textPathForbidden()

  val channel =
    Files.newByteChannel(
      realPath,
      setOf(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS),
    )
  try {
    if (Files.isSymbolicLink(requestedPath) || requestedPath.toRealPath() != realPath) textPathForbidden()
    return channel
  } catch (failure: Throwable) {
    channel.close()
    throw failure
  }
}

private fun textPathForbidden(): Nothing = throw ResponseStatusException(HttpStatus.FORBIDDEN, "SHELFVIEW_TEXT_PATH_OUTSIDE_LIBRARY")
