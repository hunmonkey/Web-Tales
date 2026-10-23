package org.tales.infrastructure.jooq.main

import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.tales.domain.model.AuthenticationActivity
import org.tales.jooq.main.Tables
import java.time.LocalDateTime
import java.util.TimeZone

@SpringBootTest
class AuthenticationActivityDaoTest(
  @Autowired private val authenticationActivityDao: AuthenticationActivityDao,
  @Autowired private val dsl: DSLContext,
) {
  @AfterEach
  fun cleanup() {
    dsl.deleteFrom(Tables.AUTHENTICATION_ACTIVITY).execute()
  }

  @Test
  fun `given a server outside UTC when an activity is stored then it is kept in UTC and read back unchanged`() {
    val localTime = LocalDateTime.parse("2026-10-23T18:30:00")

    withDefaultTimeZone("Asia/Seoul") {
      authenticationActivityDao.insert(AuthenticationActivity(email = "user@example.org", success = true, dateTime = localTime))

      val stored =
        dsl
          .select(Tables.AUTHENTICATION_ACTIVITY.DATE_TIME)
          .from(Tables.AUTHENTICATION_ACTIVITY)
          .fetchOne()
          ?.value1()
      assertThat(stored).isEqualTo(LocalDateTime.parse("2026-10-23T09:30:00"))
      val readBack =
        authenticationActivityDao
          .findAll(Pageable.unpaged())
          .content
          .single()
          .dateTime
      assertThat(readBack).isEqualTo(localTime)
    }
  }

  private fun withDefaultTimeZone(
    zone: String,
    block: () -> Unit,
  ) {
    val previous = TimeZone.getDefault()
    try {
      TimeZone.setDefault(TimeZone.getTimeZone(zone))
      block()
    } finally {
      TimeZone.setDefault(previous)
    }
  }
}
