package org.tales.domain.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.tales.domain.model.ReadListMatch
import org.tales.domain.model.ReadListRequest
import org.tales.domain.model.ReadListRequestMatch
import org.tales.domain.persistence.ReadListRepository
import org.tales.domain.persistence.ReadListRequestRepository

private val logger = KotlinLogging.logger {}

@Service
class ReadListMatcher(
  private val readListRepository: ReadListRepository,
  private val readListRequestRepository: ReadListRequestRepository,
) {
  fun matchReadListRequest(request: ReadListRequest): ReadListRequestMatch {
    logger.info { "Trying to match $request" }

    val readListMatch =
      if (readListRepository.existsByName(request.name))
        ReadListMatch(request.name, "ERR_1009")
      else
        ReadListMatch(request.name)

    val matches = readListRequestRepository.matchBookRequests(request.books)

    return ReadListRequestMatch(readListMatch, matches)
  }
}
