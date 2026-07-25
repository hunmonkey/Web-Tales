package org.tales.interfaces.apprunner

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.tales.domain.persistence.TalesUserRepository

private val logger = KotlinLogging.logger {}

@Profile("!test")
@Component
class ListUsersRunner(
  private val userRepository: TalesUserRepository,
) : ApplicationRunner {
  override fun run(args: ApplicationArguments) {
    if (args.getOptionValues("list-users") != null) {
      val emails = userRepository.findAll().map { it.email }
      if (emails.isNotEmpty())
        logger.info { "Here is a list of all users: $emails" }
      else
        logger.info { "No users exist yet" }
    }
  }
}
