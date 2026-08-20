package org.tales.shelfview.interfaces.api

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.tales.domain.model.Media
import org.tales.interfaces.api.rest.dto.BookDto
import org.tales.shelfview.domain.ShelfViewPublicationKind

internal const val SHELFVIEW_LOCATOR_MAX_ITEM_ID_LENGTH = 128
internal const val SHELFVIEW_LOCATOR_MAX_CONTENT_FINGERPRINT_LENGTH = 512

internal data class ShelfViewValidatedLocator(
  val contentFingerprint: String,
)

@Component
class ShelfViewLocatorValidator(
  private val mixedManifestResolver: ShelfViewMixedManifestResolver,
) {
  internal fun validate(
    locator: JsonNode?,
    progression: Float?,
    book: BookDto,
    media: Media,
  ): ShelfViewValidatedLocator {
    val normalizedProgression = progression ?: invalid("progression is required")
    if (!normalizedProgression.isFinite() || normalizedProgression < 0F || normalizedProgression > 1F) {
      invalid("progression must be between 0 and 1")
    }

    return validate(
      locator = locator,
      book = book,
      media = media,
    )
  }

  internal fun validate(
    locator: JsonNode?,
    book: BookDto,
    media: Media,
  ): ShelfViewValidatedLocator {
    if (locator == null || !locator.isObject) invalid("locator must be an object")

    val schemaVersion = locator.requiredInt("schemaVersion")
    if (schemaVersion != SCHEMA_VERSION) invalid("schemaVersion must be $SCHEMA_VERSION")

    val kind = locator.requiredText("kind")
    val contentFingerprint = locator.requiredText("contentFingerprint", SHELFVIEW_LOCATOR_MAX_CONTENT_FINGERPRINT_LENGTH)
    val expectedContentFingerprint = shelfViewContentFingerprint(book, media)
    if (contentFingerprint != expectedContentFingerprint) {
      invalid("contentFingerprint does not match the current book")
    }

    when (kind) {
      KIND_PAGE -> locator.validatePageLocator(shelfViewPageCount(media))
      KIND_TEXT -> locator.validateTextLocator()
      KIND_MIXED -> locator.validateMixedLocator(book, media, expectedContentFingerprint)
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

  private fun JsonNode.validateMixedLocator(
    book: BookDto,
    media: Media,
    bookContentFingerprint: String,
  ) {
    val itemId = requiredText("itemId", SHELFVIEW_LOCATOR_MAX_ITEM_ID_LENGTH)
    val itemIndex = requiredInt("itemIndex")
    if (itemIndex < 0) invalid("itemIndex must be zero or greater")

    val itemLocator = get("itemLocator") ?: invalid("itemLocator is required")
    if (!itemLocator.isObject) invalid("itemLocator must be an object")

    val schemaVersion = itemLocator.requiredInt("schemaVersion")
    if (schemaVersion != SCHEMA_VERSION) invalid("itemLocator schemaVersion must be $SCHEMA_VERSION")

    val itemContentFingerprint = itemLocator.requiredText("contentFingerprint", SHELFVIEW_LOCATOR_MAX_CONTENT_FINGERPRINT_LENGTH)
    val itemKind = itemLocator.requiredText("kind")
    when (itemKind) {
      KIND_PAGE -> itemLocator.validatePageLocator()
      KIND_TEXT -> itemLocator.validateTextLocator()
      else -> invalid("itemLocator kind must be PAGE or TEXT")
    }

    val currentItem = mixedManifestResolver.listCurrentItems(book, media, bookContentFingerprint).getOrNull(itemIndex)
    if (currentItem == null || currentItem.itemId != itemId) {
      invalid("mixed item does not exist in the current manifest")
    }
    if (itemContentFingerprint != currentItem.contentFingerprint) {
      invalid("itemLocator contentFingerprint does not match the current manifest item")
    }

    val expectedKind =
      when (currentItem.kind) {
        ShelfViewPublicationKind.FIXED_PAGE_SEQUENCE -> KIND_PAGE
        ShelfViewPublicationKind.TEXT_REFLOW -> KIND_TEXT
        else -> invalid("mixed manifest item kind is not supported")
      }
    if (itemKind != expectedKind) invalid("itemLocator kind does not match the current manifest item")
    if (itemKind == KIND_PAGE) {
      itemLocator.validatePageLocator(currentItem.pageCount ?: invalid("mixed page item has no page count"))
    }
  }

  private fun JsonNode.requiredInt(name: String): Int {
    val value = get(name) ?: invalid("$name is required")
    if (!value.isIntegralNumber || !value.canConvertToInt()) invalid("$name must be an integer")
    return value.intValue()
  }

  private fun JsonNode.requiredText(
    name: String,
    maxLength: Int? = null,
  ): String {
    val value = get(name) ?: invalid("$name is required")
    if (!value.isTextual || value.textValue().isBlank()) invalid("$name must be a non-empty string")
    val text = value.textValue()
    if (maxLength != null && text.length > maxLength) invalid("$name must be $maxLength characters or fewer")
    return text
  }

  private fun JsonNode.requiredNumber(name: String): Double {
    val value = get(name) ?: invalid("$name is required")
    if (!value.isNumber) invalid("$name must be a number")

    val number = value.doubleValue()
    if (!number.isFinite()) invalid("$name must be finite")
    return number
  }

  private fun invalid(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, "SHELFVIEW_INVALID_LOCATOR: $message")

  private companion object {
    const val SCHEMA_VERSION = 1
    const val KIND_PAGE = "PAGE"
    const val KIND_TEXT = "TEXT"
    const val KIND_MIXED = "MIXED"
  }
}
