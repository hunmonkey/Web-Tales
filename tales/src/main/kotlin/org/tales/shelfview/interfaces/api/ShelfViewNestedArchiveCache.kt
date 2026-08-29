package org.tales.shelfview.interfaces.api

import com.github.benmanes.caffeine.cache.Caffeine
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists

/**
 * Extracted nested archives kept between page requests. Serving one page of a
 * CBZ inside a ZIP used to decompress the whole inner archive to a temp file
 * and delete it again, for every page and every prefetch; here the extraction
 * happens once and later pages reopen the same local file. An entry that falls
 * out is extracted again, so eviction costs time, never correctness.
 *
 * Eight entries bounds the temp disk at eight times the nested-archive size
 * limit; a reader sits inside one inner archive at a time, so in practice one
 * or two are warm.
 */
internal class ShelfViewNestedArchiveCache(
  root: Path = Path.of(System.getProperty("java.io.tmpdir")).resolve("shelfview-nested-zip-cache"),
) : AutoCloseable {
  private val files = ShelfViewPdfCacheFiles(root, "nested-zip-", ".zip")

  private val cache =
    Caffeine
      .newBuilder()
      .maximumSize(8)
      .expireAfterAccess(30, TimeUnit.MINUTES)
      .executor { command -> command.run() }
      .removalListener<String, Path> { _, path, _ -> path?.let(files::delete) }
      .build<String, Path>()

  /**
   * Returns the extracted archive for [key], running [extract] into a fresh
   * temp file on a miss. Concurrent callers of the same key share one
   * extraction. A cached file deleted underneath the cache is extracted again.
   */
  fun get(
    key: String,
    extract: (Path) -> Unit,
  ): Path {
    cache.getIfPresent(key)?.let { cached ->
      if (cached.exists()) return cached
      cache.invalidate(key)
    }

    return requireNotNull(
      cache.get(key) {
        val tempPath = files.createTempFile()
        try {
          extract(tempPath)
          tempPath
        } catch (ex: Exception) {
          files.delete(tempPath)
          throw ex
        }
      },
    )
  }

  override fun close() {
    cache.invalidateAll()
    cache.cleanUp()
    files.close()
  }
}
