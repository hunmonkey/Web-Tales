package org.tales

import org.tales.application.gui.showErrorDialog
import org.tales.infrastructure.util.checkTempDirectory
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.server.PortInUseException
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
class DesktopApplication : Application()

fun main(args: Array<String>) {
  checkTempDirectory()

  System.setProperty("apple.awt.UIElement", "true")
  System.setProperty("org.jooq.no-logo", "true")
  System.setProperty("org.jooq.no-tips", "true")

  try {
    SpringApplicationBuilder(DesktopApplication::class.java).apply {
      headless(false)
      run(*args)
    }
  } catch (e: Exception) {
    val (message, stackTrace) =
      when (e.cause) {
        is PortInUseException -> RB.getString("error_message.port_in_use", (e.cause as PortInUseException).port) to null
        else -> RB.getString("error_message.unexpected") to e.stackTraceToString()
      }

    showErrorDialog(message, stackTrace)
  }
}
