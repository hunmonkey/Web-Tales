package org.tales.domain.persistence

import org.tales.domain.model.ApiKey
import org.tales.domain.model.TalesUser

interface TalesUserRepository {
  fun count(): Long

  fun findByIdOrNull(id: String): TalesUser?

  fun findByEmailIgnoreCaseOrNull(email: String): TalesUser?

  fun findByApiKeyOrNull(apiKey: String): Pair<TalesUser, ApiKey>?

  fun findAll(): Collection<TalesUser>

  fun findApiKeyByUserId(userId: String): Collection<ApiKey>

  fun existsByEmailIgnoreCase(email: String): Boolean

  fun existsApiKeyByIdAndUserId(
    apiKeyId: String,
    userId: String,
  ): Boolean

  fun existsApiKeyByCommentAndUserId(
    comment: String,
    userId: String,
  ): Boolean

  fun insert(user: TalesUser)

  fun insert(apiKey: ApiKey)

  fun update(user: TalesUser)

  fun delete(userId: String)

  fun deleteAll()

  fun deleteApiKeyByIdAndUserId(
    apiKeyId: String,
    userId: String,
  )

  fun deleteApiKeyByUserId(userId: String)

  fun findAnnouncementIdsReadByUserId(userId: String): Set<String>

  fun saveAnnouncementIdsRead(
    user: TalesUser,
    announcementIds: Set<String>,
  )
}
