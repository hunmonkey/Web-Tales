package org.tales

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.tales.infrastructure.util.checkTempDirectory

@SpringBootApplication
@EnableScheduling
class Application

fun main(args: Array<String>) {
  checkTempDirectory()

  System.setProperty("org.jooq.no-logo", "true")
  System.setProperty("org.jooq.no-tips", "true")

  runApplication<Application>(*args)
}
