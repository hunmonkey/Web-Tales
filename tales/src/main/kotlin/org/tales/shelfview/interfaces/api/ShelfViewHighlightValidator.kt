package org.tales.shelfview.interfaces.api

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

internal data class ShelfViewValidatedHighlight(
  val color: String,
  val note: String?,
  val textQuote: String?,
)

internal object ShelfViewHighlightValidator {
  private const val SCHEMA_VERSION = 1
  private const val KIND_TEXT_RANGE = "TEXT_RANGE"
  private const val KIND_MIXED_TEXT_RANGE = "MIXED_TEXT_RANGE"
  private const val KIND_TEXT = "TEXT"
  private const val KIND_MIXED = "MIXED"
  private const val MAX_NOTE_LENGTH = 2000
  private const val MAX_TEXT_QUOTE_LENGTH = 500
  private val COLORS = setOf("YELLOW", "GREEN", "BLUE", "PINK", "UNDERLINE")
  private val WHITESPACE = Regex("\\s+")

  fun validate(
    locator: JsonNode?,
    selector: JsonNode?,
    color: String?,
    note: String?,
    textQuote: String?,
  ): ShelfViewValidatedHighlight {
    validateSelector(locator, selector)
    return ShelfViewValidatedHighlight(
      color = normalizedColor(color),
      note = normalizedOptionalText(note, MAX_NOTE_LENGTH, "note"),
      textQuote = normalizedOptionalText(textQuote, MAX_TEXT_QUOTE_LENGTH, "textQuote"),
    )
  }

  private fun validateSelector(
    locator: JsonNode?,
    selector: JsonNode?,
  ) {
    if (locator == null || !locator.isObject) invalid("locator must be an object")
    if (selector == null || !selector.isObject) invalid("selector must be an object")

    val schemaVersion = selector.requiredInt("schemaVersion")
    if (schemaVersion != SCHEMA_VERSION) invalid("selector schemaVersion must be $SCHEMA_VERSION")

    when (selector.requiredText("kind")) {
      KIND_TEXT_RANGE -> {
        if (locator.requiredText("kind") != KIND_TEXT) invalid("TEXT_RANGE requires a TEXT locator")
        selector.validateTextRange()
      }
      KIND_MIXED_TEXT_RANGE -> selector.validateMixedTextRange(locator)
      else -> invalid("selector kind must be TEXT_RANGE or MIXED_TEXT_RANGE")
    }
  }

  private fun JsonNode.validateMixedTextRange(locator: JsonNode) {
    if (locator.requiredText("kind") != KIND_MIXED) invalid("MIXED_TEXT_RANGE requires a MIXED locator")

    val itemId = requiredText("itemId")
    val itemIndex = requiredInt("itemIndex")
    if (itemIndex < 0) invalid("itemIndex must be zero or greater")
    if (locator.requiredText("itemId") != itemId || locator.requiredInt("itemIndex") != itemIndex) {
      invalid("mixed selector item must match the locator item")
    }

    val itemSelector = get("itemSelector") ?: invalid("itemSelector is required")
    if (!itemSelector.isObject) invalid("itemSelector must be an object")
    if (itemSelector.requiredText("kind") != KIND_TEXT_RANGE) invalid("itemSelector kind must be TEXT_RANGE")
    itemSelector.validateTextRange()
  }

  private fun JsonNode.validateTextRange() {
    if (requiredInt("schemaVersion") != SCHEMA_VERSION) invalid("text range schemaVersion must be $SCHEMA_VERSION")
    if (requiredText("kind") != KIND_TEXT_RANGE) invalid("text range kind must be TEXT_RANGE")
    if (requiredInt("normalizationVersion") != SHELFVIEW_TEXT_NORMALIZATION_VERSION) {
      invalid("normalizationVersion must be $SHELFVIEW_TEXT_NORMALIZATION_VERSION")
    }

    val startParagraph = requiredInt("startParagraph")
    val startCharacterOffset = requiredInt("startCharacterOffset")
    val endParagraph = requiredInt("endParagraph")
    val endCharacterOffset = requiredInt("endCharacterOffset")
    if (startParagraph < 0 || endParagraph < 0) invalid("paragraph indexes must be zero or greater")
    if (startCharacterOffset < 0 || endCharacterOffset < 0) invalid("character offsets must be zero or greater")
    if (endParagraph < startParagraph || (endParagraph == startParagraph && endCharacterOffset <= startCharacterOffset)) {
      invalid("text range end must be after start")
    }
  }

  private fun normalizedColor(color: String?): String {
    val normalized = color?.trim()?.uppercase() ?: "YELLOW"
    if (normalized !in COLORS) invalid("color must be one of ${COLORS.joinToString()}")
    return normalized
  }

  private fun normalizedOptionalText(
    text: String?,
    maxLength: Int,
    name: String,
  ): String? {
    val normalized =
      text
        ?.replace(WHITESPACE, " ")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    if (normalized.length > maxLength) invalid("$name must be $maxLength characters or fewer")
    return normalized
  }

  private fun JsonNode.requiredInt(name: String): Int {
    val value = get(name) ?: invalid("$name is required")
    if (!value.isIntegralNumber || !value.canConvertToInt()) invalid("$name must be an integer")
    return value.intValue()
  }

  private fun JsonNode.requiredText(name: String): String {
    val value = get(name) ?: invalid("$name is required")
    if (!value.isTextual || value.textValue().isBlank()) invalid("$name must be a non-empty string")
    return value.textValue()
  }

  private fun invalid(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_INVALID_HIGHLIGHT: $message")
}
