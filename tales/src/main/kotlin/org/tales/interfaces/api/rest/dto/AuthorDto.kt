package org.tales.interfaces.api.rest.dto

import org.tales.domain.model.Author

data class AuthorDto(
  val name: String,
  val role: String,
)

fun Author.toDto() = AuthorDto(name, role)
