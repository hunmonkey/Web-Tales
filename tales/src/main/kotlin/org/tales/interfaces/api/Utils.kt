package org.tales.interfaces.api

import org.springframework.http.ResponseEntity
import org.tales.domain.model.Media
import org.tales.infrastructure.web.setCachePrivate
import java.time.ZoneOffset

fun getBookLastModified(media: Media) = media.lastModifiedDate.toInstant(ZoneOffset.UTC).toEpochMilli()

fun ResponseEntity.BodyBuilder.setNotModified(media: Media): ResponseEntity.BodyBuilder = this.setCachePrivate().lastModified(getBookLastModified(media))
