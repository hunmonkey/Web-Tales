package org.tales.shelfview.interfaces.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.tales.application.tasks.TaskEmitter
import org.tales.domain.persistence.LibraryRepository
import org.tales.shelfview.domain.persistence.ShelfViewRemovedEntryRepository

/**
 * Undo for "remove from library" (ADR-0023): forgets every removal recorded
 * for the library and queues a scan, which re-adds the entries.
 */
@RestController
@RequestMapping("api/shelfview/v1/libraries/{libraryId}/removed-entries")
@Tag(name = "ShelfView")
class ShelfViewRemovedEntryController(
  private val removedEntryRepository: ShelfViewRemovedEntryRepository,
  private val libraryRepository: LibraryRepository,
  private val taskEmitter: TaskEmitter,
) {
  @Operation(summary = "Restore every book and series removed from this library, then rescan it")
  @DeleteMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun restoreAll(
    @PathVariable libraryId: String,
  ) {
    libraryRepository.findByIdOrNull(libraryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    removedEntryRepository.deleteAll(libraryId)
    taskEmitter.scanLibrary(libraryId)
  }
}
