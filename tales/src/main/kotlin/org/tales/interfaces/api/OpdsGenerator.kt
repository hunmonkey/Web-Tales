package org.tales.interfaces.api

import org.tales.domain.persistence.MediaRepository
import org.tales.domain.service.BookAnalyzer
import org.tales.infrastructure.image.ImageConverter
import org.tales.infrastructure.image.ImageType
import org.tales.interfaces.api.dto.MEDIATYPE_OPDS_AUTHENTICATION_JSON_VALUE
import org.tales.interfaces.api.dto.MEDIATYPE_OPDS_JSON_VALUE
import org.tales.interfaces.api.dto.MEDIATYPE_OPDS_PUBLICATION_JSON
import org.tales.interfaces.api.dto.MEDIATYPE_PROGRESSION_JSON_VALUE
import org.tales.interfaces.api.dto.REL_PROGRESSION_API
import org.tales.interfaces.api.dto.WPLinkDto
import org.tales.interfaces.api.dto.WPPublicationDto
import org.tales.interfaces.api.opds.v2.ROUTE_AUTH
import org.tales.interfaces.api.opds.v2.dto.AuthenticationDocumentDto
import org.tales.interfaces.api.opds.v2.dto.AuthenticationFlowDto
import org.tales.interfaces.api.opds.v2.dto.AuthenticationType
import org.tales.interfaces.api.opds.v2.dto.LabelsDto
import org.tales.interfaces.api.rest.dto.BookDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Component
class OpdsGenerator(
  @Qualifier("thumbnailType") thumbnailType: ImageType,
  imageConverter: ImageConverter,
  bookAnalyzer: BookAnalyzer,
  mediaRepository: MediaRepository,
) : WebPubGenerator(thumbnailType, imageConverter, bookAnalyzer, mediaRepository) {
  override val pathSegments = listOf("opds", "v2")

  fun toOpdsPublicationDto(bookDto: BookDto): WPPublicationDto = toBasePublicationDto(bookDto).copy(images = buildThumbnailLinkDtos(bookDto.id))

  override fun getDefaultMediaType(): MediaType = MEDIATYPE_OPDS_PUBLICATION_JSON

  override fun getBookSeriesLink(bookDto: BookDto): List<WPLinkDto> =
    listOf(
      WPLinkDto(
        href =
          ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .pathSegment(*pathSegments.toTypedArray())
            .path("series/${bookDto.seriesId}")
            .toUriString(),
        type = MEDIATYPE_OPDS_JSON_VALUE,
      ),
    )

  override fun getExtraLinkProperties(): Map<String, Map<String, Any>> =
    mapOf(
      "authenticate" to
        mapOf(
          "href" to
            ServletUriComponentsBuilder
              .fromCurrentContextPath()
              .pathSegment(*pathSegments.toTypedArray())
              .path(ROUTE_AUTH)
              .toUriString(),
          "type" to MEDIATYPE_OPDS_AUTHENTICATION_JSON_VALUE,
        ),
    )

  override fun getExtraLinks(bookId: String): List<WPLinkDto> =
    buildList {
      add(
        WPLinkDto(
          type = MEDIATYPE_PROGRESSION_JSON_VALUE,
          rel = REL_PROGRESSION_API,
          href =
            ServletUriComponentsBuilder
              .fromCurrentContextPath()
              .pathSegment(*pathSegments.toTypedArray())
              .path("books/$bookId/progression")
              .toUriString(),
          properties = getExtraLinkProperties(),
        ),
      )
    }

  fun generateOpdsAuthDocument() =
    AuthenticationDocumentDto(
      id =
        ServletUriComponentsBuilder
          .fromCurrentContextPath()
          .pathSegment(*pathSegments.toTypedArray())
          .path(ROUTE_AUTH)
          .toUriString(),
      title = "Tales",
      description = "Enter your email and password to authenticate.",
      links =
        listOf(
          WPLinkDto(rel = "help", href = "https://github.com/hunmonkey/Web-Tales"),
          WPLinkDto(rel = "logo", href = ServletUriComponentsBuilder.fromCurrentContextPath().pathSegment("tales-icon.svg").toUriString()),
        ),
      authentication =
        listOf(
          AuthenticationFlowDto(
            type = AuthenticationType.BASIC,
            labels = LabelsDto(login = "Email", password = "Password"),
          ),
        ),
    )
}
