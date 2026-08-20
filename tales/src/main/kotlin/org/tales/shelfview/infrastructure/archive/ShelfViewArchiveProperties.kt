package org.tales.shelfview.infrastructure.archive

import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * One archive policy for both inherited Divina page extraction and ShelfView
 * mixed/image-directory delivery. Keeping one configured ceiling prevents the
 * legacy page endpoint from becoming a less restricted bypass.
 */
@Component
@ConfigurationProperties(prefix = "shelfview.archive")
@Validated
class ShelfViewArchiveProperties {
  @Positive
  var maxEntries: Int = 10_000

  @Positive
  var maxImageBytes: Long = 50L * 1024 * 1024

  @Positive
  var maxImageWidth: Int = 20_000

  @Positive
  var maxImageHeight: Int = 20_000

  @Positive
  var maxImagePixels: Long = 100_000_000

  @Positive
  var maxPdfBytes: Long = 100L * 1024 * 1024

  @Positive
  var maxPdfPages: Int = 1_000

  @Positive
  var maxNestedArchiveBytes: Long = 100L * 1024 * 1024

  @Positive
  var maxTotalUncompressedBytes: Long = 2L * 1024 * 1024 * 1024

  @Positive
  var maxCentralDirectoryBytes: Long = 32L * 1024 * 1024

  @Positive
  var maxArchiveMetadataBytes: Long = 64L * 1024 * 1024

  @Positive
  var maxCompressionRatio: Double = 100.0
}
