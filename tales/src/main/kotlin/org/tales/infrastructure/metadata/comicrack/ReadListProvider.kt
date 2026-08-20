package org.tales.infrastructure.metadata.comicrack

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.tales.domain.model.ComicRackListException
import org.tales.domain.model.ReadListRequest
import org.tales.domain.model.ReadListRequestBook
import org.tales.infrastructure.metadata.comicrack.dto.ReadingList

private val logger = KotlinLogging.logger {}

@Service
class ReadListProvider(
  @param:Autowired(required = false) private val mapper: XmlMapper = XmlMapper(),
) {
  @Throws(ComicRackListException::class)
  fun importFromCbl(cbl: ByteArray): ReadListRequest {
    val readingList =
      try {
        mapper.readValue(cbl, ReadingList::class.java)
      } catch (e: Exception) {
        logger.error { "Error while parsing ComicRack reading list: errorType=${e.javaClass.simpleName}" }
        throw ComicRackListException("Error while trying to parse ComicRack ReadingList", "ERR_1015")
      }
    logger.debug { "Converting ComicRack reading list" }

    if (readingList.name.isNullOrBlank()) throw ComicRackListException("ReadingList has no Name element", "ERR_1030")
    if (readingList.books.isEmpty()) throw ComicRackListException("ReadingList does not contain any Book element", "ERR_1029")

    val books =
      readingList.books.map {
        if (it.series.isNullOrBlank() || it.number == null) throw ComicRackListException("Book is missing series or number: $it", "ERR_1031")
        val series = setOfNotNull(computeSeriesFromSeriesAndVolume(it.series, it.volume), it.series?.ifBlank { null })
        ReadListRequestBook(series, it.number!!.trim())
      }

    return ReadListRequest(name = readingList.name!!, books = books)
      .also { logger.debug { "Converted ComicRack reading list: bookCount=${books.size}" } }
  }
}
