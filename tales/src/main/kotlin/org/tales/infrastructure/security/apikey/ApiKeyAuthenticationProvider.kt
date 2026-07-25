package org.tales.infrastructure.security.apikey

import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.tales.domain.persistence.TalesUserRepository
import org.tales.infrastructure.security.TalesPrincipal

/**
 * A provider to lookup API keys in the repository.
 */
@Component
class ApiKeyAuthenticationProvider(
  private val userRepository: TalesUserRepository,
) : AbstractUserDetailsAuthenticationProvider() {
  override fun additionalAuthenticationChecks(
    userDetails: UserDetails?,
    authentication: UsernamePasswordAuthenticationToken?,
  ) = Unit

  override fun retrieveUser(
    username: String,
    authentication: UsernamePasswordAuthenticationToken,
  ): UserDetails =
    userRepository.findByApiKeyOrNull(authentication.credentials.toString())?.let { (user, apiKey) ->
      TalesPrincipal(user, apiKey = apiKey, name = authentication.name)
    } ?: throw BadCredentialsException("Bad credentials")

  override fun createSuccessAuthentication(
    principal: Any?,
    authentication: Authentication?,
    user: UserDetails?,
  ): Authentication =
    ApiKeyAuthenticationToken
      .authenticated(principal, authentication?.credentials, user!!.authorities)
      .apply { details = authentication?.details }
      .also { logger.debug("Authenticated user") }

  override fun supports(authentication: Class<*>): Boolean = ApiKeyAuthenticationToken::class.java.isAssignableFrom(authentication)
}
