package org.gotson.komga.shelfview.api.dto

data class ShelfViewTextContentDto(
  val schemaVersion: Int = 1,
  val bookId: String,
  val contentFingerprint: String,
  val encoding: String,
  val normalizationVersion: Int,
  val paragraphs: List<String>,
)
