package org.gotson.komga.infrastructure.hash

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class HasherTest {
  private val hasher = Hasher()

  @Test
  fun `given directory when file content changes then hash changes`(
    @TempDir directory: Path,
  ) {
    Files.writeString(directory.resolve("10.txt"), "ten")
    Files.writeString(directory.resolve("2.txt"), "two")

    val firstHash = hasher.computeHash(directory)
    val secondHash = hasher.computeHash(directory)

    assertThat(secondHash).isEqualTo(firstHash)

    Files.writeString(directory.resolve("2.txt"), "two changed")

    assertThat(hasher.computeHash(directory)).isNotEqualTo(firstHash)
  }
}
