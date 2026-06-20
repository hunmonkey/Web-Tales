package org.tales.domain.persistence

import org.tales.domain.model.Sidecar
import org.tales.domain.model.SidecarStored
import java.net.URL

interface SidecarRepository {
  fun findAll(): Collection<SidecarStored>

  fun save(
    libraryId: String,
    sidecar: Sidecar,
  )

  fun deleteByLibraryIdAndUrls(
    libraryId: String,
    urls: Collection<URL>,
  )

  fun deleteByLibraryId(libraryId: String)

  fun countGroupedByLibraryId(): Map<String, Int>
}
