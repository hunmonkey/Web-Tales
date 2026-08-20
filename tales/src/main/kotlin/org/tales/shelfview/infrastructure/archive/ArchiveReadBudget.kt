package org.tales.shelfview.infrastructure.archive

import org.tales.domain.model.Dimension
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

class ArchiveLimitExceededException(
  val reason: String,
) : RuntimeException(reason)

internal class ArchiveReadBudget(
  private val properties: ShelfViewArchiveProperties,
) {
  private var entryCount = 0
  private var reservedBytes = 0L

  fun recordEntry() {
    if (entryCount >= properties.maxEntries) {
      throw ArchiveLimitExceededException("SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES")
    }
    entryCount += 1
  }

  fun recordBytes(bytes: Long) {
    require(bytes >= 0) { "Archive byte count must not be negative" }
    addBytes(bytes)
  }

  private fun addBytes(bytes: Long) {
    if (bytes > properties.maxTotalUncompressedBytes - reservedBytes) {
      throw ArchiveLimitExceededException("SHELFVIEW_ARCHIVE_TOTAL_TOO_LARGE")
    }
    reservedBytes += bytes
  }
}

internal fun ensureArchiveEntrySize(
  declaredSize: Long?,
  maxBytes: Long,
  reason: String,
) {
  if (declaredSize != null && declaredSize >= 0 && declaredSize > maxBytes) {
    throw ArchiveLimitExceededException(reason)
  }
}

internal fun ensureArchiveCompressionRatio(
  uncompressedSize: Long?,
  compressedSize: Long?,
  maxRatio: Double,
) {
  if (
    uncompressedSize != null &&
    uncompressedSize >= 0 &&
    compressedSize != null &&
    compressedSize >= 0 &&
    (
      (compressedSize == 0L && uncompressedSize > 0) ||
        (compressedSize > 0 && uncompressedSize.toDouble() / compressedSize.toDouble() > maxRatio)
    )
  ) {
    throw ArchiveLimitExceededException("SHELFVIEW_ARCHIVE_COMPRESSION_RATIO_TOO_HIGH")
  }
}

internal fun ensureArchiveImageDimension(
  dimension: Dimension?,
  properties: ShelfViewArchiveProperties,
) {
  if (dimension == null) return
  if (dimension.width > properties.maxImageWidth || dimension.height > properties.maxImageHeight) {
    throw ArchiveLimitExceededException("SHELFVIEW_IMAGE_DIMENSIONS_TOO_LARGE")
  }
  if (dimension.width.toLong() * dimension.height.toLong() > properties.maxImagePixels) {
    throw ArchiveLimitExceededException("SHELFVIEW_IMAGE_PIXELS_TOO_LARGE")
  }
}

internal fun InputStream.readArchiveBytes(
  maxBytes: Long,
  reason: String,
  declaredSize: Long? = null,
  budget: ArchiveReadBudget? = null,
): ByteArray {
  require(maxBytes >= 0) { "Archive byte-array limit must not be negative" }
  if (maxBytes >= Int.MAX_VALUE) {
    throw IllegalArgumentException("Archive byte-array limit must be lower than Int.MAX_VALUE")
  }

  ensureArchiveEntrySize(declaredSize, maxBytes, reason)
  val exactSize = declaredSize?.takeIf { it >= 0 }?.toInt()
  val source =
    if (exactSize != null) {
      budget?.recordBytes(exactSize.toLong())
      this
    } else {
      budget?.let { ArchiveBudgetInputStream(this, it) } ?: this
    }
  if (exactSize != null) {
    val bytes = ByteArray(exactSize)
    val actualSize = source.readNBytes(bytes, 0, exactSize)
    if (actualSize != exactSize) throw IOException("Archive entry size does not match its metadata")
    if (source.read() != -1) throw ArchiveLimitExceededException(reason)
    return bytes
  }

  val bytes = source.readNBytes((maxBytes + 1).toInt())
  if (bytes.size.toLong() > maxBytes) throw ArchiveLimitExceededException(reason)
  return bytes
}

internal fun InputStream.withArchiveByteLimit(
  maxBytes: Long,
  reason: String,
): InputStream {
  require(maxBytes >= 0) { "Archive stream limit must not be negative" }
  return ArchiveByteLimitedInputStream(this, maxBytes, reason)
}

internal fun InputStream.withArchiveBudget(budget: ArchiveReadBudget): InputStream = ArchiveBudgetInputStream(this, budget)

internal fun OutputStream.withArchiveByteLimit(
  maxBytes: Long,
  reason: String,
): OutputStream {
  require(maxBytes >= 0) { "Archive output limit must not be negative" }
  return ArchiveByteLimitedOutputStream(this, maxBytes, reason)
}

internal fun InputStream.copyArchiveTo(
  output: OutputStream,
  maxBytes: Long,
  reason: String,
  declaredSize: Long?,
  budget: ArchiveReadBudget,
): Long {
  ensureArchiveEntrySize(declaredSize, maxBytes, reason)
  val knownSize = declaredSize?.takeIf { it >= 0 }
  val source =
    knownSize
      ?.let {
        budget.recordBytes(it)
        this
      } ?: withArchiveBudget(budget)
  val copied =
    source
      .withArchiveByteLimit(knownSize ?: maxBytes, if (knownSize == null) reason else "SHELFVIEW_ARCHIVE_ENTRY_SIZE_MISMATCH")
      .copyTo(output)
  if (knownSize?.let { it != copied } == true) {
    throw IOException("Archive entry size does not match its metadata")
  }
  return copied
}

internal fun extractArchiveBytes(
  declaredSize: Long?,
  maxBytes: Long,
  reason: String,
  budget: ArchiveReadBudget? = null,
  writer: (OutputStream) -> Unit,
): ByteArray {
  ensureArchiveEntrySize(declaredSize, maxBytes, reason)
  val exactSize = declaredSize?.takeIf { it >= 0 } ?: throw ArchiveLimitExceededException("SHELFVIEW_ARCHIVE_ENTRY_SIZE_UNKNOWN")
  if (exactSize >= Int.MAX_VALUE) throw ArchiveLimitExceededException(reason)
  budget?.recordBytes(exactSize)

  val output = ExactSizeArchiveOutputStream(exactSize.toInt(), reason)
  writer(output)
  return output.result()
}

private class ArchiveBudgetInputStream(
  input: InputStream,
  private val budget: ArchiveReadBudget,
) : FilterInputStream(input) {
  override fun read(): Int {
    val value = super.read()
    if (value != -1) budget.recordBytes(1)
    return value
  }

  override fun read(
    buffer: ByteArray,
    offset: Int,
    length: Int,
  ): Int {
    val read = super.read(buffer, offset, length)
    if (read > 0) budget.recordBytes(read.toLong())
    return read
  }

  override fun skip(byteCount: Long): Long {
    val skipped = super.skip(byteCount)
    if (skipped > 0) budget.recordBytes(skipped)
    return skipped
  }
}

private class ArchiveByteLimitedInputStream(
  input: InputStream,
  private val maxBytes: Long,
  private val reason: String,
) : FilterInputStream(input) {
  private var bytesRead = 0L
  private var markedBytesRead = 0L

  override fun read(): Int {
    val value = super.read()
    if (value == -1) return -1
    recordRead(1)
    return value
  }

  override fun read(
    buffer: ByteArray,
    offset: Int,
    length: Int,
  ): Int {
    if (length == 0) return 0
    val remainingWithSentinel = maxBytes - bytesRead + 1
    val allowed = min(length.toLong(), remainingWithSentinel.coerceAtLeast(1)).toInt()
    val read = super.read(buffer, offset, allowed)
    if (read > 0) recordRead(read.toLong())
    return read
  }

  override fun mark(readLimit: Int) {
    super.mark(readLimit)
    markedBytesRead = bytesRead
  }

  override fun reset() {
    super.reset()
    bytesRead = markedBytesRead
  }

  override fun skip(byteCount: Long): Long {
    if (byteCount <= 0) return 0
    val remainingWithSentinel = maxBytes - bytesRead + 1
    val allowed = min(byteCount, remainingWithSentinel.coerceAtLeast(1))
    val skipped = super.skip(allowed)
    if (skipped > 0) recordRead(skipped)
    return skipped
  }

  private fun recordRead(read: Long) {
    bytesRead += read
    if (bytesRead > maxBytes) throw ArchiveLimitExceededException(reason)
  }
}

private class ArchiveByteLimitedOutputStream(
  output: OutputStream,
  private val maxBytes: Long,
  private val reason: String,
) : java.io.FilterOutputStream(output) {
  private var bytesWritten = 0L

  override fun write(value: Int) {
    ensureCapacity(1)
    out.write(value)
    bytesWritten += 1
  }

  override fun write(
    source: ByteArray,
    offset: Int,
    length: Int,
  ) {
    ensureCapacity(length.toLong())
    out.write(source, offset, length)
    bytesWritten += length
  }

  private fun ensureCapacity(additionalBytes: Long) {
    if (additionalBytes > maxBytes - bytesWritten) throw ArchiveLimitExceededException(reason)
  }
}

private class ExactSizeArchiveOutputStream(
  expectedSize: Int,
  private val reason: String,
) : OutputStream() {
  private val bytes = ByteArray(expectedSize)
  private var position = 0

  override fun write(value: Int) {
    ensureCapacity(1)
    bytes[position++] = value.toByte()
  }

  override fun write(
    source: ByteArray,
    offset: Int,
    length: Int,
  ) {
    ensureCapacity(length)
    source.copyInto(bytes, destinationOffset = position, startIndex = offset, endIndex = offset + length)
    position += length
  }

  fun result(): ByteArray {
    if (position != bytes.size) throw IOException("Archive entry size does not match its metadata")
    return bytes
  }

  private fun ensureCapacity(additionalBytes: Int) {
    if (additionalBytes > bytes.size - position) throw ArchiveLimitExceededException(reason)
  }
}
