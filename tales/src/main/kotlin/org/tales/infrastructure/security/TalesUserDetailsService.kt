package org.tales.infrastructure.security

import org.tales.domain.persistence.TalesUserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component

@Component
class TalesUserDetailsService(
  private val userRepository: TalesUserRepository,
) : UserDetailsService {
  override fun loadUserByUsername(username: String): UserDetails =
    userRepository.findByEmailIgnoreCaseOrNull(username)?.let {
      TalesPrincipal(it)
    } ?: throw UsernameNotFoundException(username)
}
