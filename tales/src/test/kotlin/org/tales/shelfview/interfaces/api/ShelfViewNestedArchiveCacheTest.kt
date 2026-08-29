package org.tales.shelfview.interfaces.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists

class ShelfViewNestedArchiveCacheTest {
  @Test
  fun `given cached extraction when the same key is requested then it extracts only once`(
    @TempDir root: Path,
  ) {
    ShelfViewNestedArchiveCache(root).use { cache ->
      var extractions = 0
      val extract: (Path) -> Unit = { path ->
        extractions += 1
        Files.writeString(path, "archive")
      }

      val first = cache.get("key", extract)
      val second = cache.get("key", extract)

      assertThat(extractions).isEqualTo(1)
      assertThat(second).isEqualTo(first)
      assertThat(Files.readString(first)).isEqualTo("archive")
    }
  }

  @Test
  fun `given cached file deleted underneath when requested then it extracts again`(
    @TempDir root: Path,
  ) {
    ShelfViewNestedArchiveCache(root).use { cache ->
      var extractions = 0
      val extract: (Path) -> Unit = { path ->
        extractions += 1
        Files.writeString(path, "archive")
      }

      cache.get("key", extract).deleteExisting()
      val again = cache.get("key", extract)

      assertThat(extractions).isEqualTo(2)
      assertThat(again.exists()).isTrue()
    }
  }

  @Test
  fun `given failing extraction then nothing is cached and the temp file is removed`(
    @TempDir root: Path,
  ) {
    ShelfViewNestedArchiveCache(root).use { cache ->
      assertThatThrownBy {
        cache.get("key") { throw IllegalStateException("extraction failed") }
      }.isInstanceOf(IllegalStateException::class.java)

      var extractions = 0
      val recovered =
        cache.get("key") { path ->
          extractions += 1
          Files.writeString(path, "archive")
        }

      assertThat(extractions).isEqualTo(1)
      assertThat(Files.readString(recovered)).isEqualTo("archive")
      Files.newDirectoryStream(recovered.parent).use { files ->
        assertThat(files.count()).isEqualTo(1)
      }
    }
  }

  @Test
  fun `given close then the cached files are removed`(
    @TempDir root: Path,
  ) {
    val cache = ShelfViewNestedArchiveCache(root)
    val cached = cache.get("key") { path -> Files.writeString(path, "archive") }

    cache.close()

    assertThat(cached.exists()).isFalse()
  }
}
