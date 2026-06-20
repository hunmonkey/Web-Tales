package org.tales.infrastructure.mediacontainer.epub

enum class Epub2Nav(
  val level1: String,
  val level2: String,
) {
  TOC("navMap", "navPoint"),
  PAGELIST("pageList", "pageTarget"),
}
