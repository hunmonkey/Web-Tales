package org.tales.shelfview.interfaces.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.tales.infrastructure.security.TalesPrincipal
import org.tales.shelfview.domain.ShelfViewDictionaryEntry
import org.tales.shelfview.domain.ShelfViewDictionaryProvider
import org.tales.shelfview.domain.ShelfViewDictionaryProviderKind
import org.tales.shelfview.domain.persistence.ShelfViewDictionaryRepository
import org.tales.shelfview.interfaces.api.dto.ShelfViewDictionaryLocalPackImportDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewDictionaryLookupResultDto
import org.tales.shelfview.interfaces.api.dto.ShelfViewDictionaryProviderDto
import org.tales.shelfview.interfaces.api.dto.toDto
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.Locale

@RestController
@RequestMapping("api/shelfview/v1/dictionary", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "ShelfView")
class ShelfViewDictionaryController(
  private val dictionaryRepository: ShelfViewDictionaryRepository,
) {
  @Operation(summary = "List ShelfView dictionary providers")
  @GetMapping("providers")
  fun getProviders(
    @AuthenticationPrincipal principal: TalesPrincipal,
  ): Collection<ShelfViewDictionaryProviderDto> =
    dictionaryRepository
      .findProviderSummaries(principal.user.id)
      .map { it.toDto() }

  @Operation(summary = "Import a local ShelfView dictionary pack")
  @PostMapping("providers/local-pack", consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  fun importLocalPack(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @RequestBody request: ShelfViewDictionaryLocalPackImportDto,
  ): ShelfViewDictionaryProviderDto {
    val validated = ShelfViewDictionaryPackValidator.validate(principal.user.id, request)

    dictionaryRepository.saveLocalPack(validated.provider, validated.entries)

    return dictionaryRepository
      .findProviderSummaries(principal.user.id)
      .firstOrNull { it.provider.id == validated.provider.id }
      ?.toDto()
      ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SHELFVIEW_DICTIONARY_SAVE_FAILED")
  }

  @Operation(summary = "Delete a ShelfView dictionary provider")
  @DeleteMapping("providers/{providerId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteProvider(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @PathVariable providerId: String,
  ) {
    if (!dictionaryRepository.deleteProvider(providerId, principal.user.id)) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
  }

  @Operation(summary = "Lookup a term in ShelfView dictionary providers")
  @GetMapping("lookup")
  fun lookup(
    @AuthenticationPrincipal principal: TalesPrincipal,
    @RequestParam query: String = "",
  ): Collection<ShelfViewDictionaryLookupResultDto> {
    val normalizedQuery = ShelfViewDictionaryPackValidator.normalizedLookupKey(query)
    if (normalizedQuery.isBlank()) return emptyList()

    return dictionaryRepository
      .findLookupEntries(principal.user.id, normalizedQuery)
      .map { it.toDto() }
  }
}

internal data class ShelfViewValidatedDictionaryPack(
  val provider: ShelfViewDictionaryProvider,
  val entries: Collection<ShelfViewDictionaryEntry>,
)

internal object ShelfViewDictionaryPackValidator {
  private const val SCHEMA_VERSION = 1
  private const val QUERY_MAX_LENGTH = 80
  private const val PACK_MAX_ENTRIES = 500
  private const val DEFINITION_MAX_LENGTH = 1000
  private const val METADATA_MAX_LENGTH = 240
  private const val SOURCE_URL_MAX_LENGTH = 500

  fun validate(
    userId: String,
    request: ShelfViewDictionaryLocalPackImportDto,
  ): ShelfViewValidatedDictionaryPack {
    if (request.schemaVersion != SCHEMA_VERSION) badRequest("UNSUPPORTED_DICTIONARY_PACK_SCHEMA")
    val metadata = request.metadata ?: badRequest("DICTIONARY_PACK_METADATA_REQUIRED")
    val sourceName = requiredBounded(metadata.sourceName, METADATA_MAX_LENGTH, "DICTIONARY_PACK_SOURCE_REQUIRED")
    val licenseName = requiredBounded(metadata.licenseName, METADATA_MAX_LENGTH, "DICTIONARY_PACK_LICENSE_REQUIRED")
    val entries = request.entries ?: badRequest("DICTIONARY_PACK_ENTRIES_REQUIRED")
    if (entries.isEmpty() || entries.size > PACK_MAX_ENTRIES) badRequest("DICTIONARY_PACK_ENTRIES_INVALID")

    val provider =
      ShelfViewDictionaryProvider(
        userId = userId,
        kind = ShelfViewDictionaryProviderKind.LOCAL_PACK,
        label = optionalBounded(request.label, METADATA_MAX_LENGTH) ?: sourceName,
        sourceName = sourceName,
        sourceUrl = optionalBounded(metadata.sourceUrl, SOURCE_URL_MAX_LENGTH),
        retrievedAt = optionalBounded(metadata.retrievedAt, METADATA_MAX_LENGTH),
        licenseName = licenseName,
        licenseUrl = optionalBounded(metadata.licenseUrl, SOURCE_URL_MAX_LENGTH),
        attribution = optionalBounded(metadata.attribution, METADATA_MAX_LENGTH),
        redistributionNotes = optionalBounded(metadata.redistributionNotes, METADATA_MAX_LENGTH),
      )

    return ShelfViewValidatedDictionaryPack(
      provider = provider,
      entries =
        entries.map {
          val term = requiredBounded(it.term, QUERY_MAX_LENGTH, "DICTIONARY_PACK_TERM_REQUIRED")
          ShelfViewDictionaryEntry(
            providerId = provider.id,
            term = term,
            normalizedTerm = normalizedLookupKey(term),
            definition = requiredBounded(it.definition, DEFINITION_MAX_LENGTH, "DICTIONARY_PACK_DEFINITION_REQUIRED"),
            sourceName = optionalBounded(it.sourceName, METADATA_MAX_LENGTH),
            sourceUrl = optionalBounded(it.sourceUrl, SOURCE_URL_MAX_LENGTH),
            licenseName = optionalBounded(it.licenseName, METADATA_MAX_LENGTH),
          )
        },
    )
  }

  fun normalizedLookupKey(text: String): String =
    text
      .replace(Regex("\\s+"), " ")
      .trim()
      .take(QUERY_MAX_LENGTH)
      .lowercase(Locale.ROOT)

  private fun requiredBounded(
    value: String?,
    maxLength: Int,
    message: String,
  ): String = optionalBounded(value, maxLength) ?: badRequest(message)

  private fun optionalBounded(
    value: String?,
    maxLength: Int,
  ): String? {
    val normalized = value?.replace(Regex("\\s+"), " ")?.trim()
    if (normalized.isNullOrBlank()) return null
    if (normalized.length > maxLength) badRequest("DICTIONARY_PACK_FIELD_TOO_LONG")
    return normalized
  }

  private fun badRequest(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)
}
