package org.tales.infrastructure.web

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.stereotype.Component
import org.tales.infrastructure.configuration.TalesSettingsProvider

private val logger = KotlinLogging.logger {}

@Component
class WebServerConfiguration(
  private val settingsProvider: TalesSettingsProvider,
) : WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
  override fun customize(factory: ConfigurableServletWebServerFactory) {
    settingsProvider.serverPort?.let {
      if (it > 1)
        factory.setPort(it)
      else
        logger.warn { "Ignoring invalid server port: $it" }
    }
    settingsProvider.serverContextPath?.let {
      if (it.startsWith("/") && !it.endsWith("/"))
        factory.setContextPath(it)
      else
        logger.warn { "Ignoring invalid server context path" }
    }
  }
}
