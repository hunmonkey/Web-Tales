package org.tales.interfaces.api.rest.dto

import org.tales.domain.model.R2Locator

data class R2Positions(
  val total: Int,
  val positions: List<R2Locator>,
)
