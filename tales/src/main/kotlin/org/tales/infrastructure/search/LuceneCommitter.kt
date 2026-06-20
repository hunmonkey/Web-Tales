package org.tales.infrastructure.search

interface LuceneCommitter {
  fun commitAndMaybeRefresh()
}
