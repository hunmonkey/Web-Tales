package org.gotson.komga.shelfview.domain

import com.github.f4b6a3.tsid.TsidCreator
import java.time.LocalDateTime

data class ShelfViewReaderPosition(
  val userId: String,
  val bookId: String,
  val locatorJson: String,
  val progression: Float,
  val contentFingerprint: String,
  val updatedAt: LocalDateTime = LocalDateTime.now(),
)

data class ShelfViewBookmark(
  val userId: String,
  val bookId: String,
  val locatorJson: String,
  val label: String? = null,
  val note: String? = null,
  val id: String = TsidCreator.getTsid256().toString(),
  val createdAt: LocalDateTime = LocalDateTime.now(),
  val updatedAt: LocalDateTime = createdAt,
)

data class ShelfViewReaderPreference(
  val userId: String,
  val scopeType: String,
  val scopeId: String,
  val format: String,
  val settingsJson: String,
  val updatedAt: LocalDateTime = LocalDateTime.now(),
)
