package org.gotson.komga.shelfview.api

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

internal data class ShelfViewValidatedLocator(
  val contentFingerprint: String,
)

internal object ShelfViewLocatorValidator {
  private const val SCHEMA_VERSION = 1
  private const val KIND_PAGE = "PAGE"
  private const val KIND_TEXT = "TEXT"
  private const val KIND_MIXED = "MIXED"

  fun validate(
    locator: JsonNode?,
    progression: Float?,
    expectedContentFingerprint: String,
    pageCount: Int,
  ): ShelfViewValidatedLocator {
    val normalizedProgression = progression ?: invalid("progression is required")
    if (!normalizedProgression.isFinite() || normalizedProgression < 0F || normalizedProgression > 1F) {
      invalid("progression must be between 0 and 1")
    }

    return validate(
      locator = locator,
      expectedContentFingerprint = expectedContentFingerprint,
      pageCount = pageCount,
    )
  }

  fun validate(
    locator: JsonNode?,
    expectedContentFingerprint: String,
    pageCount: Int,
  ): ShelfViewValidatedLocator {
    if (locator == null || !locator.isObject) invalid("locator must be an object")

    val schemaVersion = locator.requiredInt("schemaVersion")
    if (schemaVersion != SCHEMA_VERSION) invalid("schemaVersion must be $SCHEMA_VERSION")

    val kind = locator.requiredText("kind")
    val contentFingerprint = locator.requiredText("contentFingerprint")
    if (contentFingerprint != expectedContentFingerprint) {
      invalid("contentFingerprint does not match the current book")
    }

    when (kind) {
      KIND_PAGE -> locator.validatePageLocator(pageCount)
      KIND_TEXT -> locator.validateTextLocator()
      KIND_MIXED -> locator.validateMixedLocator()
      else -> invalid("kind must be PAGE, TEXT or MIXED")
    }

    return ShelfViewValidatedLocator(contentFingerprint)
  }

  private fun JsonNode.validatePageLocator(pageCount: Int? = null) {
    val page = requiredInt("page")
    if (page < 0) invalid("page must be zero or greater")
    if (pageCount != null) {
      if (pageCount < 1) invalid("page cannot be saved before the book has pages")
      if (page >= pageCount) invalid("page must be lower than the book page count")
    }

    val offset = requiredNumber("offset")
    if (offset < 0.0 || offset > 1.0) invalid("offset must be between 0 and 1")
  }

  private fun JsonNode.validateTextLocator() {
    if (requiredInt("paragraph") < 0) invalid("paragraph must be zero or greater")
    if (requiredInt("characterOffset") < 0) invalid("characterOffset must be zero or greater")
    if (requiredInt("normalizationVersion") != SHELFVIEW_TEXT_NORMALIZATION_VERSION) {
      invalid("normalizationVersion must be $SHELFVIEW_TEXT_NORMALIZATION_VERSION")
    }

    val locatorProgression = requiredNumber("progression")
    if (locatorProgression < 0.0 || locatorProgression > 1.0) invalid("locator progression must be between 0 and 1")
  }

  private fun JsonNode.validateMixedLocator() {
    requiredText("itemId")
    if (requiredInt("itemIndex") < 0) invalid("itemIndex must be zero or greater")

    val itemLocator = get("itemLocator") ?: invalid("itemLocator is required")
    if (!itemLocator.isObject) invalid("itemLocator must be an object")

    val schemaVersion = itemLocator.requiredInt("schemaVersion")
    if (schemaVersion != SCHEMA_VERSION) invalid("itemLocator schemaVersion must be $SCHEMA_VERSION")

    itemLocator.requiredText("contentFingerprint")
    when (itemLocator.requiredText("kind")) {
      KIND_PAGE -> itemLocator.validatePageLocator()
      KIND_TEXT -> itemLocator.validateTextLocator()
      else -> invalid("itemLocator kind must be PAGE or TEXT")
    }
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

  private fun JsonNode.requiredNumber(name: String): Double {
    val value = get(name) ?: invalid("$name is required")
    if (!value.isNumber) invalid("$name must be a number")

    val number = value.doubleValue()
    if (!number.isFinite()) invalid("$name must be finite")
    return number
  }

  private fun invalid(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_INVALID_LOCATOR: $message")
}
