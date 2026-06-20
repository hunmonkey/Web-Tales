package org.tales

import org.tales.domain.model.Book
import org.tales.domain.model.ScanResult
import org.tales.domain.model.Series

fun Map<Series, List<Book>>.toScanResult() = ScanResult(this, emptyList())
