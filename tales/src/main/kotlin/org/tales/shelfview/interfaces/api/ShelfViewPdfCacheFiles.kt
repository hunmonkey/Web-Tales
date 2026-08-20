package org.tales.shelfview.interfaces.api

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name

/**
 * Owns one process-scoped directory for mixed-archive PDF originals. A process
 * identity in each directory name lets a later JVM remove crash leftovers
 * without deleting files that another live Tales process is still using.
 */
internal class ShelfViewPdfCacheFiles(
  private val root: Path = Path.of(System.getProperty("java.io.tmpdir")).resolve("shelfview-mixed-pdf-cache"),
) : AutoCloseable {
  private val process = ProcessHandle.current()
  private val processStart =
    process
      .info()
      .startInstant()
      .orElse(Instant.EPOCH)
      .toEpochMilli()
  private val sessionName = "session_${process.pid()}_${processStart}_${UUID.randomUUID()}"
  private val directoryHolder = lazy { createSessionDirectory() }

  val directory: Path
    get() = directoryHolder.value

  init {
    cleanupStaleSessions()
  }

  fun createTempFile(): Path = Files.createTempFile(directory, "mixed-pdf-", ".pdf")

  fun delete(path: Path) {
    if (path.parent != directory || !isOwnedCacheFile(path)) return
    try {
      path.deleteIfExists()
    } catch (_: Exception) {
      // A cached PDF document may still hold the file on Windows. The next
      // process safely retries after this process identity is no longer live.
    }
  }

  override fun close() {
    if (directoryHolder.isInitialized()) cleanupSession(directory)
  }

  private fun createSessionDirectory(): Path {
    if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
      check(Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) { "PDF cache root is not a directory" }
    } else {
      Files.createDirectories(root)
    }
    check(Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) { "PDF cache root is not a directory" }
    return Files.createDirectory(root.resolve(sessionName))
  }

  private fun cleanupStaleSessions() {
    try {
      if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return
      Files.newDirectoryStream(root, "session_*").use { sessions ->
        sessions.forEach { session ->
          if (Files.isDirectory(session, LinkOption.NOFOLLOW_LINKS) && !isLiveSession(session.name)) {
            cleanupSession(session)
          }
        }
      }
    } catch (_: Exception) {
      // Cache cleanup must never prevent the application from starting.
    }
  }

  private fun cleanupSession(session: Path) {
    try {
      if (session.parent != root || !Files.isDirectory(session, LinkOption.NOFOLLOW_LINKS)) return
      Files.newDirectoryStream(session).use { files ->
        files.forEach { file ->
          if (isOwnedCacheFile(file)) file.deleteIfExists()
        }
      }
      session.deleteIfExists()
    } catch (_: Exception) {
      // An active document or an unrelated file leaves the directory in place;
      // a later startup can retry without following links or deleting broadly.
    }
  }

  private fun isLiveSession(name: String): Boolean {
    val parts = name.split('_', limit = 4)
    if (parts.size != 4 || parts[0] != "session") return true
    val pid = parts[1].toLongOrNull() ?: return true
    val recordedStart = parts[2].toLongOrNull() ?: return true

    return try {
      val handle = ProcessHandle.of(pid).orElse(null) ?: return false
      if (!handle.isAlive) return false
      val actualStart =
        handle
          .info()
          .startInstant()
          .orElse(null)
          ?.toEpochMilli()
      recordedStart == 0L || actualStart == null || actualStart == recordedStart
    } catch (_: Exception) {
      true
    }
  }

  private fun isOwnedCacheFile(path: Path): Boolean =
    path.name.startsWith("mixed-pdf-") &&
      path.name.endsWith(".pdf") &&
      Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
}
