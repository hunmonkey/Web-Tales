package org.tales.shelfview.interfaces.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.tales.application.tasks.TaskEmitter
import org.tales.domain.model.makeLibrary
import org.tales.domain.persistence.LibraryRepository
import org.tales.domain.service.LibraryLifecycle
import org.tales.interfaces.api.rest.WithMockCustomUser
import org.tales.shelfview.domain.persistence.ShelfViewRemovedEntryRepository

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ShelfViewRemovedEntryControllerTest(
  @Autowired private val libraryRepository: LibraryRepository,
  @Autowired private val libraryLifecycle: LibraryLifecycle,
  @Autowired private val removedEntryRepository: ShelfViewRemovedEntryRepository,
  @Autowired private val mockMvc: MockMvc,
) {
  @MockkBean
  private lateinit var mockTaskEmitter: TaskEmitter

  private val library = makeLibrary(name = "Removed", id = "shelfview-removed-library")

  @BeforeAll
  fun `setup library`() {
    libraryRepository.insert(library)
  }

  @AfterAll
  fun teardown() {
    libraryRepository.findAll().forEach { libraryLifecycle.deleteLibrary(it) }
  }

  @Test
  @WithMockCustomUser(roles = ["ADMIN"])
  fun `given removed entries when an admin restores them then the records are gone and a scan is queued`() {
    every { mockTaskEmitter.scanLibrary(any(), any(), any()) } just Runs
    removedEntryRepository.add(library.id, "file:/removed/book1.cbz")
    removedEntryRepository.add(library.id, "file:/removed/series")

    mockMvc
      .delete("/api/shelfview/v1/libraries/${library.id}/removed-entries")
      .andExpect { status { isNoContent() } }

    assertThat(removedEntryRepository.findUrls(library.id)).isEmpty()
    verify(exactly = 1) { mockTaskEmitter.scanLibrary(library.id, any(), any()) }
  }

  @Test
  @WithMockCustomUser(roles = ["ADMIN"])
  fun `given an unknown library when restoring then 404`() {
    mockMvc
      .delete("/api/shelfview/v1/libraries/no-such-library/removed-entries")
      .andExpect { status { isNotFound() } }
  }

  @Test
  @WithMockCustomUser
  fun `given a non-admin user when restoring then it is denied`() {
    removedEntryRepository.add(library.id, "file:/removed/kept.cbz")

    mockMvc
      .delete("/api/shelfview/v1/libraries/${library.id}/removed-entries")
      .andExpect { status { isForbidden() } }

    assertThat(removedEntryRepository.findUrls(library.id)).containsExactly("file:/removed/kept.cbz")
  }
}
