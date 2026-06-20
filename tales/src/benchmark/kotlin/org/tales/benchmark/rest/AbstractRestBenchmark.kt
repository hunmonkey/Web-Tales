package org.tales.benchmark.rest

import org.tales.benchmark.AbstractBenchmark
import org.tales.domain.persistence.TalesUserRepository
import org.tales.infrastructure.security.TalesPrincipal
import org.tales.interfaces.api.rest.BookController
import org.tales.interfaces.api.rest.SeriesController
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Setup
import org.springframework.beans.factory.annotation.Autowired

internal const val DEFAULT_PAGE_SIZE = 20

abstract class AbstractRestBenchmark : AbstractBenchmark() {
  companion object {
    lateinit var userRepository: TalesUserRepository
    lateinit var seriesController: SeriesController
    lateinit var bookController: BookController
  }

  @Autowired
  fun setUserRepository(repository: TalesUserRepository) {
    userRepository = repository
  }

  @Autowired
  fun setSeriesController(controller: SeriesController) {
    seriesController = controller
  }

  @Autowired
  fun setBookController(controller: BookController) {
    bookController = controller
  }

  protected lateinit var principal: TalesPrincipal

  @Setup(Level.Trial)
  fun prepareData() {
    principal =
      TalesPrincipal(
        userRepository.findByEmailIgnoreCaseOrNull("admin@example.org")
          ?: throw IllegalStateException("no user found"),
      )
  }
}
