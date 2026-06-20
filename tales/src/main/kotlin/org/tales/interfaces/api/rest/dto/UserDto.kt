package org.tales.interfaces.api.rest.dto

import com.fasterxml.jackson.annotation.JsonInclude
import org.tales.domain.model.AgeRestriction
import org.tales.domain.model.AllowExclude
import org.tales.domain.model.TalesUser
import org.tales.infrastructure.security.TalesPrincipal

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserDto(
  val id: String,
  val email: String,
  val roles: Set<String>,
  val sharedAllLibraries: Boolean,
  val sharedLibrariesIds: Set<String>,
  val labelsAllow: Set<String>,
  val labelsExclude: Set<String>,
  val ageRestriction: AgeRestrictionDto?,
)

data class AgeRestrictionDto(
  val age: Int,
  val restriction: AllowExclude,
)

fun AgeRestriction.toDto() = AgeRestrictionDto(age, restriction)

fun TalesUser.toDto() =
  UserDto(
    id = id,
    email = email,
    roles = roles.map { it.name }.toSet() + "USER",
    sharedAllLibraries = sharedAllLibraries,
    sharedLibrariesIds = sharedLibrariesIds,
    labelsAllow = restrictions.labelsAllow,
    labelsExclude = restrictions.labelsExclude,
    ageRestriction = restrictions.ageRestriction?.toDto(),
  )

fun TalesPrincipal.toDto() = user.toDto()
