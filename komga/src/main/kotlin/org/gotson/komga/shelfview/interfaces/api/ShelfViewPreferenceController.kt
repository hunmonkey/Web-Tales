package org.gotson.komga.shelfview.interfaces.api

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
import org.gotson.komga.shelfview.domain.ShelfViewReaderPreference
import org.gotson.komga.shelfview.domain.persistence.ShelfViewReaderPreferenceRepository
import org.gotson.komga.shelfview.interfaces.api.dto.ShelfViewPreferenceResponseDto
import org.gotson.komga.shelfview.interfaces.api.dto.ShelfViewPreferenceUpdateDto
import org.gotson.komga.shelfview.interfaces.api.dto.toDto
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
      effectiveSettings = effectiveSettings(format, sources),
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
    if (normalizedFormat !in SUPPORTED_FORMATS) {
      invalid("format must be one of ${SUPPORTED_FORMATS.joinToString(", ")}")
    }
    return normalizedFormat
  }

  private fun validateSettings(
    settings: JsonNode?,
    format: String,
  ): ObjectNode {
    if (settings == null || !settings.isObject) invalid("settings must be an object")

    val allowedFields = settingsFields(format)
    settings.fieldNames().asSequence().forEach {
      if (it !in allowedFields) invalid("settings contains unsupported field $it")
    }

    return mapper.createObjectNode().also { target ->
      when (format) {
        FORMAT_FIXED_PAGE_SEQUENCE -> {
          copyTextSetting(settings, target, "layout", setOf("SINGLE_PAGE", "DOUBLE_PAGE", "CONTINUOUS_SCROLL"))
          copyTextSetting(settings, target, "readingDirection", setOf("LTR", "RTL"))
          copyTextSetting(settings, target, "pageFit", setOf("SCREEN", "WIDTH", "HEIGHT", "ORIGINAL"))
          copyTextSetting(settings, target, "imageFilterPreset", setOf("ORIGINAL", "IMAGE_ENHANCE", "FADED_SCAN", "CANVAS_SHARPEN", "EYE_COMFORT", "MONO_COMIC", "LOW_LIGHT", "NIGHT_INVERT", "OLED_TEXT"))
          copyIntegerChoiceSetting(settings, target, "fixedPageRotation", setOf(0, 90, 180, 270))
          copyBooleanSetting(settings, target, "fixedPageCoverSingle")
          copyIntegerSetting(settings, target, "fixedPageScale", 50, 300)
          copyIntegerSetting(settings, target, "fixedPageGap", 0, 64)
          copyIntegerSetting(settings, target, "fixedPageMargin", 0, 96)
          copyTextSetting(settings, target, "fixedPageBackground", setOf("BLACK", "DARK", "GRAY", "PAPER", "WHITE"))
          copyDecimalSetting(settings, target, "fixedPageBrightness", 0.8, 1.2)
          copyDecimalSetting(settings, target, "fixedPageContrast", 0.85, 1.35)
        }

        FORMAT_TEXT_REFLOW -> {
          copyTextSetting(settings, target, "layout", setOf("SINGLE_PAGE", "CONTINUOUS_SCROLL"))
          copyTextSetting(settings, target, "textTheme", setOf("PAPER", "DARK", "OLED"))
          copyTextSetting(settings, target, "textFontFamily", setOf("SYSTEM", "GOWUN_DODUM", "GOWUN_BATANG", "READABLE_KR", "SERIF", "SANS", "MONO"))
          copyIntegerSetting(settings, target, "textFontSize", 14, 32)
          copyDecimalSetting(settings, target, "textLineHeight", 1.35, 2.4)
          copyDecimalSetting(settings, target, "textLetterSpacing", 0.0, 2.5)
          copyDecimalSetting(settings, target, "textWordSpacing", 0.0, 8.0)
          copyIntegerSetting(settings, target, "textContentWidth", 520, 1180)
          copyIntegerSetting(settings, target, "textPageMargin", 16, 120)
          copyDecimalSetting(settings, target, "textParagraphSpacing", 0.6, 2.6)
          copyTextSetting(settings, target, "textAlign", setOf("LEFT", "JUSTIFY"))
          copyIntegerSetting(settings, target, "textFontWeight", 300, 700)
          copyDecimalSetting(settings, target, "textBrightness", 0.8, 1.2)
        }
      }
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

  private fun copyIntegerSetting(
    source: JsonNode,
    target: ObjectNode,
    field: String,
    min: Int,
    max: Int,
  ) {
    val value = source.get(field) ?: return
    if (!value.isIntegralNumber || value.asInt() !in min..max) invalid("$field has an invalid value")
    target.put(field, value.asInt())
  }

  private fun copyIntegerChoiceSetting(
    source: JsonNode,
    target: ObjectNode,
    field: String,
    allowedValues: Set<Int>,
  ) {
    val value = source.get(field) ?: return
    if (!value.isIntegralNumber || value.asInt() !in allowedValues) invalid("$field has an invalid value")
    target.put(field, value.asInt())
  }

  private fun copyBooleanSetting(
    source: JsonNode,
    target: ObjectNode,
    field: String,
  ) {
    val value = source.get(field) ?: return
    if (!value.isBoolean) invalid("$field has an invalid value")
    target.put(field, value.asBoolean())
  }

  private fun copyDecimalSetting(
    source: JsonNode,
    target: ObjectNode,
    field: String,
    min: Double,
    max: Double,
  ) {
    val value = source.get(field) ?: return
    val number = value.asDouble()
    if (!value.isNumber || !number.isFinite() || number < min || number > max) invalid("$field has an invalid value")
    target.put(field, number)
  }

  private fun effectiveSettings(
    format: String,
    sources: Collection<ShelfViewReaderPreference>,
  ): ObjectNode =
    defaultSettings(format).also { effective ->
      val allowedFields = settingsFields(format)
      sources.forEach { preference ->
        val settings = mapper.readTree(preference.settingsJson)
        allowedFields.forEach { field ->
          settings.get(field)?.let { effective.set<JsonNode>(field, it.deepCopy()) }
        }
      }
    }

  private fun defaultSettings(format: String): ObjectNode =
    mapper.createObjectNode().apply {
      when (format) {
        FORMAT_TEXT_REFLOW -> {
          put("layout", "SINGLE_PAGE")
          put("textTheme", "PAPER")
          put("textFontFamily", "SYSTEM")
          put("textFontSize", 20)
          put("textLineHeight", 1.75)
          put("textLetterSpacing", 0.0)
          put("textWordSpacing", 0.0)
          put("textContentWidth", 760)
          put("textPageMargin", 56)
          put("textParagraphSpacing", 1.35)
          put("textAlign", "LEFT")
          put("textFontWeight", 400)
          put("textBrightness", 1.0)
        }

        else -> {
          put("layout", "SINGLE_PAGE")
          put("readingDirection", "LTR")
          put("pageFit", "SCREEN")
          put("imageFilterPreset", "ORIGINAL")
          put("fixedPageRotation", 0)
          put("fixedPageCoverSingle", false)
          put("fixedPageScale", 100)
          put("fixedPageGap", 12)
          put("fixedPageMargin", 16)
          put("fixedPageBackground", "DARK")
          put("fixedPageBrightness", 1.0)
          put("fixedPageContrast", 1.0)
        }
      }
    }

  private fun settingsFields(format: String): Set<String> =
    when (format) {
      FORMAT_TEXT_REFLOW -> TEXT_REFLOW_SETTINGS_FIELDS
      else -> FIXED_PAGE_SETTINGS_FIELDS
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
    const val FORMAT_TEXT_REFLOW = "TEXT_REFLOW"

    val SUPPORTED_FORMATS = setOf(FORMAT_FIXED_PAGE_SEQUENCE, FORMAT_TEXT_REFLOW)
    val FIXED_PAGE_SETTINGS_FIELDS =
      setOf(
        "layout",
        "readingDirection",
        "pageFit",
        "imageFilterPreset",
        "fixedPageRotation",
        "fixedPageCoverSingle",
        "fixedPageScale",
        "fixedPageGap",
        "fixedPageMargin",
        "fixedPageBackground",
        "fixedPageBrightness",
        "fixedPageContrast",
      )
    val TEXT_REFLOW_SETTINGS_FIELDS =
      setOf(
        "layout",
        "textTheme",
        "textFontFamily",
        "textFontSize",
        "textLineHeight",
        "textLetterSpacing",
        "textWordSpacing",
        "textContentWidth",
        "textPageMargin",
        "textParagraphSpacing",
        "textAlign",
        "textFontWeight",
        "textBrightness",
      )
  }
}
