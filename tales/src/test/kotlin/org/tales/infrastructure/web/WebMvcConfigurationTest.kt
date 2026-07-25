package org.tales.infrastructure.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class WebMvcConfigurationTest {
  @Test
  fun `given webui public files when checking resource handlers then all of them are served from the root`() {
    // given
    val publicDir = findWebuiPublicDir()
    assertThat(publicDir).describedAs("tales-webui/public directory").isNotNull()

    val publicFiles =
      Files.list(publicDir).use { paths ->
        paths
          .filter { it.isRegularFile() }
          .map { "/${it.name}" }
          .toList()
      }
    assertThat(publicFiles).describedAs("files in tales-webui/public").isNotEmpty()

    // then
    assertThat(WebMvcConfiguration.rootStaticResources)
      .describedAs("every file in tales-webui/public must be registered in WebMvcConfiguration, else it falls back to index.html")
      .containsAll(publicFiles)
  }

  private fun findWebuiPublicDir(): Path? =
    generateSequence(Path.of("").toAbsolutePath()) { it.parent }
      .map { it.resolve("tales-webui").resolve("public") }
      .firstOrNull { it.exists() }
}
