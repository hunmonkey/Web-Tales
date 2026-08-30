package org.tales.domain.service

import io.github.oshai.kotlinlogging.KotlinLogging
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.tales.application.tasks.TaskEmitter
import org.tales.domain.model.Book
import org.tales.domain.model.BookMetadata
import org.tales.domain.model.BookMetadataAggregation
import org.tales.domain.model.BookMetadataPatchCapability
import org.tales.domain.model.DomainEvent
import org.tales.domain.model.Library
import org.tales.domain.model.MarkSelectedPreference
import org.tales.domain.model.Media
import org.tales.domain.model.ReadProgress
import org.tales.domain.model.Series
import org.tales.domain.model.SeriesMetadata
import org.tales.domain.model.TalesUser
import org.tales.domain.model.ThumbnailSeries
import org.tales.domain.persistence.BookMetadataAggregationRepository
import org.tales.domain.persistence.BookMetadataRepository
import org.tales.domain.persistence.BookRepository
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.persistence.MediaRepository
import org.tales.domain.persistence.ReadProgressRepository
import org.tales.domain.persistence.SeriesCollectionRepository
import org.tales.domain.persistence.SeriesMetadataRepository
import org.tales.domain.persistence.SeriesRepository
import org.tales.domain.persistence.ThumbnailSeriesRepository
import org.tales.language.stripAccents
import java.io.File
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}
private val natSortComparator: Comparator<String> = CaseInsensitiveSimpleNaturalComparator.getInstance()

@Service
class SeriesLifecycle(
  private val libraryRepository: LibraryRepository,
  private val bookRepository: BookRepository,
  private val bookLifecycle: BookLifecycle,
  private val mediaRepository: MediaRepository,
  private val bookMetadataRepository: BookMetadataRepository,
  private val seriesRepository: SeriesRepository,
  private val thumbnailsSeriesRepository: ThumbnailSeriesRepository,
  private val seriesMetadataRepository: SeriesMetadataRepository,
  private val bookMetadataAggregationRepository: BookMetadataAggregationRepository,
  private val collectionRepository: SeriesCollectionRepository,
  private val readProgressRepository: ReadProgressRepository,
  private val taskEmitter: TaskEmitter,
  private val eventPublisher: ApplicationEventPublisher,
  private val transactionTemplate: TransactionTemplate,
) {
  private val whitespacePattern = """\s+""".toRegex()

  fun sortBooks(series: Series) {
    logger.info { "Sorting books: seriesId=${series.id}" }

    val books = bookRepository.findAllBySeriesId(series.id)
    val metadatas = bookMetadataRepository.findAllByIds(books.map { it.id })
    logger.debug { "Existing books for sort: seriesId=${series.id}, count=${books.size}" }
    logger.debug { "Existing metadata for sort: seriesId=${series.id}, count=${metadatas.size}" }

    val sorted =
      books
        .sortedWith(
          compareBy(natSortComparator) {
            it.name
              .trim()
              .stripAccents()
              .replace(whitespacePattern, " ")
          },
        ).map { book -> book to metadatas.first { it.bookId == book.id } }
    logger.debug { "Sorted books: seriesId=${series.id}, count=${sorted.size}" }

    bookRepository.update(
      sorted.mapIndexed { index, (book, _) -> book.copy(number = index + 1) },
    )

    val oldToNew =
      sorted.mapIndexedNotNull { index, (book, metadata) ->
        if (metadata.numberLock && metadata.numberSortLock)
          null
        else
          Triple(
            book,
            metadata,
            metadata.copy(
              number = if (!metadata.numberLock) (index + 1).toString() else metadata.number,
              numberSort = if (!metadata.numberSortLock) (index + 1).toFloat() else metadata.numberSort,
            ),
          )
      }
    bookMetadataRepository.update(oldToNew.map { it.third })

    // refresh metadata to reimport book number, else the series resorting would overwrite it
    oldToNew.forEach { (book, old, new) ->
      if (old.number != new.number || old.numberSort != new.numberSort) {
        logger.debug { "Metadata numbering has changed, refreshing metadata for book ${new.bookId} " }
        taskEmitter.refreshBookMetadata(book, setOf(BookMetadataPatchCapability.NUMBER, BookMetadataPatchCapability.NUMBER_SORT))
      }
    }

    // update book count for series
    seriesRepository.findByIdOrNull(series.id)?.let {
      seriesRepository.update(it.copy(bookCount = books.size), false)
    }
  }

  fun addBooks(
    series: Series,
    booksToAdd: Collection<Book>,
  ) {
    booksToAdd.forEach {
      check(it.libraryId == series.libraryId) { "Cannot add book to series if they don't share the same libraryId" }
    }
    val toAdd = booksToAdd.map { it.copy(seriesId = series.id) }

    transactionTemplate.executeWithoutResult {
      bookRepository.insert(toAdd)

      // create associated media
      mediaRepository.insert(toAdd.map { Media(bookId = it.id) })

      // create associated metadata
      toAdd
        .map {
          BookMetadata(
            title = it.name,
            number = it.number.toString(),
            numberSort = it.number.toFloat(),
            bookId = it.id,
          )
        }.let { bookMetadataRepository.insert(it) }
    }

    toAdd.forEach { eventPublisher.publishEvent(DomainEvent.BookAdded(it)) }
  }

  fun createSeries(series: Series): Series {
    transactionTemplate.executeWithoutResult {
      seriesRepository.insert(series)

      seriesMetadataRepository.insert(
        SeriesMetadata(
          title = series.name,
          titleSort = series.name,
          seriesId = series.id,
        ),
      )

      bookMetadataAggregationRepository.insert(
        BookMetadataAggregation(seriesId = series.id),
      )
    }

    eventPublisher.publishEvent(DomainEvent.SeriesAdded(series))

    return seriesRepository.findByIdOrNull(series.id)!!
  }

  fun softDeleteMany(series: Collection<Series>) {
    logger.info { "Soft delete series ids: ${series.map { it.id }}" }
    val deletedDate = LocalDateTime.now()

    transactionTemplate.executeWithoutResult {
      bookLifecycle.softDeleteMany(bookRepository.findAllBySeriesIds(series.map { it.id }))
      series.forEach {
        seriesRepository.update(it.copy(deletedDate = deletedDate))
      }
    }

    series.forEach { eventPublisher.publishEvent(DomainEvent.SeriesUpdated(it)) }
  }

  fun deleteMany(series: Collection<Series>) {
    val seriesIds = series.map { it.id }
    logger.info { "Delete series ids: $seriesIds" }

    transactionTemplate.executeWithoutResult {
      bookLifecycle.deleteMany(bookRepository.findAllBySeriesIds(seriesIds))

      readProgressRepository.deleteBySeriesIds(seriesIds)
      collectionRepository.removeSeriesFromAll(seriesIds)
      thumbnailsSeriesRepository.deleteBySeriesIds(seriesIds)
      seriesMetadataRepository.delete(seriesIds)
      bookMetadataAggregationRepository.delete(seriesIds)

      seriesRepository.delete(seriesIds)
    }

    series.forEach { eventPublisher.publishEvent(DomainEvent.SeriesDeleted(it)) }
  }

  fun markReadProgressCompleted(
    seriesId: String,
    user: TalesUser,
  ) {
    val bookIds =
      bookRepository
        .findAllIdsBySeriesId(seriesId)
        .filter { bookId ->
          val readProgress = readProgressRepository.findByBookIdAndUserIdOrNull(bookId, user.id)
          readProgress == null || !readProgress.completed
        }
    val progresses =
      mediaRepository
        .getPagesSizes(bookIds)
        .map { (bookId, pageSize) -> ReadProgress(bookId, user.id, pageSize, true) }

    readProgressRepository.save(progresses)
    progresses.forEach { eventPublisher.publishEvent(DomainEvent.ReadProgressChanged(it)) }
    eventPublisher.publishEvent(DomainEvent.ReadProgressSeriesChanged(seriesId, user.id))
  }

  fun deleteReadProgress(
    seriesId: String,
    user: TalesUser,
  ) {
    val bookIds = bookRepository.findAllIdsBySeriesId(seriesId)
    val progresses = readProgressRepository.findAllByBookIdsAndUserId(bookIds, user.id)
    readProgressRepository.deleteByBookIdsAndUserId(bookIds, user.id)

    progresses.forEach { eventPublisher.publishEvent(DomainEvent.ReadProgressDeleted(it)) }
    eventPublisher.publishEvent(DomainEvent.ReadProgressSeriesDeleted(seriesId, user.id))
  }

  fun getSelectedThumbnail(seriesId: String): ThumbnailSeries? {
    val selected = thumbnailsSeriesRepository.findSelectedBySeriesIdOrNull(seriesId)

    if (selected == null || (selected.type == ThumbnailSeries.Type.SIDECAR && !selected.exists())) {
      thumbnailsHouseKeeping(seriesId)
      return thumbnailsSeriesRepository.findSelectedBySeriesIdOrNull(seriesId)
    }

    return selected
  }

  private fun getBytesFromThumbnailSeries(thumbnail: ThumbnailSeries): ByteArray? =
    when {
      thumbnail.thumbnail != null -> thumbnail.thumbnail
      thumbnail.url != null -> File(thumbnail.url.toURI()).readBytes()
      else -> null
    }

  fun getThumbnailBytesByThumbnailId(thumbnailId: String): ByteArray? =
    thumbnailsSeriesRepository.findByIdOrNull(thumbnailId)?.let {
      getBytesFromThumbnailSeries(it)
    }

  fun getThumbnailBytes(
    seriesId: String,
    userId: String,
  ): ByteArray? {
    getSelectedThumbnail(seriesId)?.let {
      return getBytesFromThumbnailSeries(it)
    }

    seriesRepository.findByIdOrNull(seriesId)?.let { series ->
      val bookId =
        when (libraryRepository.findById(series.libraryId).seriesCover) {
          Library.SeriesCover.FIRST -> bookRepository.findFirstIdInSeriesOrNull(seriesId)
          Library.SeriesCover.FIRST_UNREAD_OR_FIRST ->
            bookRepository.findFirstUnreadIdInSeriesOrNull(seriesId, userId)
              ?: bookRepository.findFirstIdInSeriesOrNull(seriesId)

          Library.SeriesCover.FIRST_UNREAD_OR_LAST ->
            bookRepository.findFirstUnreadIdInSeriesOrNull(seriesId, userId)
              ?: bookRepository.findLastIdInSeriesOrNull(seriesId)

          Library.SeriesCover.LAST -> bookRepository.findLastIdInSeriesOrNull(seriesId)
        }
      if (bookId != null) return bookLifecycle.getThumbnailBytes(bookId)?.bytes
    }

    return null
  }

  fun addThumbnailForSeries(
    thumbnail: ThumbnailSeries,
    markSelected: MarkSelectedPreference,
  ): ThumbnailSeries {
    // delete existing thumbnail with the same url
    if (thumbnail.url != null) {
      thumbnailsSeriesRepository
        .findAllBySeriesId(thumbnail.seriesId)
        .filter { it.url == thumbnail.url }
        .forEach {
          thumbnailsSeriesRepository.delete(it.id)
        }
    }
    thumbnailsSeriesRepository.insert(thumbnail.copy(selected = false))

    val selected =
      when (markSelected) {
        MarkSelectedPreference.YES -> true
        MarkSelectedPreference.IF_NONE_OR_GENERATED -> {
          thumbnailsSeriesRepository.findSelectedBySeriesIdOrNull(thumbnail.seriesId) == null
        }

        MarkSelectedPreference.NO -> false
      }

    if (selected) thumbnailsSeriesRepository.markSelected(thumbnail)

    val newThumbnail = thumbnail.copy(selected = selected)
    eventPublisher.publishEvent(DomainEvent.ThumbnailSeriesAdded(newThumbnail))
    return newThumbnail
  }

  fun deleteThumbnailForSeries(thumbnail: ThumbnailSeries) {
    require(thumbnail.type == ThumbnailSeries.Type.USER_UPLOADED) { "Only uploaded thumbnails can be deleted" }
    thumbnailsSeriesRepository.delete(thumbnail.id)
    eventPublisher.publishEvent(DomainEvent.ThumbnailSeriesDeleted(thumbnail))
  }

  /**
   * Removes the series and its books from the library. Nothing on disk is
   * touched: the original media library is read-only (SV-1298), so this only
   * soft deletes them, which drops them out of every list the same way a series
   * whose folder went missing does. The files stay where they are.
   */
  fun removeSeriesFromLibrary(series: Series) {
    logger.info { "Remove series from library: seriesId=${series.id}" }
    bookRepository
      .findAllBySeriesId(series.id)
      .forEach { bookLifecycle.removeBookFromLibrary(it) }

    softDeleteMany(listOf(series))
  }

  private fun thumbnailsHouseKeeping(seriesId: String) {
    logger.info { "House keeping thumbnails for series: $seriesId" }
    val all =
      thumbnailsSeriesRepository
        .findAllBySeriesId(seriesId)
        .mapNotNull {
          if (!it.exists()) {
            logger.warn { "Thumbnail doesn't exist, removing entry" }
            thumbnailsSeriesRepository.delete(it.id)
            null
          } else {
            it
          }
        }

    val selected = all.filter { it.selected }
    when {
      selected.size > 1 -> {
        logger.info { "More than one thumbnail is selected, removing extra ones" }
        thumbnailsSeriesRepository.markSelected(selected[0])
      }
      selected.isEmpty() && all.isNotEmpty() -> {
        logger.info { "Series has no selected thumbnail, choosing one automatically" }
        thumbnailsSeriesRepository.markSelected(all.first())
      }
    }
  }
}
