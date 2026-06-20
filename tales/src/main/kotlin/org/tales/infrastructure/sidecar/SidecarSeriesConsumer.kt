package org.tales.infrastructure.sidecar

import org.tales.domain.model.Sidecar

interface SidecarSeriesConsumer {
  fun getSidecarSeriesType(): Sidecar.Type

  fun getSidecarSeriesFilenames(): List<String>
}
