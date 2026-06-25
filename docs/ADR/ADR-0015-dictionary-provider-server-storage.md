# ADR-0015: Server-Synced Dictionary Provider Storage

- Status: Accepted for planning
- Date: 2026-06-25
- Ticket: Dictionary provider server/account sync planning
- Supersedes: none
- Related: [ADR-0006](ADR-0006-dictionary-data-source-policy.md), `docs/BACKLOG.md` SV-930

## Context

SV-930 closed the current reading-aids scope at browser-local dictionary packs.
The reader can import a bounded JSON dictionary pack and remember it in browser
`localStorage`, but that pack is not synced across browsers or devices.

The owner-approved follow-up asks for server/account-synced dictionary provider
storage. This is not the same as a normal reader preference:

- Reader preferences are small view settings scoped by format/book/library.
- Dictionary providers can contain third-party content, attribution metadata,
  source URLs, license details and many entries.
- Online dictionary providers may need API keys or endpoint configuration.
- Dictionary query text is user-selected content and should not be logged.

The current code map is:

1. `ADR-0006`
   - Forbids bundling third-party dictionary data in the default app without
     owner-approved source, license, attribution and update policy.
   - Allows user-configured providers or owner-approved packs later.
2. `dictionary-lookup.ts`
   - Defines `WEB_LOOKUP`, `LOCAL_PACK` and `CONFIGURED_API` provider kinds.
   - Parses local dictionary pack JSON with schema version, metadata and entry
     limits.
   - Requires source and license metadata for local packs.
3. `ShelfViewReaderShell.vue`
   - Imports dictionary pack JSON in the browser.
   - Stores the imported pack in `localStorage` under
     `shelfview.dictionaryPack.v1`.
4. `ShelfViewReaderStateDao.kt` / `SHELFVIEW_READER_PREFERENCE`
   - Store reader settings, not dictionary content.
   - Should not be reused for dictionary packs.
5. Existing ShelfView storage
   - Reader position, bookmarks, highlights and preferences are user-scoped.
   - New storage must follow the same user isolation principle.

## Decision

Add a dedicated dictionary storage model in a later implementation slice.
Do not store dictionary packs inside `SHELFVIEW_READER_PREFERENCE`.

The accepted direction is:

1. Add dedicated tables for dictionary providers and local dictionary entries.
2. Scope all provider records by `USER_ID`.
3. Support server-synced `LOCAL_PACK` first.
4. Keep `WEB_LOOKUP` as the built-in fallback and do not store it per user
   unless the user customizes it later.
5. Keep `CONFIGURED_API` metadata out of the first storage slice unless it has
   no secret fields.
6. Do not store online API keys in the database until a separate secret-storage
   decision exists.
7. Preserve source and license metadata with every stored provider.
8. Render definitions as text, not HTML.
9. Keep source media read-only; dictionary storage belongs in the application
   database and optional derived cache, not media folders.

## Planned Schema

The implementation should use a forward-only Flyway migration after the current
highest migration.

Suggested tables:

```sql
CREATE TABLE SHELFVIEW_DICTIONARY_PROVIDER (
    ID TEXT PRIMARY KEY,
    USER_ID TEXT NOT NULL,
    KIND TEXT NOT NULL,
    LABEL TEXT NOT NULL,
    ENABLED BOOLEAN NOT NULL DEFAULT 1,
    SOURCE_NAME TEXT,
    SOURCE_URL TEXT,
    RETRIEVED_AT TEXT,
    LICENSE_NAME TEXT,
    LICENSE_URL TEXT,
    ATTRIBUTION TEXT,
    REDISTRIBUTION_NOTES TEXT,
    CONFIG_JSON TEXT,
    CREATED_AT datetime NOT NULL,
    UPDATED_AT datetime NOT NULL
);

CREATE INDEX IDX_SHELFVIEW_DICTIONARY_PROVIDER_USER
    ON SHELFVIEW_DICTIONARY_PROVIDER (USER_ID);

CREATE TABLE SHELFVIEW_DICTIONARY_ENTRY (
    ID TEXT PRIMARY KEY,
    PROVIDER_ID TEXT NOT NULL,
    TERM TEXT NOT NULL,
    NORMALIZED_TERM TEXT NOT NULL,
    DEFINITION TEXT NOT NULL,
    SOURCE_NAME TEXT,
    SOURCE_URL TEXT,
    LICENSE_NAME TEXT,
    CREATED_AT datetime NOT NULL,
    UPDATED_AT datetime NOT NULL,
    FOREIGN KEY (PROVIDER_ID) REFERENCES SHELFVIEW_DICTIONARY_PROVIDER(ID)
        ON DELETE CASCADE
);

CREATE INDEX IDX_SHELFVIEW_DICTIONARY_ENTRY_PROVIDER_TERM
    ON SHELFVIEW_DICTIONARY_ENTRY (PROVIDER_ID, NORMALIZED_TERM);
```

The exact SQL can change during implementation, but the separation matters:
provider metadata is not entry content, and user dictionary content is not a
reader preference blob.

## API Shape

The first backend slice should expose authenticated endpoints under
`/api/shelfview/v1`:

- `GET /dictionary/providers`
- `POST /dictionary/providers/local-pack`
- `DELETE /dictionary/providers/{providerId}`
- `GET /dictionary/lookup?query=...`

All endpoints are user-scoped. A user can only list, query or delete their own
providers.

The server should reuse the existing frontend pack schema as the contract
baseline, then mirror validation on the backend:

- schema version
- maximum query length
- maximum provider label/source/license field length
- maximum entry count
- maximum term length
- maximum definition length
- required source and license metadata

## Security And License Policy

- Do not log full dictionary lookup queries by default.
- Do not return another user's providers or entries.
- Do not accept HTML definitions as trusted content.
- Do not store API keys in plain client-visible JSON.
- Do not bundle third-party dictionary packs by default.
- Record source name, source URL, license name, license URL, attribution and
  redistribution notes for imported local packs.
- Reject oversized packs before writing entries.
- Make delete operations cascade provider entries.

## Consequences

Positive:

- Dictionary packs can sync across browsers for the same account.
- The license and source metadata stays attached to the stored provider.
- Lookup can happen server-side without shipping the whole pack to every
  browser session.
- Reader preference storage stays small and purpose-specific.

Negative:

- Requires a new migration, repository, DTO validation, API tests and frontend
  service integration.
- Stored dictionary entries may grow the database.
- API-key-backed online providers still need another secret-storage decision.
- Importing third-party content remains the user's responsibility unless the
  owner separately approves a bundled pack and notices.

## Rejected Alternatives

- **Store the dictionary pack JSON in `SHELFVIEW_READER_PREFERENCE`.** Rejected
  because preferences are scoped view settings, while dictionary packs are
  content with source/license obligations and many entries.
- **Store dictionary packs only in browser `localStorage`.** Rejected for the
  approved follow-up because it does not sync across devices.
- **Store API keys in the browser bundle.** Rejected because keys would be
  exposed to every client.
- **Bundle a default third-party dictionary pack now.** Rejected by ADR-0006
  until source, license, attribution and redistribution policy are accepted.

## Follow-Up Implementation Shape

The next implementation ticket should:

1. Add a Flyway migration for provider and entry tables.
2. Add domain models and repository interfaces.
3. Add a jOOQ DAO with user-scoped queries.
4. Add backend DTOs and validation mirroring `dictionary-lookup.ts`.
5. Add authenticated provider import/list/delete/lookup endpoints.
6. Add frontend service methods.
7. Let the reader load account-synced providers and keep browser-local import
   as fallback.
8. Add tests for user isolation, invalid packs, oversized packs, lookup results
   and delete cascade.

## Verification Targets

A later implementation should verify:

- User A cannot list, query or delete User B's dictionary provider.
- A valid local pack imports and can be queried from another browser session.
- Missing license/source metadata is rejected.
- Oversized entry counts and long fields are rejected.
- Definitions render as text.
- Browser-local pack fallback still works when server providers are absent.

## Revisit Triggers

- Owner wants server-wide shared dictionary packs instead of user-private packs.
- Online dictionary providers require API keys or OAuth-style credentials.
- Dictionary entries grow enough to need a cache/indexing strategy.
- A bundled first-party or third-party dictionary pack is approved.
