package org.tales.infrastructure.util

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

/**
 * Checks that the temp directory, obtained with System.getProperty("java.io.tmpdir"),
 * exists and is accessible.
 * If the directory does not exist, this will attempt to create it.
 *
 * @throws IllegalStateException if the temp directory cannot be created,
 * or is not accessible, or if the system property 'java.io.tmpdir' is not defined.
 */
fun checkTempDirectory() {
  System.getProperty("java.io.tmpdir")?.let {
    val tmpDir = Path(it)

    if (Files.notExists(tmpDir)) {
      logger.warn { "Temp directory does not exist; attempting creation" }
      try {
        Files.createDirectories(tmpDir)
        logger.info { "Created missing temp directory" }
      } catch (e: Exception) {
        logger.error { "Could not create missing temp directory: errorType=${e.javaClass.simpleName}" }
        throw IllegalStateException("Could not create the configured temp directory")
      }
    }

    check(Files.isWritable(tmpDir)) { "The configured temp directory is not writable" }
  } ?: throw IllegalStateException("System property 'java.io.tmpdir' is not defined")
}
