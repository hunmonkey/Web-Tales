package org.tales.shelfview.interfaces.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Duration

class ShelfViewPdfContentsProviderTest {
  private val provider = ShelfViewPdfContentsProvider(ObjectMapper())

  @Test
  fun `given an outline whose sibling chain loops back when listing contents then each item is listed once`(
    @TempDir dir: Path,
  ) {
    val path =
      writeOutlinePdf(dir) { items ->
        // A -> B -> C -> B: the chain never returns to A, PDFBox's stop condition.
        items[2].cosObject.setItem(COSName.NEXT, items[1].cosObject)
      }

    val contents = assertTimeoutPreemptively(Duration.ofSeconds(10)) { provider.listContents(path, "fingerprint", 3) }

    assertThat(contents.map { it.label }).containsExactly("A", "B", "C")
  }

  @Test
  fun `given an outline item whose children point back to its own list when listing contents then it stops`(
    @TempDir dir: Path,
  ) {
    val path =
      writeOutlinePdf(dir) { items ->
        items[1].cosObject.setItem(COSName.FIRST, items[0].cosObject)
        items[1].cosObject.setItem(COSName.LAST, items[0].cosObject)
      }

    val contents = assertTimeoutPreemptively(Duration.ofSeconds(10)) { provider.listContents(path, "fingerprint", 3) }

    assertThat(contents.map { it.label }).containsExactly("A", "B", "C")
  }

  private fun writeOutlinePdf(
    dir: Path,
    corrupt: (List<PDOutlineItem>) -> Unit,
  ): Path {
    val path = dir.resolve("outline.pdf")
    PDDocument().use { document ->
      val pages = List(3) { PDPage().also(document::addPage) }
      val outline = PDDocumentOutline()
      document.documentCatalog.documentOutline = outline
      val items =
        listOf("A", "B", "C").mapIndexed { index, label ->
          PDOutlineItem().apply {
            title = label
            setDestination(pages[index])
            outline.addLast(this)
          }
        }
      corrupt(items)
      document.save(path.toFile())
    }
    return path
  }
}
