package org.tales.interfaces.api.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.tales.infrastructure.configuration.TalesSettingsProvider
import org.tales.infrastructure.openapi.OpenApiConfiguration
import org.tales.infrastructure.web.WebServerEffectiveSettings
import org.tales.interfaces.api.rest.dto.SettingMultiSource
import org.tales.interfaces.api.rest.dto.SettingsDto
import org.tales.interfaces.api.rest.dto.SettingsUpdateDto
import org.tales.interfaces.api.rest.dto.toDomain
import org.tales.interfaces.api.rest.dto.toDto
import kotlin.time.Duration.Companion.days

@RestController
@RequestMapping(value = ["api/v1/settings"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = OpenApiConfiguration.TagNames.SERVER_SETTINGS)
class SettingsController(
  private val talesSettingsProvider: TalesSettingsProvider,
  @param:Value($$"${server.port:#{null}}") private val configServerPort: Int?,
  @param:Value($$"${server.servlet.context-path:#{null}}") private val configServerContextPath: String?,
  private val serverSettings: WebServerEffectiveSettings,
) {
  @GetMapping
  @Operation(summary = "Retrieve server settings")
  fun getServerSettings(): SettingsDto =
    SettingsDto(
      talesSettingsProvider.deleteEmptyCollections,
      talesSettingsProvider.deleteEmptyReadLists,
      talesSettingsProvider.rememberMeDuration.inWholeDays,
      talesSettingsProvider.thumbnailSize.toDto(),
      talesSettingsProvider.taskPoolSize,
      SettingMultiSource(configServerPort, talesSettingsProvider.serverPort, serverSettings.effectiveServerPort),
      SettingMultiSource(configServerContextPath, talesSettingsProvider.serverContextPath, serverSettings.effectiveServletContextPath),
    )

  @PatchMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Update server settings", description = "You can omit fields you don't want to update")
  fun updateServerSettings(
    @Valid @RequestBody
    @Parameter(description = "Fields to update. You can omit fields you don't want to update.")
    newSettings: SettingsUpdateDto,
  ) {
    newSettings.deleteEmptyCollections?.let { talesSettingsProvider.deleteEmptyCollections = it }
    newSettings.deleteEmptyReadLists?.let { talesSettingsProvider.deleteEmptyReadLists = it }
    newSettings.rememberMeDurationDays?.let { talesSettingsProvider.rememberMeDuration = it.days }
    if (newSettings.renewRememberMeKey == true) talesSettingsProvider.renewRememberMeKey()
    newSettings.thumbnailSize?.let { talesSettingsProvider.thumbnailSize = it.toDomain() }
    newSettings.taskPoolSize?.let { talesSettingsProvider.taskPoolSize = it }

    if (newSettings.isSet("serverPort")) talesSettingsProvider.serverPort = newSettings.serverPort
    if (newSettings.isSet("serverContextPath")) talesSettingsProvider.serverContextPath = newSettings.serverContextPath
  }
}
