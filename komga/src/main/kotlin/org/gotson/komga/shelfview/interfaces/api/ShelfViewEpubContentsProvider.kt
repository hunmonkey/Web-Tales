package org.gotson.komga.shelfview.interfaces.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.gotson.komga.domain.model.EpubTocEntry
import org.gotson.komga.domain.model.MediaExtension
import org.gotson.komga.domain.model.MediaExtensionEpub
import org.gotson.komga.shelfview.interfaces.api.dto.ShelfViewManifestContentsItemDto
import org.springframework.stereotype.Component

@Component
class ShelfViewEpubContentsProvider(
  private val mapper: ObjectMapper,
) {
  fun listContents(
    extension: MediaExtension?,
    contentFingerprint: String,
  ): List<ShelfViewManifestContentsItemDto> {
    val epubExtension = extension as? MediaExtensionEpub ?: return emptyList()
    val contents = mutableListOf<ShelfViewManifestContentsItemDto>()

    epubExtension.toc.forEach { entry ->
      collectEntry(entry, contentFingerprint, level = 1, contents)
    }

    return contents
  }

  private fun collectEntry(
    entry: EpubTocEntry,
    contentFingerprint: String,
    level: Int,
    contents: MutableList<ShelfViewManifestContentsItemDto>,
  ) {
    if (contents.size >= MAX_CONTENTS_ITEMS || level > MAX_CONTENTS_DEPTH) return

    val label = normalizedLabel(entry.title)
    val href = normalizedHref(entry.href)
    if (label != null && href != null) {
      contents +=
        ShelfViewManifestContentsItemDto(
          label = label,
          locator = epubLocator(href, label, contentFingerprint),
          level = level,
        )
    }

    entry.children.forEach { child ->
      if (contents.size >= MAX_CONTENTS_ITEMS) return
      collectEntry(child, contentFingerprint, level + 1, contents)
    }
  }

  private fun normalizedLabel(title: String): String? =
    title
      .replace(WHITESPACE, " ")
      .trim()
      .take(MAX_LABEL_LENGTH)
      .takeIf { it.isNotBlank() }

  private fun normalizedHref(href: String?): String? =
    href
      ?.trim()
      ?.takeIf { it.isNotBlank() }
      ?.takeIf { it.length <= MAX_HREF_LENGTH }
      ?.takeIf { !it.startsWith("/") && !it.contains("://") }

  private fun epubLocator(
    href: String,
    title: String,
    contentFingerprint: String,
  ) = mapper
    .createObjectNode()
    .put("schemaVersion", 1)
    .put("kind", "EPUB")
    .put("href", href)
    .put("title", title)
    .put("contentFingerprint", contentFingerprint)

  private companion object {
    const val MAX_CONTENTS_ITEMS = 500
    const val MAX_CONTENTS_DEPTH = 8
    const val MAX_LABEL_LENGTH = 160
    const val MAX_HREF_LENGTH = 1000
    val WHITESPACE = Regex("\\s+")
  }
}
