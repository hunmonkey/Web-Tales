package org.tales.domain.persistence

import org.tales.domain.model.ReadListRequestBook
import org.tales.domain.model.ReadListRequestBookMatches

interface ReadListRequestRepository {
  fun matchBookRequests(requests: Collection<ReadListRequestBook>): Collection<ReadListRequestBookMatches>
}
