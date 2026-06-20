package org.tales.infrastructure.security.oauth2

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.tales.domain.model.TalesUser
import org.tales.domain.persistence.TalesUserRepository
import org.tales.domain.service.TalesUserLifecycle
import org.tales.infrastructure.configuration.TalesProperties
import org.tales.infrastructure.security.TalesPrincipal
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

private val logger = KotlinLogging.logger {}

@Configuration
class TalesOAuth2UserServiceConfiguration(
  private val userRepository: TalesUserRepository,
  private val userLifecycle: TalesUserLifecycle,
  private val talesProperties: TalesProperties,
) {
  @Bean
  fun oauth2UserService(): OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    val defaultDelegate = DefaultOAuth2UserService()
    val githubDelegate = GithubOAuth2UserService()

    return OAuth2UserService { userRequest: OAuth2UserRequest ->
      val delegate =
        when (userRequest.clientRegistration.registrationId.lowercase()) {
          "github" -> githubDelegate
          else -> defaultDelegate
        }

      val oAuth2User = delegate.loadUser(userRequest)

      val email =
        oAuth2User.getAttribute<String>("email")
          ?: throw OAuth2AuthenticationException("ERR_1024")

      val existingUser =
        userRepository.findByEmailIgnoreCaseOrNull(email)
          ?: tryCreateNewUser(email)

      TalesPrincipal(existingUser, oAuth2User = oAuth2User)
    }
  }

  @Bean
  fun oidcUserService(): OAuth2UserService<OidcUserRequest, OidcUser> {
    val delegate = OidcUserService()
    return OAuth2UserService { userRequest: OidcUserRequest ->
      val oidcUser = delegate.loadUser(userRequest)

      if (oidcUser.email == null) throw OAuth2AuthenticationException("ERR_1028")
      if (talesProperties.oidcEmailVerification && oidcUser.emailVerified == null) throw OAuth2AuthenticationException("ERR_1027")
      if (talesProperties.oidcEmailVerification && oidcUser.emailVerified == false) throw OAuth2AuthenticationException("ERR_1026")

      val existingUser =
        userRepository.findByEmailIgnoreCaseOrNull(oidcUser.email)
          ?: tryCreateNewUser(oidcUser.email)

      TalesPrincipal(existingUser, oidcUser)
    }
  }

  private fun tryCreateNewUser(email: String) =
    if (talesProperties.oauth2AccountCreation) {
      logger.info { "Creating new user from OAuth2 login: $email" }
      userLifecycle.createUser(TalesUser(email, RandomStringUtils.secure().nextAlphanumeric(12)))
    } else {
      throw OAuth2AuthenticationException("ERR_1025")
    }
}
