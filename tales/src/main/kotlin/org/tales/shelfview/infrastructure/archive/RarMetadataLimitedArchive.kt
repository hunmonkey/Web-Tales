package org.tales.shelfview.infrastructure.archive

import com.github.junrar.Archive
import com.github.junrar.io.SeekableReadOnlyByteChannel
import com.github.junrar.volume.FileVolumeManager
import com.github.junrar.volume.Volume
import com.github.junrar.volume.VolumeManager
import java.io.IOException
import java.nio.file.Path

/**
 * Applies limits to the exact bytes junrar consumes while constructing its
 * internal header index. Junrar swallows ordinary I/O failures raised by its
 * channel, so the manager also records the reason and translates it after the
 * constructor returns its bounded, partial archive.
 */
internal fun <T> withMetadataLimitedRar4(
  path: Path,
  properties: ShelfViewArchiveProperties,
  block: (Archive) -> T,
): T {
  val manager = MetadataLimitedRarVolumeManager(FileVolumeManager(path.toFile()), properties)
  val archive =
    try {
      Archive(manager, null, null)
    } catch (failure: Throwable) {
      manager.closeChannels()
      manager.failureReason?.let { throw ArchiveLimitExceededException(it) }
      throw failure
    }

  manager.failureReason?.let { reason ->
    archive.close()
    throw ArchiveLimitExceededException(reason)
  }
  manager.disableLimit()
  return archive.use(block)
}

private class MetadataLimitedRarVolumeManager(
  private val delegate: VolumeManager,
  properties: ShelfViewArchiveProperties,
) : VolumeManager {
  private val maxFileHeaders = properties.maxEntries.toLong()
  private val maxTotalHeaders = maxFileHeaders + RAR_STRUCTURAL_HEADER_ALLOWANCE
  private val maxMetadataBytes = properties.maxArchiveMetadataBytes
  private val channels = mutableListOf<SeekableReadOnlyByteChannel>()
  private var metadataBytes = 0L
  private var fileHeaders = 0L
  private var totalHeaders = 0L
  private var limitEnabled = true

  var failureReason: String? = null
    private set

  override fun nextVolume(
    archive: Archive,
    lastVolume: Volume?,
  ): Volume {
    val delegateLast = (lastVolume as? MetadataLimitedRarVolume)?.delegate ?: lastVolume
    return MetadataLimitedRarVolume(delegate.nextVolume(archive, delegateLast), this)
  }

  fun disableLimit() {
    limitEnabled = false
  }

  fun closeChannels() {
    channels.forEach { channel -> runCatching { channel.close() } }
  }

  fun wrap(channel: SeekableReadOnlyByteChannel): SeekableReadOnlyByteChannel = MetadataLimitedRarChannel(channel, this).also { channels += it }

  fun beforeRead(byteCount: Int) {
    if (!limitEnabled || byteCount <= 0) return
    if (byteCount.toLong() > maxMetadataBytes - metadataBytes) fail(RAR_METADATA_TOO_LARGE)
    metadataBytes += byteCount
  }

  fun recordHeader(bytes: ByteArray) {
    if (!limitEnabled || bytes.size < RAR_BASE_HEADER_SIZE) return
    val headerType = bytes[2].toInt() and 0xff
    val headerSize =
      (bytes[5].toInt() and 0xff) or
        ((bytes[6].toInt() and 0xff) shl 8)
    if (headerType !in RAR_KNOWN_HEADER_TYPES || headerSize < RAR_BASE_HEADER_SIZE) return

    totalHeaders += 1
    if (totalHeaders > maxTotalHeaders) fail(RAR_TOO_MANY_ENTRIES)
    if (headerType == RAR_FILE_HEADER) {
      fileHeaders += 1
      if (fileHeaders > maxFileHeaders) fail(RAR_TOO_MANY_ENTRIES)
    }
  }

  private fun fail(reason: String): Nothing {
    failureReason = reason
    throw RarMetadataLimitIOException()
  }
}

private class MetadataLimitedRarVolume(
  val delegate: Volume,
  private val manager: MetadataLimitedRarVolumeManager,
) : Volume {
  override fun getChannel(): SeekableReadOnlyByteChannel = manager.wrap(delegate.channel)

  override fun getLength(): Long = delegate.length

  override fun getArchive(): Archive = delegate.archive
}

private class MetadataLimitedRarChannel(
  private val delegate: SeekableReadOnlyByteChannel,
  private val manager: MetadataLimitedRarVolumeManager,
) : SeekableReadOnlyByteChannel {
  override fun getPosition(): Long = delegate.position

  override fun setPosition(position: Long) = delegate.setPosition(position)

  override fun read(): Int {
    manager.beforeRead(1)
    return delegate.read()
  }

  override fun read(
    buffer: ByteArray,
    offset: Int,
    count: Int,
  ): Int {
    manager.beforeRead(count)
    return delegate.read(buffer, offset, count)
  }

  override fun readFully(
    buffer: ByteArray,
    count: Int,
  ): Int {
    manager.beforeRead(count)
    val read = delegate.readFully(buffer, count)
    if (read == RAR_BASE_HEADER_SIZE && count == RAR_BASE_HEADER_SIZE) manager.recordHeader(buffer)
    return read
  }

  override fun close() = delegate.close()
}

private class RarMetadataLimitIOException : IOException()

private val RAR_KNOWN_HEADER_TYPES = 0x72..0x7b
private const val RAR_BASE_HEADER_SIZE = 7
private const val RAR_FILE_HEADER = 0x74
private const val RAR_STRUCTURAL_HEADER_ALLOWANCE = 128L
private const val RAR_TOO_MANY_ENTRIES = "SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES"
private const val RAR_METADATA_TOO_LARGE = "SHELFVIEW_ARCHIVE_METADATA_TOO_LARGE"
