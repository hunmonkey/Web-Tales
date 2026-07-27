package org.tales.infrastructure.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.pathString
import kotlin.io.path.toPath

fun URL.toFilePath(): String = this.toURI().toPath().pathString

fun filePathToUrl(filePath: String): URL = Paths.get(filePath).toUri().toURL()

fun ResponseEntity.BodyBuilder.setCachePrivate() = this.cacheControl(cachePrivate)

val cachePrivate =
  CacheControl
    .maxAge(0, TimeUnit.SECONDS)
    .cachePrivate()
    .mustRevalidate()

/**
 * For binary image content: posters and page images.
 *
 * Both only change when their source changes, and both give the client a new URL when that
 * happens: the web UI cache-busts a poster URL from the thumbnail SSE events, and a replaced book
 * file is re-analyzed. Revalidating them on every render costs one round trip per image and
 * returns 304 nearly every time, which is what makes browsing feel slow on a remote connection.
 *
 * Stays `private` so only the browser stores it, never a shared proxy or CDN, because the content
 * is behind authentication.
 */
val cacheImageContent =
  CacheControl
    .maxAge(1, TimeUnit.DAYS)
    .cachePrivate()

fun getMediaTypeOrDefault(mediaTypeString: String?): MediaType {
  mediaTypeString?.let {
    try {
      return MediaType.parseMediaType(mediaTypeString)
    } catch (_: Exception) {
    }
  }
  return MediaType.APPLICATION_OCTET_STREAM
}

fun getCurrentRequest(): HttpServletRequest = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?)?.request ?: throw IllegalStateException("Could not get current request")
