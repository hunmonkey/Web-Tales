package org.tales.infrastructure.mediacontainer.epub

import org.apache.commons.compress.archivers.zip.ZipFile
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.tales.domain.model.MediaUnsupportedException
import org.tales.infrastructure.util.getEntryInputStream
import org.tales.shelfview.infrastructure.archive.ShelfViewArchiveProperties
import org.tales.shelfview.infrastructure.archive.withPreflightedZip
import java.nio.file.Path
import java.nio.file.Paths

data class EpubPackage(
  val zip: ZipFile,
  val opfDoc: Document,
  val opfDir: Path?,
  val manifest: Map<String, ManifestItem>,
)

fun <R> Path.epub(
  archiveProperties: ShelfViewArchiveProperties,
  block: (EpubPackage) -> R,
): R =
  withPreflightedZip(this, archiveProperties) { zip ->
    val opfFile = zip.getPackagePath()
    val opfDoc = zip.getEntryInputStream(opfFile)?.use { Jsoup.parse(it, null, "", Parser.xmlParser()) } ?: throw MediaUnsupportedException("Could not open OPF resource")
    val opfDir = Paths.get(opfFile).parent
    block(EpubPackage(zip, opfDoc, opfDir, opfDoc.getManifest()))
  }

/**
 * Returns the zip entry path of the Epub package file
 */
fun ZipFile.getPackagePath(): String =
  getEntryInputStream("META-INF/container.xml")
    ?.use { Jsoup.parse(it, null, "", Parser.xmlParser()) }
    ?.getElementsByTag("rootfile")
    ?.first()
    ?.attr("full-path") ?: throw MediaUnsupportedException("META-INF/container.xml does not contain rootfile tag")

/**
 * Returns the content of the Epub package file as a [String]
 */
fun getPackageFileContent(
  path: Path,
  archiveProperties: ShelfViewArchiveProperties,
): String? =
  withPreflightedZip(path, archiveProperties) { zip ->
    try {
      zip.getEntryInputStream(zip.getPackagePath())?.reader()?.use { it.readText() }
    } catch (e: Exception) {
      null
    }
  }
