package org.tales.interfaces.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.tales.domain.model.TalesUser
import org.tales.domain.model.UserRoles
import org.tales.domain.service.TalesUserLifecycle

private val logger = KotlinLogging.logger {}

@Profile("!test & noclaim")
@Component
class InitialUserController(
  private val userLifecycle: TalesUserLifecycle,
  private val initialUsers: List<TalesUser>,
) {
  @EventListener(ApplicationReadyEvent::class)
  fun createInitialUserOnStartupIfNoneExist() {
    if (userLifecycle.countUsers() == 0L) {
      logger.info { "No users exist in database, creating initial users" }

      initialUsers
        .forEach {
          userLifecycle.createUser(it)
          logger.info { "Initial user created" }
        }
    }
  }
}

@Configuration
@Profile("dev")
class InitialUsersDevConfiguration {
  @Bean
  fun initialUsers(): List<TalesUser> =
    listOf(
      TalesUser("admin@example.org", "admin", roles = UserRoles.entries.toSet()),
      TalesUser("user@example.org", "user"),
    )
}

@Configuration
@Profile("!dev")
class InitialUsersProdConfiguration {
  @Bean
  fun initialUsers(): List<TalesUser> =
    listOf(
      TalesUser("admin@example.org", RandomStringUtils.secure().nextAlphanumeric(12), roles = UserRoles.entries.toSet()),
    )
}
