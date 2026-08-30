package org.tales.shelfview.interfaces.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** Lock state of one PIN-protected library. Never carries the PIN or its hash. */
data class ShelfViewLibraryLockDto(
  val libraryId: String,
  val name: String,
  val unlocked: Boolean,
)

data class ShelfViewLibraryPinDto(
  @get:NotBlank
  @get:Size(min = 4, max = 64)
  val pin: String,
)
