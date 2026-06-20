package org.tales.application.gui

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import org.tales.RB
import org.tales.infrastructure.web.WebServerEffectiveSettings
import org.tales.openExplorer
import org.tales.openUrl
import org.jetbrains.compose.resources.decodeToSvgPainter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.io.File

@Profile("!test")
@Component
class TrayIconRunner(
  @param:Value("#{talesProperties.configDir}") talesConfigDir: String,
  @param:Value($$"${logging.file.name}") logFileName: String,
  serverSettings: WebServerEffectiveSettings,
  env: Environment,
) : ApplicationRunner {
  val talesUrl by lazy { "http://localhost:${serverSettings.effectiveServerPort}${serverSettings.effectiveServletContextPath}" }
  val talesConfigDir by lazy { File(talesConfigDir) }
  val logFile by lazy { File(logFileName) }
  val iconFileName = if (env.activeProfiles.contains("mac")) "tales-gray-minimal.svg" else "tales-color.svg"

  @Async
  override fun run(args: ApplicationArguments) {
    runTray()
  }

  private fun runTray() {
    application {
      Tray(
        icon = ClassPathResource("icons/$iconFileName").inputStream.readAllBytes().decodeToSvgPainter(LocalDensity.current),
        menu = {
          Item(RB.getString("menu.open_tales"), onClick = { openUrl(talesUrl) })
          Item(RB.getString("menu.show_log"), onClick = { openExplorer(logFile) })
          Item(RB.getString("menu.show_conf_dir"), onClick = { openExplorer(talesConfigDir) })
          Item(RB.getString("menu.quit"), onClick = ::exitApplication)
        },
      )
    }
  }
}
