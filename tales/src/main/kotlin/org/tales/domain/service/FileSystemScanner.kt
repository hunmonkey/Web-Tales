package org.tales.domain.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.tales.domain.model.Book
import org.tales.domain.model.DirectoryNotFoundException
import org.tales.domain.model.ScanResult
import org.tales.domain.model.Series
import org.tales.domain.model.Sidecar
import org.tales.infrastructure.sidecar.SidecarBookConsumer
import org.tales.infrastructure.sidecar.SidecarSeriesConsumer
import java.io.IOException
import java.net.URL
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes
import kotlin.time.measureTime

private val logger = KotlinLogging.logger {}

const val SHELFVIEW_IMAGE_DIRECTORY_MARKER = ".shelfview-book"

@Service
class FileSystemScanner(
  private val sidecarBookConsumers: List<SidecarBookConsumer>,
  private val sidecarSeriesConsumers: List<SidecarSeriesConsumer>,
) {
  private data class TempSidecar(
    val name: String,
    val url: URL,
    val lastModifiedTime: LocalDateTime,
    val type: Sidecar.Type? = null,
  )

  private data class ImageDirectoryStateFile(
    val size: Long,
    val lastModifiedTime: LocalDateTime,
  )

  private val sidecarBookPrefilter = sidecarBookConsumers.flatMap { it.getSidecarBookPrefilter() }

  fun scanRootFolder(
    root: Path,
    forceDirectoryModifiedTime: Boolean = false,
    oneshotsDir: String? = null,
    scanCbx: Boolean = true,
    scanPdf: Boolean = true,
    scanEpub: Boolean = true,
    scanTxt: Boolean = false,
    scanImageDirectory: Boolean = false,
    directoryExclusions: Set<String> = emptySet(),
  ): ScanResult {
    val scanForExtensions =
      buildList {
        if (scanCbx) addAll(listOf("cbz", "zip", "cbr", "rar"))
        if (scanPdf) add("pdf")
        if (scanEpub) add("epub")
        if (scanTxt) add("txt")
      }
    logger.info { "Scanning library content" }
    logger.info { "Scan for extensions: $scanForExtensions" }
    logger.info { "Scan ShelfView image directories: $scanImageDirectory" }
    logger.info { "Excluded directory pattern count: ${directoryExclusions.size}" }
    logger.info { "Force directory modified time: $forceDirectoryModifiedTime" }

    if (!(Files.isDirectory(root) && Files.isReadable(root)))
      throw DirectoryNotFoundException("Folder is not accessible: $root", "ERR_1016")

    val scannedSeries = mutableMapOf<Series, List<Book>>()
    val scannedSidecars = mutableListOf<Sidecar>()

    measureTime {
      // path is the series directory
      val pathToSeries = mutableMapOf<Path, Series>()
      val pathToSeriesSidecars = mutableMapOf<Path, MutableList<Sidecar>>()
      // path is the book's parent directory, ie the series directory
      val pathToBooks = mutableMapOf<Path, MutableList<Book>>()
      val pathToBookSidecars = mutableMapOf<Path, MutableList<TempSidecar>>()

      Files.walkFileTree(
        root,
        setOf(FileVisitOption.FOLLOW_LINKS),
        Integer.MAX_VALUE,
        object : FileVisitor<Path> {
          override fun preVisitDirectory(
            dir: Path,
            attrs: BasicFileAttributes,
          ): FileVisitResult {
            logger.trace { "preVisit (regularFile:${attrs.isRegularFile}, directory:${attrs.isDirectory}, symbolicLink:${attrs.isSymbolicLink}, other:${attrs.isOther})" }
            if (dir.name.startsWith(".") ||
              directoryExclusions.any { exclude ->
                dir.pathString.contains(exclude, true)
              }
            )
              return FileVisitResult.SKIP_SUBTREE

            pathToSeries[dir] =
              Series(
                name = dir.name.ifBlank { dir.pathString },
                url = dir.toUri().toURL(),
                fileLastModified = attrs.getUpdatedTime(),
              )

            if (scanImageDirectory && dir.isShelfViewImageDirectory()) {
              val book = pathToImageDirectoryBook(dir, attrs)
              val parent = dir.parent ?: dir
              pathToBooks.merge(parent, mutableListOf(book)) { prev, one -> prev.union(one).toMutableList() }
              return FileVisitResult.SKIP_SUBTREE
            }

            return FileVisitResult.CONTINUE
          }

          override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes,
          ): FileVisitResult {
            logger.trace { "visitFile (regularFile:${attrs.isRegularFile}, directory:${attrs.isDirectory}, symbolicLink:${attrs.isSymbolicLink}, other:${attrs.isOther})" }
            if (!attrs.isSymbolicLink && !attrs.isDirectory) {
              if (scanForExtensions.contains(file.extension.lowercase()) &&
                !file.name.startsWith(".")
              ) {
                val book = pathToBook(file, attrs)
                file.parent.let { key ->
                  pathToBooks.merge(key, mutableListOf(book)) { prev, one -> prev.union(one).toMutableList() }
                }
              }

              sidecarSeriesConsumers
                .firstOrNull { consumer ->
                  consumer.getSidecarSeriesFilenames().any { file.name.equals(it, ignoreCase = true) }
                }?.let {
                  val sidecar = Sidecar(file.toUri().toURL(), file.parent.toUri().toURL(), attrs.getUpdatedTime(), it.getSidecarSeriesType(), Sidecar.Source.SERIES)
                  pathToSeriesSidecars.merge(file.parent, mutableListOf(sidecar)) { prev, one -> prev.union(one).toMutableList() }
                }

              // book sidecars can't be exactly matched during a file visit
              // this prefilters files to reduce the candidates
              if (sidecarBookPrefilter.any { it.matches(file.name) }) {
                val sidecar = TempSidecar(file.name, file.toUri().toURL(), attrs.getUpdatedTime())
                pathToBookSidecars.merge(file.parent, mutableListOf(sidecar)) { prev, one -> prev.union(one).toMutableList() }
              }
            }

            return FileVisitResult.CONTINUE
          }

          override fun visitFileFailed(
            file: Path?,
            exc: IOException?,
          ): FileVisitResult {
            logger.warn { "Could not access scanned entry: errorType=${exc?.javaClass?.simpleName ?: "UNKNOWN"}" }
            return FileVisitResult.SKIP_SUBTREE
          }

          override fun postVisitDirectory(
            dir: Path,
            exc: IOException?,
          ): FileVisitResult {
            logger.trace { "postVisit directory" }
            val books = pathToBooks[dir]
            val tempSeries = pathToSeries[dir]
            if (!books.isNullOrEmpty() && tempSeries !== null) {
              if (!oneshotsDir.isNullOrBlank() && dir.pathString.contains(oneshotsDir, true)) {
                books.forEach { book ->
                  val series =
                    Series(
                      name = book.name,
                      url = book.url,
                      fileLastModified = book.fileLastModified,
                      oneshot = true,
                    )
                  scannedSeries[series] = listOf(book.copy(oneshot = true))
                }
              } else {
                val series =
                  if (forceDirectoryModifiedTime)
                    tempSeries.copy(fileLastModified = maxOf(tempSeries.fileLastModified, books.maxOf { it.fileLastModified }))
                  else
                    tempSeries

                scannedSeries[series] = books

                // only add series sidecars if series has books
                pathToSeriesSidecars[dir]?.let { scannedSidecars.addAll(it) }
              }

              // book sidecars are matched here, with the actual list of books
              books.forEach { book ->
                val sidecars =
                  pathToBookSidecars[dir]
                    ?.mapNotNull { sidecar ->
                      sidecarBookConsumers.firstOrNull { it.isSidecarBookMatch(book.name, sidecar.name) }?.let {
                        sidecar to it.getSidecarBookType()
                      }
                    }?.toMap() ?: emptyMap()
                pathToBookSidecars[dir]?.minusAssign(sidecars.keys)

                sidecars.mapTo(scannedSidecars) { (sidecar, type) ->
                  Sidecar(sidecar.url, book.url, sidecar.lastModifiedTime, type, Sidecar.Source.BOOK)
                }
              }
            }

            return FileVisitResult.CONTINUE
          }
        },
      )
    }.also {
      val countOfBooks = scannedSeries.values.sumOf { it.size }
      logger.info { "Scanned ${scannedSeries.size} series, $countOfBooks books, and ${scannedSidecars.size} sidecars in $it" }
    }

    return ScanResult(scannedSeries, scannedSidecars)
  }

  fun scanFile(path: Path): Book? {
    if (!path.exists()) return null

    return pathToBook(path, path.readAttributes())
  }

  fun scanBookSidecars(path: Path): List<Sidecar> {
    val bookBaseName = path.nameWithoutExtension
    val parent = path.parent
    return parent
      .listDirectoryEntries()
      .filter { candidate -> sidecarBookPrefilter.any { it.matches(candidate.name) } }
      .mapNotNull { candidate ->
        sidecarBookConsumers.firstOrNull { it.isSidecarBookMatch(bookBaseName, candidate.name) }?.let {
          Sidecar(candidate.toUri().toURL(), parent.toUri().toURL(), candidate.readAttributes<BasicFileAttributes>().getUpdatedTime(), it.getSidecarBookType(), Sidecar.Source.BOOK)
        }
      }
  }

  private fun pathToBook(
    path: Path,
    attrs: BasicFileAttributes,
  ): Book =
    Book(
      name = path.nameWithoutExtension,
      url = path.toUri().toURL(),
      fileLastModified = attrs.getUpdatedTime(),
      fileSize = attrs.size(),
    )

  private fun pathToImageDirectoryBook(
    path: Path,
    attrs: BasicFileAttributes,
  ): Book {
    val stateFiles = path.listImageDirectoryStateFiles()
    val directoryLastModified = attrs.getUpdatedTime()

    return Book(
      name = path.nameWithoutExtension,
      url = path.toUri().toURL(),
      fileLastModified = maxOf(directoryLastModified, stateFiles.maxOfOrNull { it.lastModifiedTime } ?: directoryLastModified),
      fileSize = stateFiles.sumOf { it.size },
    )
  }

  private fun Path.listImageDirectoryStateFiles(): List<ImageDirectoryStateFile> =
    Files
      .list(this)
      .use { stream -> stream.toList() }
      .filter { candidate -> Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS) }
      .filterNot { candidate -> candidate.name == SHELFVIEW_IMAGE_DIRECTORY_MARKER || candidate.name.startsWith(".") }
      .mapNotNull { candidate ->
        try {
          val attrs = Files.readAttributes(candidate, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
          ImageDirectoryStateFile(
            size = attrs.size(),
            lastModifiedTime = attrs.getUpdatedTime(),
          )
        } catch (ex: IOException) {
          logger.warn { "Could not read image-directory state file: errorType=${ex.javaClass.simpleName}" }
          null
        }
      }

  private fun Path.isShelfViewImageDirectory(): Boolean = Files.isRegularFile(resolve(SHELFVIEW_IMAGE_DIRECTORY_MARKER), LinkOption.NOFOLLOW_LINKS)
}

fun BasicFileAttributes.getUpdatedTime(): LocalDateTime = maxOf(creationTime(), lastModifiedTime()).toLocalDateTime()

fun FileTime.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this.toInstant(), ZoneId.systemDefault())
