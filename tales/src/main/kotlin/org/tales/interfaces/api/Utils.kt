package org.tales.interfaces.api

import org.springframework.http.ResponseEntity
import org.tales.domain.model.Media
import org.tales.infrastructure.web.setCachePrivate
import java.time.ZoneOffset

fun getBookLastModified(media: Media) = media.lastModifiedDate.toInstant(ZoneOffset.UTC).toEpochMilli()

fun ResponseEntity.BodyBuilder.setNotModified(media: Media): ResponseEntity.BodyBuilder = this.setCachePrivate().lastModified(getBookLastModified(media))

/**
 * Sets the same validator as [setNotModified] but no `Cache-Control`, leaving that to the image
 * cache mapping in `WebMvcConfiguration`.
 *
 * A `Cache-Control` set here would not replace the one the interceptor already wrote: response
 * entity headers are added to the servlet response, not set on it, so the two would both be sent
 * and the interceptor's value would be the one that counts.
 *
 * Only for page images - not for manifests, positions or EPUB resources, which the reader has to
 * see the current version of.
 */
fun ResponseEntity.BodyBuilder.setImageLastModified(media: Media): ResponseEntity.BodyBuilder = this.lastModified(getBookLastModified(media))
