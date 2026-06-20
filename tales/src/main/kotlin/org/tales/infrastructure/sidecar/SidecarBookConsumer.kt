package org.tales.infrastructure.sidecar

import org.tales.domain.model.Sidecar

interface SidecarBookConsumer {
  fun getSidecarBookType(): Sidecar.Type

  fun getSidecarBookPrefilter(): List<Regex>

  fun isSidecarBookMatch(
    basename: String,
    sidecar: String,
  ): Boolean
}
