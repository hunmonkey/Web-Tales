package org.tales.domain.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.tales.domain.model.ApiKey
import org.tales.domain.model.DomainEvent
import org.tales.domain.model.DuplicateNameException
import org.tales.domain.model.TalesUser
import org.tales.domain.model.UserEmailAlreadyExistsException
import org.tales.domain.persistence.AuthenticationActivityRepository
import org.tales.domain.persistence.ReadProgressRepository
import org.tales.domain.persistence.TalesUserRepository
import org.tales.infrastructure.jooq.main.ClientSettingsDtoDao
import org.tales.infrastructure.security.TalesPrincipal
import org.tales.infrastructure.security.TokenEncoder
import org.tales.infrastructure.security.apikey.ApiKeyGenerator

private val logger = KotlinLogging.logger {}

@Service
class TalesUserLifecycle(
  private val userRepository: TalesUserRepository,
  private val readProgressRepository: ReadProgressRepository,
  private val authenticationActivityRepository: AuthenticationActivityRepository,
  private val passwordEncoder: PasswordEncoder,
  private val tokenEncoder: TokenEncoder,
  private val sessionRegistry: SessionRegistry,
  private val transactionTemplate: TransactionTemplate,
  private val eventPublisher: ApplicationEventPublisher,
  private val apiKeyGenerator: ApiKeyGenerator,
  private val clientSettingsDtoDao: ClientSettingsDtoDao,
) {
  fun updatePassword(
    user: TalesUser,
    newPassword: String,
    expireSessions: Boolean,
  ) {
    logger.info { "Changing password for user ${user.email}" }
    val updatedUser = user.copy(password = passwordEncoder.encode(newPassword))
    userRepository.update(updatedUser)

    if (expireSessions) expireSessions(updatedUser)

    eventPublisher.publishEvent(DomainEvent.UserUpdated(updatedUser, expireSessions))
  }

  fun updateUser(user: TalesUser) {
    val existing = userRepository.findByIdOrNull(user.id)
    requireNotNull(existing) { "User doesn't exist, cannot update: $user" }

    val toUpdate = user.copy(password = existing.password)
    logger.info { "Update user: $toUpdate" }
    userRepository.update(toUpdate)

    val expireSessions =
      existing.roles != user.roles ||
        existing.restrictions != user.restrictions ||
        existing.sharedAllLibraries != user.sharedAllLibraries ||
        existing.sharedLibrariesIds != user.sharedLibrariesIds

    if (expireSessions) expireSessions(toUpdate)

    eventPublisher.publishEvent(DomainEvent.UserUpdated(toUpdate, expireSessions))
  }

  fun countUsers() = userRepository.count()

  @Throws(UserEmailAlreadyExistsException::class)
  fun createUser(talesUser: TalesUser): TalesUser {
    if (userRepository.existsByEmailIgnoreCase(talesUser.email)) throw UserEmailAlreadyExistsException("A user with the same email already exists: ${talesUser.email}")

    userRepository.insert(talesUser.copy(password = passwordEncoder.encode(talesUser.password)))

    val createdUser = userRepository.findByIdOrNull(talesUser.id)!!
    logger.info { "User created: $createdUser" }
    return createdUser
  }

  fun deleteUser(user: TalesUser) {
    logger.info { "Deleting user: $user" }

    transactionTemplate.executeWithoutResult {
      clientSettingsDtoDao.deleteByUserId(user.id)
      readProgressRepository.deleteByUserId(user.id)
      authenticationActivityRepository.deleteByUser(user)
      userRepository.delete(user.id)
    }

    expireSessions(user)

    eventPublisher.publishEvent(DomainEvent.UserUpdated(user, true))
  }

  fun expireSessions(user: TalesUser) {
    logger.info { "Expiring all sessions for user: ${user.email}" }
    sessionRegistry
      .getAllSessions(TalesPrincipal(user), false)
      .forEach {
        logger.info { "Expiring session: ${it.sessionId}" }
        it.expireNow()
      }
  }

  /**
   * Create and persist an API key for the user.
   * @return the ApiKey, or null if a unique API key could not be generated
   */
  fun createApiKey(
    user: TalesUser,
    comment: String,
  ): ApiKey? {
    val commentTrimmed = comment.trim()
    if (userRepository.existsApiKeyByCommentAndUserId(commentTrimmed, user.id))
      throw DuplicateNameException("api key comment already exists for this user", "ERR_1034")
    for (attempt in 1..10) {
      try {
        val plainTextKey =
          ApiKey(
            userId = user.id,
            key = apiKeyGenerator.generate(),
            comment = commentTrimmed,
          )
        userRepository.insert(plainTextKey.copy(key = tokenEncoder.encode(plainTextKey.key)))
        return plainTextKey
      } catch (e: Exception) {
        logger.debug { "Failed to generate unique api key, attempt #$attempt" }
      }
    }
    return null
  }
}
