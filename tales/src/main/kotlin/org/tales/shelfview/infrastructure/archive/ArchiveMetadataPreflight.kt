package org.tales.shelfview.infrastructure.archive

import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.NonWritableChannelException
import java.nio.channels.SeekableByteChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.math.min

/**
 * Reads only bounded archive metadata before format libraries build their own
 * in-memory entry indexes. The later extractor pass still validates the same
 * limits while it consumes entries; this pass closes the allocation window
 * before `ZipFile` or junrar is constructed.
 */
internal fun <T> withPreflightedZip(
  path: Path,
  properties: ShelfViewArchiveProperties,
  configure: ZipFile.Builder.() -> Unit = {},
  block: (ZipFile) -> T,
): T =
  FileChannel.open(path, StandardOpenOption.READ).use { channel ->
    preflightZipArchive(channel, properties)
    val limitedChannel = MetadataReadLimitedChannel(channel, properties.maxArchiveMetadataBytes)
    val builder = ZipFile.builder()
    builder.configure()
    builder.setSeekableByteChannel(limitedChannel)
    try {
      val zip = builder.get()
      limitedChannel.disableLimit()
      zip.use(block)
    } catch (exception: IOException) {
      if (generateSequence<Throwable>(exception) { it.cause }.any { it is ArchiveMetadataReadLimitException }) {
        archiveMetadataTooLarge()
      }
      throw exception
    }
  }

private fun preflightZipArchive(
  channel: FileChannel,
  properties: ShelfViewArchiveProperties,
) {
  val directory = channel.readZipDirectory()
  if (directory.size > properties.maxCentralDirectoryBytes) archiveMetadataTooLarge()

  val scannedEntries =
    channel.scanZipCentralDirectory(
      start = directory.start,
      size = directory.size,
      maxEntries = properties.maxEntries,
    )
  if (scannedEntries != directory.entryCount) archiveMetadataInvalid()
}

private fun FileChannel.readZipDirectory(): ZipDirectory {
  val fileSize = size()
  if (fileSize < ZIP_EOCD_SIZE) archiveMetadataInvalid()

  val tailSize = min(fileSize, ZIP_EOCD_SIZE + ZIP_MAX_COMMENT_SIZE).toInt()
  val tailStart = fileSize - tailSize
  val tail = ByteArray(tailSize)
  readFully(tailStart, ByteBuffer.wrap(tail))
  val eocdOffset = tail.findZipEocdOffset()
  if (eocdOffset < 0) archiveMetadataInvalid()
  val eocdPosition = tailStart + eocdOffset

  val diskNumber = tail.unsignedShort(eocdOffset + 4)
  val directoryDisk = tail.unsignedShort(eocdOffset + 6)
  val entriesOnDisk = tail.unsignedShort(eocdOffset + 8)
  val totalEntries = tail.unsignedShort(eocdOffset + 10)
  val directorySize = tail.unsignedInt(eocdOffset + 12)
  val directoryOffset = tail.unsignedInt(eocdOffset + 16)
  val hasZip64Sentinel =
    entriesOnDisk == ZIP16_MAGIC ||
      totalEntries == ZIP16_MAGIC ||
      directorySize == ZIP32_MAGIC ||
      directoryOffset == ZIP32_MAGIC
  val locatorPosition = eocdPosition - ZIP64_LOCATOR_SIZE
  val locator = locatorPosition.takeIf { it >= 0 }?.let { readBytes(it, ZIP64_LOCATOR_SIZE) }
  val hasZip64Locator = locator?.unsignedInt(0) == ZIP64_LOCATOR_SIGNATURE

  if (!hasZip64Locator) {
    if (hasZip64Sentinel) archiveMetadataInvalid()
    if (diskNumber != 0 || directoryDisk != 0 || entriesOnDisk != totalEntries) archiveMetadataInvalid()
    if (directorySize > eocdPosition) archiveMetadataInvalid()
    val computedStart = eocdPosition - directorySize
    if (directoryOffset > computedStart) archiveMetadataInvalid()
    return ZipDirectory(
      start = computedStart,
      size = directorySize,
      entryCount = totalEntries.toLong(),
    )
  }

  requireNotNull(locator)
  if (locator.unsignedInt(4) != 0L || locator.unsignedInt(16) != 1L) archiveMetadataInvalid()

  val zip64Position = locator.positiveLong(8)
  if (zip64Position > locatorPosition - ZIP64_EOCD_MIN_SIZE) archiveMetadataInvalid()
  val zip64 = readBytes(zip64Position, ZIP64_EOCD_MIN_SIZE)
  if (zip64.unsignedInt(0) != ZIP64_EOCD_SIGNATURE) archiveMetadataInvalid()
  val recordSize = zip64.positiveLong(4)
  if (recordSize < ZIP64_EOCD_BODY_MIN_SIZE || recordSize > locatorPosition - zip64Position - 12) archiveMetadataInvalid()
  if (zip64Position + 12 + recordSize != locatorPosition) archiveMetadataInvalid()
  if (zip64.unsignedInt(16) != 0L || zip64.unsignedInt(20) != 0L) archiveMetadataInvalid()

  val zip64EntriesOnDisk = zip64.positiveLong(24)
  val zip64TotalEntries = zip64.positiveLong(32)
  val zip64DirectorySize = zip64.positiveLong(40)
  val zip64DirectoryOffset = zip64.positiveLong(48)
  if (
    zip64EntriesOnDisk != zip64TotalEntries ||
    zip64DirectoryOffset > zip64Position ||
    zip64DirectorySize != zip64Position - zip64DirectoryOffset
  ) {
    archiveMetadataInvalid()
  }
  if (diskNumber != ZIP16_MAGIC && diskNumber != 0) archiveMetadataInvalid()
  if (directoryDisk != ZIP16_MAGIC && directoryDisk != 0) archiveMetadataInvalid()
  if (entriesOnDisk != ZIP16_MAGIC && entriesOnDisk.toLong() != zip64EntriesOnDisk) archiveMetadataInvalid()
  if (totalEntries != ZIP16_MAGIC && totalEntries.toLong() != zip64TotalEntries) archiveMetadataInvalid()
  if (directorySize != ZIP32_MAGIC && directorySize != zip64DirectorySize) archiveMetadataInvalid()
  if (directoryOffset != ZIP32_MAGIC && directoryOffset != zip64DirectoryOffset) archiveMetadataInvalid()
  return ZipDirectory(
    start = zip64DirectoryOffset,
    size = zip64DirectorySize,
    entryCount = zip64TotalEntries,
  )
}

private fun FileChannel.scanZipCentralDirectory(
  start: Long,
  size: Long,
  maxEntries: Int,
): Long {
  if (start < 0 || size < 0 || start > this.size() - size) archiveMetadataInvalid()

  val header = ByteBuffer.allocate(ZIP_CENTRAL_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
  var position = start
  var remaining = size
  var entryCount = 0L
  while (remaining > 0) {
    if (remaining < 4) archiveMetadataInvalid()
    header.clear()
    header.limit(min(remaining, ZIP_CENTRAL_HEADER_SIZE.toLong()).toInt())
    readFully(position, header)

    when (header.getInt(0).toLong() and ZIP32_MAGIC) {
      ZIP_CENTRAL_HEADER_SIGNATURE -> {
        if (remaining < ZIP_CENTRAL_HEADER_SIZE) archiveMetadataInvalid()
        val variableSize =
          header.unsignedShort(28).toLong() +
            header.unsignedShort(30).toLong() +
            header.unsignedShort(32).toLong()
        val recordSize = ZIP_CENTRAL_HEADER_SIZE + variableSize
        if (recordSize > remaining) archiveMetadataInvalid()
        entryCount += 1
        if (entryCount > maxEntries) archiveTooManyEntries()
        position += recordSize
        remaining -= recordSize
      }

      ZIP_CENTRAL_DIGITAL_SIGNATURE -> {
        if (remaining < ZIP_CENTRAL_DIGITAL_HEADER_SIZE) archiveMetadataInvalid()
        val recordSize = ZIP_CENTRAL_DIGITAL_HEADER_SIZE + header.unsignedShort(4).toLong()
        if (recordSize > remaining) archiveMetadataInvalid()
        position += recordSize
        remaining -= recordSize
      }

      ZIP_ARCHIVE_EXTRA_SIGNATURE -> {
        if (remaining < ZIP_ARCHIVE_EXTRA_HEADER_SIZE) archiveMetadataInvalid()
        val recordSize = ZIP_ARCHIVE_EXTRA_HEADER_SIZE + header.unsignedInt(4)
        if (recordSize > remaining) archiveMetadataInvalid()
        position += recordSize
        remaining -= recordSize
      }

      else -> archiveMetadataInvalid()
    }
  }
  return entryCount
}

private fun FileChannel.readBytes(
  position: Long,
  length: Int,
): ByteArray {
  if (position < 0 || position > size() - length) archiveMetadataInvalid()
  return ByteArray(length).also { readFully(position, ByteBuffer.wrap(it)) }
}

private fun FileChannel.readFully(
  position: Long,
  buffer: ByteBuffer,
) {
  var currentPosition = position
  while (buffer.hasRemaining()) {
    val read = read(buffer, currentPosition)
    if (read <= 0) archiveMetadataInvalid()
    currentPosition += read
  }
}

private fun ByteArray.findZipEocdOffset(): Int {
  for (offset in size - ZIP_EOCD_SIZE.toInt() downTo 0) {
    if (unsignedInt(offset) != ZIP_EOCD_SIGNATURE) continue

    return offset.takeIf {
      unsignedShort(offset + 20).toLong() == size.toLong() - offset - ZIP_EOCD_SIZE
    } ?: -1
  }
  return -1
}

private fun ByteArray.unsignedShort(offset: Int): Int =
  (this[offset].toInt() and 0xff) or
    ((this[offset + 1].toInt() and 0xff) shl 8)

private fun ByteArray.unsignedInt(offset: Int): Long =
  unsignedShort(offset).toLong() or
    (unsignedShort(offset + 2).toLong() shl 16)

private fun ByteArray.positiveLong(offset: Int): Long =
  ByteBuffer
    .wrap(this, offset, Long.SIZE_BYTES)
    .order(ByteOrder.LITTLE_ENDIAN)
    .long
    .takeIf { it >= 0 } ?: archiveMetadataInvalid()

private fun ByteBuffer.unsignedShort(offset: Int): Int = getShort(offset).toInt() and 0xffff

private fun ByteBuffer.unsignedInt(offset: Int): Long = getInt(offset).toLong() and ZIP32_MAGIC

private fun archiveTooManyEntries(): Nothing = throw ArchiveLimitExceededException("SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES")

private fun archiveMetadataTooLarge(): Nothing = throw ArchiveLimitExceededException("SHELFVIEW_ARCHIVE_METADATA_TOO_LARGE")

private fun archiveMetadataInvalid(): Nothing = throw ArchiveLimitExceededException("SHELFVIEW_ARCHIVE_METADATA_INVALID")

private data class ZipDirectory(
  val start: Long,
  val size: Long,
  val entryCount: Long,
)

private class MetadataReadLimitedChannel(
  private val delegate: SeekableByteChannel,
  private val maxBytes: Long,
) : SeekableByteChannel {
  private var bytesRead = 0L
  private var limitEnabled = true

  fun disableLimit() {
    limitEnabled = false
  }

  override fun read(destination: ByteBuffer): Int {
    if (!limitEnabled || !destination.hasRemaining()) return delegate.read(destination)

    val remaining = maxBytes - bytesRead
    val remainingWithSentinel = if (remaining == Long.MAX_VALUE) Long.MAX_VALUE else remaining + 1
    val allowed = min(destination.remaining().toLong(), remainingWithSentinel.coerceAtLeast(1)).toInt()
    val originalLimit = destination.limit()
    destination.limit(destination.position() + allowed)
    try {
      val read = delegate.read(destination)
      if (read > 0) {
        bytesRead += read
        if (bytesRead > maxBytes) throw ArchiveMetadataReadLimitException()
      }
      return read
    } finally {
      destination.limit(originalLimit)
    }
  }

  override fun write(source: ByteBuffer): Int = throw NonWritableChannelException()

  override fun position(): Long = delegate.position()

  override fun position(newPosition: Long): SeekableByteChannel {
    delegate.position(newPosition)
    return this
  }

  override fun size(): Long = delegate.size()

  override fun truncate(size: Long): SeekableByteChannel = throw NonWritableChannelException()

  override fun isOpen(): Boolean = delegate.isOpen

  override fun close() = delegate.close()
}

private class ArchiveMetadataReadLimitException : IOException()

private const val ZIP_EOCD_SIGNATURE = 0x06054b50L
private const val ZIP64_EOCD_SIGNATURE = 0x06064b50L
private const val ZIP64_LOCATOR_SIGNATURE = 0x07064b50L
private const val ZIP_CENTRAL_HEADER_SIGNATURE = 0x02014b50L
private const val ZIP_CENTRAL_DIGITAL_SIGNATURE = 0x05054b50L
private const val ZIP_ARCHIVE_EXTRA_SIGNATURE = 0x08064b50L
private const val ZIP16_MAGIC = 0xffff
private const val ZIP32_MAGIC = 0xffffffffL
private const val ZIP_EOCD_SIZE = 22L
private const val ZIP_MAX_COMMENT_SIZE = 65_535L
private const val ZIP64_LOCATOR_SIZE = 20
private const val ZIP64_EOCD_MIN_SIZE = 56
private const val ZIP64_EOCD_BODY_MIN_SIZE = 44L
private const val ZIP_CENTRAL_HEADER_SIZE = 46
private const val ZIP_CENTRAL_DIGITAL_HEADER_SIZE = 6L
private const val ZIP_ARCHIVE_EXTRA_HEADER_SIZE = 8L
