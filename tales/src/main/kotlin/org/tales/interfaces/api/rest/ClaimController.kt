package org.tales.interfaces.api.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.tales.domain.model.TalesUser
import org.tales.domain.model.UserRoles
import org.tales.domain.service.TalesUserLifecycle
import org.tales.infrastructure.openapi.OpenApiConfiguration
import org.tales.interfaces.api.rest.dto.UserDto
import org.tales.interfaces.api.rest.dto.toDto
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("api/v1/claim", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = OpenApiConfiguration.TagNames.CLAIM)
@Validated
@SecurityRequirements
class ClaimController(
  private val userDetailsLifecycle: TalesUserLifecycle,
) {
  @GetMapping
  @Operation(summary = "Retrieve claim status", description = "Check whether this server has already been claimed.")
  fun getClaimStatus() = ClaimStatus(userDetailsLifecycle.countUsers() > 0)

  @PostMapping
  @Operation(summary = "Claim server", description = "Creates an admin user with the provided credentials.")
  fun claimServer(
    @Email(regexp = ".+@.+\\..+")
    @RequestHeader("X-Tales-Email")
    email: String,
    @NotBlank
    @RequestHeader("X-Tales-Password")
    password: String,
  ): UserDto {
    if (userDetailsLifecycle.countUsers() > 0)
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "This server has already been claimed")

    return userDetailsLifecycle
      .createUser(
        TalesUser(
          email = email,
          password = password,
          roles = UserRoles.entries.toSet(),
        ),
      ).toDto()
  }

  data class ClaimStatus(
    val isClaimed: Boolean,
  )
}
