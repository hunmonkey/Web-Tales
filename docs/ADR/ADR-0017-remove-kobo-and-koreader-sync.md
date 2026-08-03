# ADR-0017: Remove Kobo Sync and KOReader Sync

- Status: Accepted
- Date: 2026-08-03
- Ticket: SV-1076
- Related: `docs/BACKLOG.md`, `docs/ARCHITECTURE.md`,
  `docs/SECURITY_AND_LICENSE.md`, `docs/AI_PROGRESS.md`

## Context

The upstream fork ships two external device-sync integrations:

- **Kobo Sync** — an emulation of the Kobo store sync protocol under `/kobo/**`,
  with its own security filter chain, API-key-in-URL authentication, sync-point
  bookkeeping tables, a Kobo store reverse proxy, and an EPUB-to-KEPUB conversion
  step that shells out to an external `kepubify` binary.
- **KOReader Sync** — a kosync-compatible progress sync endpoint under
  `/koreader/**`, with its own security filter chain, a header-based API-key
  converter, an anonymous user-creation endpoint, and a KOReader-compatible
  partial-MD5 file hash stored per book.

This deployment is a private self-hosted reader. It has no Kobo device, no
KOReader client, and no requirement to reach the Kobo store. Both features are
therefore permanently unused attack surface: two extra security filter chains,
one anonymous public endpoint, one outbound HTTP client to a third-party host,
and one external process invocation.

The owner asked for complete removal with no leftover traces, not for the
features to be hidden in the UI. A previous change (2026-06-24) only filtered the
two sync roles out of the add-user dialog; the backend remained fully wired.

### The `koboSpan` question

`R2Locator.koboSpan` initially looked like shared reader infrastructure rather
than a sync concern, because it is produced inside `EpubExtractor.computePositions`.
Reading that code precisely shows otherwise:

- For reflowable EPUB, position count and progression are always derived from the
  Readium approximation (one position per 1024 bytes). `koboSpan` does not affect
  either value.
- The `kepubify` conversion in `computePositions` exists only to look up a span id
  to store in that one field.
- The only consumers of the value are `KoboController` and `ReadingStateDto`. The
  frontend has zero references.

So keeping `koboSpan` after removing Kobo Sync would mean running a full external
EPUB conversion on every EPUB analysis to populate a field nothing reads.

## Decision

Remove both integrations completely, including their persistence.

1. Delete the `/kobo/**` and `/koreader/**` HTTP surfaces, their DTOs, their
   security filter chains, and the anonymous `/koreader/users/create` exception.
2. Delete the Kobo store reverse proxy (`KoboProxy`) and the sync token generator.
   No outbound connection to a third-party host remains.
3. Delete sync points entirely. They are Kobo-only bookkeeping: the domain model,
   repository, lifecycle, jOOQ DAO, the `DELETE /api/v1/syncpoints/me` REST
   endpoint and the frontend service and plugin.
4. Delete the `KOBO_SYNC` and `KOREADER_SYNC` user roles.
5. Delete the KOReader partial-MD5 hash: the hasher, the per-library
   `hashKoreader` option, the per-book `fileHashKoreader` value and the task that
   computes it.
6. Delete `koboSpan`, `computePositionsFromKoboSpan`, `isKepub`,
   `Media.epubIsKepub`, `KepubConverter` and the `kepubify` path setting. EPUB
   positions keep the Readium approximation they already used.
7. Add one new Flyway migration that drops the now-unreachable schema instead of
   leaving orphaned tables and columns behind.

## Consequences

### Breaking changes (accepted by the owner)

- Any Kobo device or KOReader client configured against this server stops
  working. There is no migration path back other than restoring the fork's code.
- The `DELETE /api/v1/syncpoints/me` endpoint disappears from the public API.
- Existing `USER_ROLE` rows for `KOBO_SYNC` and `KOREADER_SYNC` are deleted.
- Existing KOReader file hashes are deleted with the `BOOK.FILE_HASH_KOREADER`
  column. Recomputing them would require restoring the feature.
- Persisted `R2Locator` JSON keeps its stored `koboSpan` key in old rows. Jackson
  is configured to ignore unknown properties, so old read-progress rows still
  deserialize; the field is simply dropped on the next write.

### Data migration

A single forward-only Flyway migration:

- drops `SYNC_POINT`, `SYNC_POINT_BOOK`, `SYNC_POINT_BOOK_REMOVED_SYNCED`,
  `SYNC_POINT_READLIST`, `SYNC_POINT_READLIST_BOOK` and
  `SYNC_POINT_READLIST_REMOVED_SYNCED`;
- deletes `USER_ROLE` rows with role `KOBO_SYNC` or `KOREADER_SYNC`;
- drops `LIBRARY.HASH_KOREADER`, `BOOK.FILE_HASH_KOREADER` and
  `MEDIA.EPUB_IS_KEPUB`.

Existing migrations are not edited. Rolling back requires restoring a database
backup, so a backup must be taken before deploying this change.

### Upstream merge cost

This is a deliberate divergence from the fork. Future upstream merges that touch
Kobo or KOReader files will conflict as deletions. That cost is accepted: the
owner will never enable either feature.

### Preserved

- The `kepubify` third-party notice, if present in license files, stays until a
  license review confirms it is no longer required. Legal notices are not removed
  as part of a feature deletion.
- Git history retains the deleted code. That is not a leftover trace to clean up.

## Alternatives considered

- **Disable via configuration.** Rejected: the endpoints, filter chains and the
  anonymous user-creation route would still be reachable in a misconfiguration,
  which is exactly the attack surface the owner wants gone.
- **Remove code but leave the tables.** Rejected by the owner. jOOQ would keep
  generating classes for the dead tables from the migrated schema, which is a
  visible leftover trace.
- **Rename `koboSpan` and keep the kepubify path.** Considered and rejected once
  it was confirmed that nothing reads the value. Keeping it would preserve a
  per-analysis external process call and the Docker `kepubify` dependency for no
  behavioral benefit.
