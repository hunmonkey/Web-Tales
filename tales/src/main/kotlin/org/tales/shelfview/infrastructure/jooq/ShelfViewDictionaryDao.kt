package org.tales.shelfview.infrastructure.jooq

import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.tales.infrastructure.jooq.SplitDslDaoBase
import org.tales.jooq.main.Tables
import org.tales.language.toCurrentTimeZone
import org.tales.language.toUTC
import org.tales.shelfview.domain.ShelfViewDictionaryEntry
import org.tales.shelfview.domain.ShelfViewDictionaryLookupEntry
import org.tales.shelfview.domain.ShelfViewDictionaryProvider
import org.tales.shelfview.domain.ShelfViewDictionaryProviderKind
import org.tales.shelfview.domain.ShelfViewDictionaryProviderSummary
import org.tales.shelfview.domain.persistence.ShelfViewDictionaryRepository
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class ShelfViewDictionaryDao(
  dslRW: DSLContext,
  @Qualifier("dslContextRO") dslRO: DSLContext,
) : SplitDslDaoBase(dslRW, dslRO),
  ShelfViewDictionaryRepository {
  private val providerTable = Tables.SHELFVIEW_DICTIONARY_PROVIDER
  private val entryTable = Tables.SHELFVIEW_DICTIONARY_ENTRY

  override fun findProviderSummaries(userId: String): Collection<ShelfViewDictionaryProviderSummary> {
    val providers =
      dslRO
        .selectFrom(providerTable)
        .where(providerTable.USER_ID.eq(userId))
        .orderBy(providerTable.CREATED_AT.asc())
        .fetch { it.toProvider() }

    if (providers.isEmpty()) return emptyList()

    val countField = DSL.count()
    val counts =
      dslRO
        .select(entryTable.PROVIDER_ID, countField)
        .from(entryTable)
        .where(entryTable.PROVIDER_ID.`in`(providers.map { it.id }))
        .groupBy(entryTable.PROVIDER_ID)
        .fetchMap(entryTable.PROVIDER_ID, countField)

    return providers.map {
      ShelfViewDictionaryProviderSummary(
        provider = it,
        entryCount = counts[it.id] ?: 0,
      )
    }
  }

  override fun findLookupEntries(
    userId: String,
    normalizedTerm: String,
  ): Collection<ShelfViewDictionaryLookupEntry> =
    dslRO
      .select()
      .from(entryTable)
      .join(providerTable)
      .on(providerTable.ID.eq(entryTable.PROVIDER_ID))
      .where(providerTable.USER_ID.eq(userId))
      .and(providerTable.ENABLED.eq(true))
      .and(entryTable.NORMALIZED_TERM.eq(normalizedTerm))
      .orderBy(providerTable.CREATED_AT.asc(), entryTable.CREATED_AT.asc())
      .fetch {
        ShelfViewDictionaryLookupEntry(
          provider = it.toProvider(),
          entry = it.toEntry(),
        )
      }

  @Transactional
  override fun saveLocalPack(
    provider: ShelfViewDictionaryProvider,
    entries: Collection<ShelfViewDictionaryEntry>,
  ) {
    dslRW
      .insertInto(
        providerTable,
        providerTable.ID,
        providerTable.USER_ID,
        providerTable.KIND,
        providerTable.LABEL,
        providerTable.ENABLED,
        providerTable.SOURCE_NAME,
        providerTable.SOURCE_URL,
        providerTable.RETRIEVED_AT,
        providerTable.LICENSE_NAME,
        providerTable.LICENSE_URL,
        providerTable.ATTRIBUTION,
        providerTable.REDISTRIBUTION_NOTES,
        providerTable.CONFIG_JSON,
        providerTable.CREATED_AT,
        providerTable.UPDATED_AT,
      ).values(
        provider.id,
        provider.userId,
        provider.kind.name,
        provider.label,
        provider.enabled,
        provider.sourceName,
        provider.sourceUrl,
        provider.retrievedAt,
        provider.licenseName,
        provider.licenseUrl,
        provider.attribution,
        provider.redistributionNotes,
        provider.configJson,
        provider.createdAt.toUTC(),
        provider.updatedAt.toUTC(),
      ).execute()

    val now = LocalDateTime.now(ZoneId.of("Z"))
    val entryQueries =
      entries.map { entry ->
        dslRW
          .insertInto(
            entryTable,
            entryTable.ID,
            entryTable.PROVIDER_ID,
            entryTable.TERM,
            entryTable.NORMALIZED_TERM,
            entryTable.DEFINITION,
            entryTable.SOURCE_NAME,
            entryTable.SOURCE_URL,
            entryTable.LICENSE_NAME,
            entryTable.CREATED_AT,
            entryTable.UPDATED_AT,
          ).values(
            entry.id,
            provider.id,
            entry.term,
            entry.normalizedTerm,
            entry.definition,
            entry.sourceName,
            entry.sourceUrl,
            entry.licenseName,
            entry.createdAt.toUTC(),
            now,
          )
      }
    if (entryQueries.isNotEmpty()) dslRW.batch(entryQueries).execute()
  }

  @Transactional
  override fun deleteProvider(
    providerId: String,
    userId: String,
  ): Boolean =
    dslRW
      .deleteFrom(providerTable)
      .where(providerTable.ID.eq(providerId))
      .and(providerTable.USER_ID.eq(userId))
      .execute() > 0

  @Transactional
  override fun deleteProvidersByUserId(userId: String) {
    dslRW
      .deleteFrom(providerTable)
      .where(providerTable.USER_ID.eq(userId))
      .execute()
  }

  @Transactional
  override fun deleteAllProviders() {
    dslRW.deleteFrom(providerTable).execute()
  }

  private fun Record.toProvider() =
    ShelfViewDictionaryProvider(
      id = get(providerTable.ID),
      userId = get(providerTable.USER_ID),
      kind = ShelfViewDictionaryProviderKind.valueOf(get(providerTable.KIND)),
      label = get(providerTable.LABEL),
      enabled = get(providerTable.ENABLED),
      sourceName = get(providerTable.SOURCE_NAME),
      sourceUrl = get(providerTable.SOURCE_URL),
      retrievedAt = get(providerTable.RETRIEVED_AT),
      licenseName = get(providerTable.LICENSE_NAME),
      licenseUrl = get(providerTable.LICENSE_URL),
      attribution = get(providerTable.ATTRIBUTION),
      redistributionNotes = get(providerTable.REDISTRIBUTION_NOTES),
      configJson = get(providerTable.CONFIG_JSON),
      createdAt = get(providerTable.CREATED_AT).toCurrentTimeZone(),
      updatedAt = get(providerTable.UPDATED_AT).toCurrentTimeZone(),
    )

  private fun Record.toEntry() =
    ShelfViewDictionaryEntry(
      id = get(entryTable.ID),
      providerId = get(entryTable.PROVIDER_ID),
      term = get(entryTable.TERM),
      normalizedTerm = get(entryTable.NORMALIZED_TERM),
      definition = get(entryTable.DEFINITION),
      sourceName = get(entryTable.SOURCE_NAME),
      sourceUrl = get(entryTable.SOURCE_URL),
      licenseName = get(entryTable.LICENSE_NAME),
      createdAt = get(entryTable.CREATED_AT).toCurrentTimeZone(),
      updatedAt = get(entryTable.UPDATED_AT).toCurrentTimeZone(),
    )
}
