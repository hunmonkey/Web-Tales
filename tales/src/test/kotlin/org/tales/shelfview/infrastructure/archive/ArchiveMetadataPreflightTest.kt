package org.tales.shelfview.infrastructure.archive

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveMetadataPreflightTest {
  @Test
  fun `given actual central directory entries over limit when opening zip then rejects before builder`(
    @TempDir directory: Path,
  ) {
    val path = directory.resolve("too-many.zip")
    createZip(path, 3)
    val properties = ShelfViewArchiveProperties().apply { maxEntries = 2 }

    assertThatThrownBy { withPreflightedZip(path, properties) { } }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES")
  }

  @Test
  fun `given forged declared count when opening zip then rejects mismatched central directory`(
    @TempDir directory: Path,
  ) {
    val path = directory.resolve("forged-count.zip")
    createZip(path, 2)
    val bytes = Files.readAllBytes(path)
    val eocd = bytes.lastIndexOfSignature(ZIP_EOCD_SIGNATURE)
    bytes.putUnsignedShort(eocd + 8, 1)
    bytes.putUnsignedShort(eocd + 10, 1)
    Files.write(path, bytes)

    assertMetadataFailure(path, "SHELFVIEW_ARCHIVE_METADATA_INVALID")
  }

  @Test
  fun `given nearer false end record in comment when opening zip then does not inspect an older record`(
    @TempDir directory: Path,
  ) {
    val path = directory.resolve("false-eocd.zip")
    createZip(path, 1, comment = "x".repeat(30))
    val bytes = Files.readAllBytes(path)
    val realEocd = bytes.lastIndexOfSignature(ZIP_EOCD_SIGNATURE)
    val falseEocd = realEocd + ZIP_EOCD_SIZE + 5
    bytes.putSignature(falseEocd, ZIP_EOCD_SIGNATURE)
    Files.write(path, bytes)

    assertMetadataFailure(path, "SHELFVIEW_ARCHIVE_METADATA_INVALID")
  }

  @Test
  fun `given central directory over metadata limit when opening zip then rejects it`(
    @TempDir directory: Path,
  ) {
    val path = directory.resolve("metadata.zip")
    createZip(path, 1)
    val bytes = Files.readAllBytes(path)
    val eocd = bytes.lastIndexOfSignature(ZIP_EOCD_SIGNATURE)
    val centralDirectorySize = bytes.unsignedInt(eocd + 12)
    val properties =
      ShelfViewArchiveProperties().apply {
        maxCentralDirectoryBytes = centralDirectorySize - 1
      }

    assertThatThrownBy { withPreflightedZip(path, properties) { } }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_METADATA_TOO_LARGE")
  }

  @Test
  fun `given builder metadata reads over limit when opening zip then translates guarded IO failure`(
    @TempDir directory: Path,
  ) {
    val path = directory.resolve("builder-metadata.zip")
    createZip(path, 1)
    val properties = ShelfViewArchiveProperties().apply { maxArchiveMetadataBytes = 1 }

    assertThatThrownBy { withPreflightedZip(path, properties) { } }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage("SHELFVIEW_ARCHIVE_METADATA_TOO_LARGE")
  }

  @Test
  fun `given maximum long metadata setting when opening zip then sentinel arithmetic does not overflow`(
    @TempDir directory: Path,
  ) {
    val path = directory.resolve("long-max.zip")
    createZip(path, 1)
    val properties =
      ShelfViewArchiveProperties().apply {
        maxCentralDirectoryBytes = Long.MAX_VALUE
        maxArchiveMetadataBytes = Long.MAX_VALUE
      }

    val names =
      withPreflightedZip(path, properties) { zip ->
        zip.entries
          .asSequence()
          .map { it.name }
          .toList()
      }

    assertThat(names).containsExactly("entry-1.txt")
  }

  private fun assertMetadataFailure(
    path: Path,
    reason: String,
  ) {
    assertThatThrownBy { withPreflightedZip(path, ShelfViewArchiveProperties()) { } }
      .isInstanceOf(ArchiveLimitExceededException::class.java)
      .hasMessage(reason)
  }

  private fun createZip(
    path: Path,
    entryCount: Int,
    comment: String? = null,
  ) {
    ZipOutputStream(Files.newOutputStream(path)).use { zip ->
      comment?.let(zip::setComment)
      repeat(entryCount) { index ->
        zip.putNextEntry(ZipEntry("entry-${index + 1}.txt"))
        zip.write(byteArrayOf(index.toByte()))
        zip.closeEntry()
      }
    }
  }

  private fun ByteArray.lastIndexOfSignature(signature: Long): Int {
    for (offset in size - Int.SIZE_BYTES downTo 0) {
      if (unsignedInt(offset) == signature) return offset
    }
    error("ZIP signature not found")
  }

  private fun ByteArray.putSignature(
    offset: Int,
    signature: Long,
  ) {
    repeat(Int.SIZE_BYTES) { index -> this[offset + index] = (signature shr (index * 8)).toByte() }
  }

  private fun ByteArray.putUnsignedShort(
    offset: Int,
    value: Int,
  ) {
    this[offset] = value.toByte()
    this[offset + 1] = (value shr 8).toByte()
  }

  private fun ByteArray.unsignedInt(offset: Int): Long =
    (this[offset].toLong() and 0xff) or
      ((this[offset + 1].toLong() and 0xff) shl 8) or
      ((this[offset + 2].toLong() and 0xff) shl 16) or
      ((this[offset + 3].toLong() and 0xff) shl 24)

  private companion object {
    const val ZIP_EOCD_SIGNATURE = 0x06054b50L
    const val ZIP_EOCD_SIZE = 22
  }
}
