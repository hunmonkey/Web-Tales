package org.tales.infrastructure.metadata

import org.tales.domain.model.Library
import org.tales.domain.model.MetadataPatchTarget

interface MetadataProvider {
  fun shouldLibraryHandlePatch(
    library: Library,
    target: MetadataPatchTarget,
  ): Boolean
}
