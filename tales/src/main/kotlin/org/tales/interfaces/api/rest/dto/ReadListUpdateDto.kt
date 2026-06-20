package org.tales.interfaces.api.rest.dto

import org.tales.infrastructure.validation.NullOrNotBlank
import org.tales.infrastructure.validation.NullOrNotEmpty
import org.hibernate.validator.constraints.UniqueElements

data class ReadListUpdateDto(
  @get:NullOrNotBlank val name: String?,
  val summary: String?,
  @get:NullOrNotEmpty @get:UniqueElements
  val bookIds: List<String>?,
  val ordered: Boolean?,
)
