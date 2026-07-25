package org.tales.interfaces.api.rest.dto

import org.hibernate.validator.constraints.UniqueElements
import org.tales.infrastructure.validation.NullOrNotBlank
import org.tales.infrastructure.validation.NullOrNotEmpty

data class CollectionUpdateDto(
  @get:NullOrNotBlank val name: String?,
  val ordered: Boolean?,
  @get:NullOrNotEmpty @get:UniqueElements
  val seriesIds: List<String>?,
)
