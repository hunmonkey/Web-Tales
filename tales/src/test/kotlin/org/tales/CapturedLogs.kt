package org.tales

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory

data class CapturedLogs<T>(
  val result: T,
  val rendered: String,
)

fun <T> captureLogs(
  loggerClass: Class<*>,
  block: () -> T,
): CapturedLogs<T> {
  val logger = LoggerFactory.getLogger(loggerClass) as Logger
  val appender =
    ListAppender<ILoggingEvent>().apply {
      context = logger.loggerContext
      start()
    }
  val layout =
    PatternLayout().apply {
      context = logger.loggerContext
      pattern = "%msg%n%ex"
      start()
    }

  logger.addAppender(appender)
  try {
    val result = block()
    return CapturedLogs(result, appender.list.joinToString(separator = "") { layout.doLayout(it) })
  } finally {
    logger.detachAppender(appender)
    appender.stop()
    layout.stop()
  }
}
