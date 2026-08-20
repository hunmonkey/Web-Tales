package org.tales.shelfview.interfaces.api

import jakarta.annotation.PreDestroy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class ShelfViewPdfCacheFilesTest {
  @Test
  fun `mixed provider keeps shutdown cache cleanup hook`() {
    val method = ShelfViewZipMixedContentProvider::class.java.getDeclaredMethod("closePdfCache")

    assertThat(method.getAnnotation(PreDestroy::class.java)).isNotNull()
  }

  @Test
  fun `given cached PDF when session closes then owned file and directory are removed`(
    @TempDir root: Path,
  ) {
    val cacheFiles = ShelfViewPdfCacheFiles(root)
    val cachedPdf = cacheFiles.createTempFile()
    val session = cacheFiles.directory
    Files.write(cachedPdf, byteArrayOf(1, 2, 3))

    cacheFiles.close()

    assertThat(cachedPdf.exists()).isFalse()
    assertThat(session.exists()).isFalse()
  }

  @Test
  fun `given another live session when cache starts then active files are preserved`(
    @TempDir root: Path,
  ) {
    val first = ShelfViewPdfCacheFiles(root)
    val activePdf = first.createTempFile()
    Files.write(activePdf, byteArrayOf(1))

    val second = ShelfViewPdfCacheFiles(root)
    second.createTempFile()

    assertThat(activePdf.exists()).isTrue()

    second.close()
    first.close()
  }

  @Test
  fun `given stale process session when cache starts then only owned leftovers are removed`(
    @TempDir root: Path,
  ) {
    val staleSession = root.resolve("session_999999999_1_stale").createDirectories()
    val stalePdf = staleSession.resolve("mixed-pdf-stale.pdf")
    val unrelated = staleSession.resolve("keep.txt")
    Files.write(stalePdf, byteArrayOf(1))
    Files.write(unrelated, byteArrayOf(2))

    val cacheFiles = ShelfViewPdfCacheFiles(root)

    assertThat(stalePdf.exists()).isFalse()
    assertThat(unrelated.exists()).isTrue()

    cacheFiles.close()
  }

  @Test
  fun `given cache root symlink when cache starts then external tree is not followed`(
    @TempDir directory: Path,
  ) {
    val externalRoot = directory.resolve("external").createDirectories()
    val staleSession = externalRoot.resolve("session_999999999_1_stale").createDirectories()
    val externalPdf = staleSession.resolve("mixed-pdf-external.pdf")
    Files.write(externalPdf, byteArrayOf(1))
    val linkedRoot = directory.resolve("cache-link")
    createSymbolicLinkOrSkip(linkedRoot, externalRoot)

    val cacheFiles = ShelfViewPdfCacheFiles(linkedRoot)

    assertThat(externalPdf.exists()).isTrue()
    assertThatThrownBy { cacheFiles.createTempFile() }
      .isInstanceOf(IllegalStateException::class.java)
    cacheFiles.close()
  }

  @Test
  fun `given owned-looking symlink in stale session when cache starts then link target is preserved`(
    @TempDir root: Path,
  ) {
    val externalPdf = root.resolve("external.pdf")
    Files.write(externalPdf, byteArrayOf(1))
    val staleSession = root.resolve("session_999999999_1_stale").createDirectories()
    val linkedPdf = staleSession.resolve("mixed-pdf-linked.pdf")
    createSymbolicLinkOrSkip(linkedPdf, externalPdf)

    val cacheFiles = ShelfViewPdfCacheFiles(root)

    assertThat(linkedPdf.exists()).isTrue()
    assertThat(externalPdf.exists()).isTrue()
    cacheFiles.close()
  }

  private fun createSymbolicLinkOrSkip(
    link: Path,
    target: Path,
  ) {
    try {
      Files.createSymbolicLink(link, target)
    } catch (failure: Exception) {
      assumeTrue(false, "Symbolic links are not available: ${failure.javaClass.simpleName}")
    }
  }
}
