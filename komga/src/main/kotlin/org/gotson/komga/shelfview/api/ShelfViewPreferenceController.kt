package org.gotson.komga.shelfview.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.gotson.komga.domain.model.KomgaUser
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.infrastructure.security.KomgaPrincipal
import org.gotson.komga.interfaces.api.ContentRestrictionChecker
import org.gotson.komga.interfaces.api.persistence.BookDtoRepository
import org.gotson.komga.interfaces.api.persistence.SeriesDtoRepository
import org.gotson.komga.interfaces.api.rest.dto.BookDto
import org.gotson.komga.interfaces.api.rest.dto.SeriesDto
import org.gotson.komga.language.toUTC
import org.gotson.komga.shelfview.api.dto.ShelfViewPreferenceResponseDto
import org.gotson.komga.shelfview.api.dto.ShelfViewPreferenceUpdateDto
import org.gotson.komga.shelfview.api.dto.toDto
import org.gotson.komga.shelfview.domain.ShelfViewReaderPreference
import org.gotson.komga.shelfview.domain.persistence.ShelfViewReaderPreferenceRepository
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("api/shelfview/v1", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ShelfView")
class ShelfViewPreferenceController(
  private val bookDtoRepository: BookDtoRepository,
  private val seriesDtoRepository: SeriesDtoRepository,
  private val libraryRepository: LibraryRepository,
  private val contentRestrictionChecker: ContentRestrictionChecker,
  private val preferenceRepository: ShelfViewReaderPreferenceRepository,
  private val mapper: ObjectMapper,
) {
  @Operation(summary = "Get ShelfView reader preferences")
  @GetMapping("preferences")
  fun getPreference(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestParam scopeType: String?,
    @RequestParam scopeId: String?,
    @RequestParam format: String?,
  ): ShelfViewPreferenceResponseDto {
    val validFormat = validateFormat(format)
    val scope = resolveScope(scopeType, scopeId, principal)

    return preferenceResponse(principal.user.id, scope, validFormat)
  }

  @Operation(summary = "Save ShelfView reader preferences")
  @PutMapping("preferences", consumes = [MediaType.APPLICATION_JSON_VALUE])
  fun savePreference(
    @AuthenticationPrincipal principal: KomgaPrincipal,
    @RequestBody request: ShelfViewPreferenceUpdateDto,
  ): ShelfViewPreferenceResponseDto {
    val validFormat = validateFormat(request.format)
    val scope = resolveScope(request.scopeType, request.scopeId, principal)
    val settings = validateSettings(request.settings, validFormat)

    preferenceRepository.savePreference(
      ShelfViewReaderPreference(
        userId = principal.user.id,
        scopeType = scope.current.scopeType,
        scopeId = scope.current.scopeId,
        format = validFormat,
        settingsJson = mapper.writeValueAsString(settings),
      ),
    )

    return preferenceResponse(principal.user.id, scope, validFormat)
  }

  private fun preferenceResponse(
    userId: String,
    scope: ResolvedPreferenceScope,
    format: String,
  ): ShelfViewPreferenceResponseDto {
    val direct =
      preferenceRepository.findPreferenceOrNull(
        userId = userId,
        scopeType = scope.current.scopeType,
        scopeId = scope.current.scopeId,
        format = format,
      )
    val sources =
      scope.chain.mapNotNull {
        preferenceRepository.findPreferenceOrNull(
          userId = userId,
          scopeType = it.scopeType,
          scopeId = it.scopeId,
          format = format,
        )
      }

    return ShelfViewPreferenceResponseDto(
      scopeType = scope.current.scopeType,
      scopeId = scope.current.scopeId,
      format = format,
      settings = direct?.let { mapper.readTree(it.settingsJson) },
      updatedAt = direct?.updatedAt?.toUTC(),
      effectiveSettings = effectiveSettings(sources),
      sources = sources.map { it.toDto(mapper) },
    )
  }

  private fun resolveScope(
    scopeType: String?,
    scopeId: String?,
    principal: KomgaPrincipal,
  ): ResolvedPreferenceScope {
    val normalizedScopeType = scopeType?.ifBlank { null } ?: invalid("scopeType is required")
    val normalizedScopeId = scopeId?.ifBlank { null } ?: invalid("scopeId is required")

    return when (normalizedScopeType) {
      SCOPE_GLOBAL -> {
        if (normalizedScopeId != GLOBAL_SCOPE_ID) invalid("GLOBAL scopeId must be $GLOBAL_SCOPE_ID")
        ResolvedPreferenceScope(current = PreferenceScope(SCOPE_GLOBAL, GLOBAL_SCOPE_ID))
      }

      SCOPE_LIBRARY -> {
        val libraryId = findAuthorizedLibraryId(normalizedScopeId, principal.user)
        ResolvedPreferenceScope(
          current = PreferenceScope(SCOPE_LIBRARY, libraryId),
          libraryId = libraryId,
        )
      }

      SCOPE_SERIES -> {
        val series = findAuthorizedSeries(normalizedScopeId, principal)
        ResolvedPreferenceScope(
          current = PreferenceScope(SCOPE_SERIES, series.id),
          libraryId = series.libraryId,
          seriesId = series.id,
        )
      }

      SCOPE_BOOK -> {
        val book = findAuthorizedBook(normalizedScopeId, principal)
        ResolvedPreferenceScope(
          current = PreferenceScope(SCOPE_BOOK, book.id),
          libraryId = book.libraryId,
          seriesId = book.seriesId,
          bookId = book.id,
        )
      }

      else -> invalid("scopeType must be GLOBAL, LIBRARY, SERIES or BOOK")
    }
  }

  private fun findAuthorizedLibraryId(
    libraryId: String,
    user: KomgaUser,
  ): String {
    val library = libraryRepository.findByIdOrNull(libraryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    if (!user.canAccessLibrary(library)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
    return library.id
  }

  private fun findAuthorizedSeries(
    seriesId: String,
    principal: KomgaPrincipal,
  ): SeriesDto =
    seriesDtoRepository.findByIdOrNull(seriesId, principal.user.id)?.also {
      contentRestrictionChecker.checkContentRestriction(principal.user, it)
    } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

  private fun findAuthorizedBook(
    bookId: String,
    principal: KomgaPrincipal,
  ): BookDto =
    bookDtoRepository.findByIdOrNull(bookId, principal.user.id)?.also {
      contentRestrictionChecker.checkContentRestriction(principal.user, it)
    } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

  private fun validateFormat(format: String?): String {
    val normalizedFormat = format?.ifBlank { null } ?: invalid("format is required")
    if (normalizedFormat != FORMAT_FIXED_PAGE_SEQUENCE) {
      invalid("format must be $FORMAT_FIXED_PAGE_SEQUENCE")
    }
    return normalizedFormat
  }

  private fun validateSettings(
    settings: JsonNode?,
    format: String,
  ): ObjectNode {
    if (format != FORMAT_FIXED_PAGE_SEQUENCE) invalid("format must be $FORMAT_FIXED_PAGE_SEQUENCE")
    if (settings == null || !settings.isObject) invalid("settings must be an object")

    settings.fieldNames().asSequence().forEach {
      if (it !in SETTINGS_FIELDS) invalid("settings contains unsupported field $it")
    }

    return mapper.createObjectNode().also {
      copyTextSetting(settings, it, "layout", setOf("SINGLE_PAGE", "DOUBLE_PAGE", "CONTINUOUS_SCROLL"))
      copyTextSetting(settings, it, "readingDirection", setOf("LTR", "RTL"))
      copyTextSetting(settings, it, "pageFit", setOf("SCREEN", "WIDTH", "HEIGHT", "ORIGINAL"))
      copyTextSetting(settings, it, "imageFilterPreset", setOf("ORIGINAL", "EYE_COMFORT", "MONO_COMIC", "LOW_LIGHT", "NIGHT_INVERT", "OLED_TEXT"))
    }
  }

  private fun copyTextSetting(
    source: JsonNode,
    target: ObjectNode,
    field: String,
    allowedValues: Set<String>,
  ) {
    val value = source.get(field) ?: return
    if (!value.isTextual || value.asText() !in allowedValues) invalid("$field has an invalid value")
    target.put(field, value.asText())
  }

  private fun effectiveSettings(sources: Collection<ShelfViewReaderPreference>): ObjectNode =
    defaultSettings().also { effective ->
      sources.forEach { preference ->
        val settings = mapper.readTree(preference.settingsJson)
        SETTINGS_FIELDS.forEach { field ->
          settings.get(field)?.let { effective.set<JsonNode>(field, it.deepCopy()) }
        }
      }
    }

  private fun defaultSettings(): ObjectNode =
    mapper.createObjectNode().apply {
      put("layout", "SINGLE_PAGE")
      put("readingDirection", "LTR")
      put("pageFit", "SCREEN")
      put("imageFilterPreset", "ORIGINAL")
    }

  private fun invalid(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_INVALID_PREFERENCE: $message")

  private data class PreferenceScope(
    val scopeType: String,
    val scopeId: String,
  )

  private data class ResolvedPreferenceScope(
    val current: PreferenceScope,
    val libraryId: String? = null,
    val seriesId: String? = null,
    val bookId: String? = null,
  ) {
    val chain: List<PreferenceScope> =
      buildList {
        add(PreferenceScope(SCOPE_GLOBAL, GLOBAL_SCOPE_ID))
        libraryId?.let { add(PreferenceScope(SCOPE_LIBRARY, it)) }
        seriesId?.let { add(PreferenceScope(SCOPE_SERIES, it)) }
        bookId?.let { add(PreferenceScope(SCOPE_BOOK, it)) }
      }
  }

  private companion object {
    const val SCOPE_GLOBAL = "GLOBAL"
    const val SCOPE_LIBRARY = "LIBRARY"
    const val SCOPE_SERIES = "SERIES"
    const val SCOPE_BOOK = "BOOK"
    const val GLOBAL_SCOPE_ID = "GLOBAL"
    const val FORMAT_FIXED_PAGE_SEQUENCE = "FIXED_PAGE_SEQUENCE"

    val SETTINGS_FIELDS = setOf("layout", "readingDirection", "pageFit", "imageFilterPreset")
  }
}
