package org.tales.interfaces.api.rest.dto

import org.tales.domain.model.AlternateTitle

data class AlternateTitleDto(
  val label: String,
  val title: String,
)

fun AlternateTitle.toDto() = AlternateTitleDto(label, title)
