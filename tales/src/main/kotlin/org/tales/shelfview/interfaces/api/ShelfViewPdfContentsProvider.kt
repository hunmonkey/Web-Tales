package org.tales.shelfview.interfaces.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.springframework.stereotype.Component
import org.tales.shelfview.interfaces.api.dto.ShelfViewManifestContentsItemDto
import java.nio.file.Path

@Component
class ShelfViewPdfContentsProvider(
  private val mapper: ObjectMapper,
) {
  fun listContents(
    path: Path,
    contentFingerprint: String,
    pageCount: Int,
  ): List<ShelfViewManifestContentsItemDto> {
    if (pageCount < 1) return emptyList()

    return try {
      Loader.loadPDF(path.toFile()).use { document ->
        val outline = document.documentCatalog.documentOutline ?: return emptyList()
        val contents = mutableListOf<ShelfViewManifestContentsItemDto>()

        outline.children().forEach { item ->
          collectItem(document, item, contentFingerprint, pageCount, level = 1, contents)
        }

        contents
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun collectItem(
    document: PDDocument,
    item: PDOutlineItem,
    contentFingerprint: String,
    pageCount: Int,
    level: Int,
    contents: MutableList<ShelfViewManifestContentsItemDto>,
  ) {
    if (contents.size >= MAX_CONTENTS_ITEMS || level > MAX_CONTENTS_DEPTH) return

    val label = normalizedLabel(item.title)
    val pageIndex = destinationPageIndex(document, item)
    if (label != null && pageIndex != null && pageIndex in 0 until pageCount) {
      contents +=
        ShelfViewManifestContentsItemDto(
          label = label,
          locator = pageLocator(pageIndex, contentFingerprint),
          level = level,
        )
    }

    item.children().forEach { child ->
      if (contents.size >= MAX_CONTENTS_ITEMS) return
      collectItem(document, child, contentFingerprint, pageCount, level + 1, contents)
    }
  }

  private fun normalizedLabel(title: String?): String? =
    title
      ?.replace(WHITESPACE, " ")
      ?.trim()
      ?.take(MAX_LABEL_LENGTH)
      ?.takeIf { it.isNotBlank() }

  private fun destinationPageIndex(
    document: PDDocument,
    item: PDOutlineItem,
  ): Int? =
    try {
      val page = item.findDestinationPage(document) ?: return null
      document.pages.indexOf(page).takeIf { it >= 0 }
    } catch (_: Exception) {
      null
    }

  private fun pageLocator(
    page: Int,
    contentFingerprint: String,
  ) = mapper
    .createObjectNode()
    .put("schemaVersion", 1)
    .put("kind", "PAGE")
    .put("page", page)
    .put("offset", 0.0)
    .put("contentFingerprint", contentFingerprint)

  private companion object {
    const val MAX_CONTENTS_ITEMS = 500
    const val MAX_CONTENTS_DEPTH = 8
    const val MAX_LABEL_LENGTH = 160
    val WHITESPACE = Regex("\\s+")
  }
}
