package org.tales.infrastructure.kobo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import org.tales.domain.model.TalesSyncToken
import org.tales.infrastructure.kobo.KoboHeaders.X_KOBO_SYNCTOKEN
import java.util.Base64

private val logger = KotlinLogging.logger {}

private const val TALES_TOKEN_PREFIX = "TALES."

@Component
class TalesSyncTokenGenerator(
  private val objectMapper: ObjectMapper,
) {
  private val base64Encoder by lazy { Base64.getEncoder().withoutPadding() }
  private val base64Decoder by lazy { Base64.getDecoder() }

  /**
   * Convert any SyncToken to a [TalesSyncToken].
   * The input SyncToken type depends on the String format:
   * - the official Kobo store token is of the form `base64.base64`
   * - the Calibre Web token is a single base64 string
   * - the Tales token is a base64 string prefixed by `TALES.`
   */
  fun fromBase64(base64Token: String): TalesSyncToken {
    try {
      // check for a Tales token
      if (base64Token.startsWith(TALES_TOKEN_PREFIX)) {
        return objectMapper.readValue(base64Decoder.decode(base64Token.removePrefix(TALES_TOKEN_PREFIX)))
      }

      // check for a Calibre Web token
      if (!base64Token.contains('.')) {
        try {
          val json = objectMapper.readTree(base64Decoder.decode(base64Token))
          val koboToken = json.get("data").get("raw_kobo_store_token").asText()
          return TalesSyncToken(rawKoboSyncToken = koboToken)
        } catch (e: Exception) {
          logger.warn { "Failed to parse potential CalibreWeb token" }
        }
      }

      // check for a Kobo store token
      if (base64Token.contains('.')) {
        return TalesSyncToken(rawKoboSyncToken = base64Token)
      }
    } catch (_: Exception) {
    }

    // in last resort return a default token
    return TalesSyncToken()
  }

  fun toBase64(token: TalesSyncToken): String = TALES_TOKEN_PREFIX + base64Encoder.encodeToString(objectMapper.writeValueAsString(token).toByteArray())

  fun fromRequestHeaders(request: HttpServletRequest): TalesSyncToken? {
    val syncTokenB64 = request.getHeader(X_KOBO_SYNCTOKEN)
    return if (syncTokenB64 != null) fromBase64(syncTokenB64) else null
  }
}
