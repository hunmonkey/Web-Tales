package org.tales.interfaces.apprunner

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.tales.domain.persistence.TalesUserRepository
import org.tales.domain.service.TalesUserLifecycle

private val logger = KotlinLogging.logger {}

@Profile("!test")
@Component
class PasswordResetRunner(
  private val userRepository: TalesUserRepository,
  private val userLifecycle: TalesUserLifecycle,
) : ApplicationRunner {
  private val resetFor = "reset"
  private val resetTo = "newpassword"

  override fun run(args: ApplicationArguments) {
    val newPassword = args.getOptionValues(resetTo)?.firstOrNull()
    val resetFor = args.getOptionValues(resetFor)?.toSet() ?: emptySet()

    if (resetFor.isEmpty() xor (newPassword == null))
      return logger.warn { "You need to specify both '--${this.resetFor}=user@domain.com' and '--$resetTo=YourNewPassword'" }

    if (resetFor.isEmpty()) return

    if (newPassword.isNullOrBlank())
      return logger.warn { "The new password must not be blank" }

    resetFor
      .forEach { arg ->
        userRepository.findByEmailIgnoreCaseOrNull(arg)?.let { user ->
          logger.info { "Reset password for user: ${user.email}" }
          userLifecycle.updatePassword(user, newPassword, true)
        } ?: logger.warn { "User does not exist: $arg" }
      }
  }
}
