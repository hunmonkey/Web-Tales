package org.tales.domain.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.tales.domain.model.AuthenticationActivity
import org.tales.domain.model.TalesUser
import java.time.LocalDateTime

interface AuthenticationActivityRepository {
  fun findAll(pageable: Pageable): Page<AuthenticationActivity>

  fun findAllByUser(
    user: TalesUser,
    pageable: Pageable,
  ): Page<AuthenticationActivity>

  fun findMostRecentByUser(
    user: TalesUser,
    apiKeyId: String?,
  ): AuthenticationActivity?

  fun insert(activity: AuthenticationActivity)

  fun deleteByUser(user: TalesUser)

  fun deleteOlderThan(dateTime: LocalDateTime)
}
