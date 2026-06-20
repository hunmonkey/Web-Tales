package org.tales.interfaces.api.kobo.persistence

import org.tales.interfaces.api.kobo.dto.KoboBookMetadataDto

interface KoboDtoRepository {
  fun findBookMetadataByIds(
    bookIds: Collection<String>,
  ): Collection<KoboBookMetadataDto>
}
