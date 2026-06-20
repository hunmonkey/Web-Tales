package org.tales.domain.model

data class ScanResult(
  val series: Map<Series, List<Book>>,
  val sidecars: List<Sidecar>,
)
