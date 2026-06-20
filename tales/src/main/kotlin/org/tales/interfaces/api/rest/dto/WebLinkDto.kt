package org.tales.interfaces.api.rest.dto

import org.tales.domain.model.WebLink

data class WebLinkDto(
  val label: String,
  val url: String,
)

fun WebLink.toDto() = WebLinkDto(label, url.toString())
