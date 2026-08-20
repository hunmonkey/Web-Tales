package org.tales.infrastructure.configuration

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import org.tales.domain.model.ConfigurationException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

@Component
class ConfigurationChecker(
  private val talesProperties: TalesProperties,
) {
  private val blockTypes = listOf("cifs", "nfs")

  @PostConstruct
  fun checkDatabasesPath() {
    checkDatabaseIsLocal(talesProperties.database, "tales.database.check-local-filesystem: false")
    checkDatabaseIsLocal(talesProperties.tasksDb, "tales.tasks-db.check-local-filesystem: false")
  }

  private fun checkDatabaseIsLocal(
    database: TalesProperties.Database,
    ignoreProp: String,
  ) {
    if (database.checkLocalFilesystem) {
      val path =
        try {
          Path(database.file)
        } catch (_: Exception) {
          return
        }
      checkIfRemote(path, ignoreProp)
      if (!path.exists()) checkIfRemote(path.parent, ignoreProp)
    }
  }

  private fun checkIfRemote(
    path: Path?,
    ignoreProp: String,
  ) {
    if (path == null) return
    if (path.exists()) {
      val storeType =
        try {
          Files.getFileStore(path).type().lowercase()
        } catch (e: Exception) {
          logger.warn { "Could not get FileStore type: errorType=${e.javaClass.simpleName}" }
          "unknown"
        }

      if (blockTypes.any { storeType.startsWith(it, ignoreCase = true) }) {
        val errorMessage =
          "The configured database path must be on a local filesystem, but was detected on a remote filesystem " +
            "($storeType). If this is inaccurate you can set '$ignoreProp' to ignore this check."

        logger.error { "Configuration path uses a blocked filesystem type: fileStoreType=$storeType, overrideProperty=$ignoreProp" }
        throw ConfigurationException(errorMessage)
      } else {
        logger.debug { "Configuration FileStore type: $storeType" }
      }
    }
  }
}
