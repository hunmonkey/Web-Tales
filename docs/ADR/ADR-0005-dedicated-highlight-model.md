# ADR-0005: Dedicated ShelfView highlight model

- Status: Accepted for planning
- Date: 2026-06-20
- Ticket: SV-930l (Phase 3 dedicated highlight model ADR)
- Supersedes: none
- Related: [ADR-0004](ADR-0004-epub-integration.md), `docs/BACKLOG.md` SV-930

## Context

ShelfView currently has a small TXT highlight feature from SV-930d. It stores a
highlighted TXT paragraph as a normal ShelfView bookmark with a highlight-style
label. That was a safe first slice because it reused the existing authenticated
bookmark API, locator validation and per-user storage.

That bookmark-backed approach is not enough for range and color highlights:

- A bookmark is a navigation point. A highlight is an annotation range or region.
- Bookmark labels and notes are user-facing bookmark metadata, not highlight
  style or selected-text metadata.
- The bookmarks drawer becomes noisy if every highlight is also a bookmark.
- Text-range highlights need start/end offsets, optional selected quote text and
  color/style data.
- Future fixed-page and EPUB highlights need a model that can grow without
  changing the meaning of existing bookmarks.

### Verified code map

Read directly in the current Web-Tales/ShelfView fork:

1. `tales/src/flyway/resources/db/migration/sqlite/V20260618210500__shelfview_reader_state.sql`
   - Defines `SHELFVIEW_READER_POSITION`, `SHELFVIEW_BOOKMARK` and
     `SHELFVIEW_READER_PREFERENCE`.
2. `tales/src/main/kotlin/org/tales/shelfview/domain/ShelfViewReaderState.kt`
   - Contains `ShelfViewReaderPosition`, `ShelfViewBookmark` and
     `ShelfViewReaderPreference` domain records.
3. `tales/src/main/kotlin/org/tales/shelfview/infrastructure/jooq/ShelfViewReaderStateDao.kt`
   - Implements the current state repositories in one DAO backed by generated
     jOOQ table bindings.
4. `tales/src/main/kotlin/org/tales/shelfview/interfaces/api/ShelfViewBookmarkController.kt`
   - Lists, creates, updates and deletes per-user bookmarks behind book access
     checks.
5. `tales/src/main/kotlin/org/tales/shelfview/interfaces/api/ShelfViewLocatorValidator.kt`
   - Validates storable `PAGE`, `TEXT` and `MIXED` locators against the current
     book fingerprint. `EPUB` locators exist for Manifest contents but are not
     yet accepted for saved ShelfView state.
6. `tales-webui/src/shelfview/views/ShelfViewReaderShell.vue`
   - Renders bookmark-backed TXT paragraph highlights and already loads
     bookmarks through `$shelfViewState`.

## Decision

Create a **dedicated ShelfView highlight model** instead of continuing to store
new highlights as bookmarks.

The first implementation should add a forward-only migration and new APIs, but
it should keep the scope to TXT and mixed-TXT highlights. Fixed-page region
highlights and EPUB highlights can be added later when their selector policies
are tested.

### Future table

Add a new main-database table in a future forward-only Flyway migration after
the current highest migration at implementation time:

```sql
CREATE TABLE SHELFVIEW_HIGHLIGHT
(
    ID                  varchar  NOT NULL PRIMARY KEY,
    USER_ID             varchar  NOT NULL,
    BOOK_ID             varchar  NOT NULL,
    LOCATOR_JSON        varchar  NOT NULL,
    SELECTOR_JSON       varchar  NOT NULL,
    CONTENT_FINGERPRINT varchar  NOT NULL,
    COLOR               varchar  NOT NULL,
    NOTE                varchar,
    TEXT_QUOTE          varchar,
    CREATED_AT          datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT          datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (USER_ID) REFERENCES USER (ID) ON DELETE CASCADE,
    FOREIGN KEY (BOOK_ID) REFERENCES BOOK (ID) ON DELETE CASCADE
);

CREATE INDEX IDX_SHELFVIEW_HIGHLIGHT_USER_BOOK
    ON SHELFVIEW_HIGHLIGHT (USER_ID, BOOK_ID);

CREATE INDEX IDX_SHELFVIEW_HIGHLIGHT_BOOK
    ON SHELFVIEW_HIGHLIGHT (BOOK_ID);
```

`LOCATOR_JSON` is the navigation target, using the same versioned ShelfView
locator family as reader position and bookmarks.

`SELECTOR_JSON` is the highlighted range or region. Version it separately from
the navigation locator:

```json
{
  "schemaVersion": 1,
  "kind": "TEXT_RANGE",
  "startParagraph": 12,
  "startCharacterOffset": 3,
  "endParagraph": 13,
  "endCharacterOffset": 42,
  "normalizationVersion": 1
}
```

For mixed ZIP text items, wrap the text range with the stable mixed item
identity:

```json
{
  "schemaVersion": 1,
  "kind": "MIXED_TEXT_RANGE",
  "itemId": "opaque-item-id",
  "itemIndex": 0,
  "itemSelector": {
    "schemaVersion": 1,
    "kind": "TEXT_RANGE",
    "startParagraph": 2,
    "startCharacterOffset": 0,
    "endParagraph": 2,
    "endCharacterOffset": 18,
    "normalizationVersion": 1
  }
}
```

`TEXT_QUOTE` is optional, bounded display context. It must not store full
chapters or raw files. The first implementation should cap it to a small value,
for example 500 Unicode code points after whitespace normalization.

`COLOR` should be an enum-like string, not arbitrary CSS. Start with a small
set such as `YELLOW`, `GREEN`, `BLUE`, `PINK` and `UNDERLINE`.

### Future API

Add endpoints under the existing ShelfView API namespace:

- `GET /api/shelfview/v1/books/{bookId}/highlights`
- `POST /api/shelfview/v1/books/{bookId}/highlights`
- `PUT /api/shelfview/v1/highlights/{highlightId}`
- `DELETE /api/shelfview/v1/highlights/{highlightId}`

The DTO shape should be close to:

```json
{
  "id": "highlight-id",
  "bookId": "book-id",
  "locator": {"schemaVersion": 1, "kind": "TEXT"},
  "selector": {"schemaVersion": 1, "kind": "TEXT_RANGE"},
  "contentFingerprint": "tales-file-hash:...",
  "color": "YELLOW",
  "note": "optional memo",
  "textQuote": "short selected text",
  "createdAt": "2026-06-20T00:00:00Z",
  "updatedAt": "2026-06-20T00:00:00Z"
}
```

Authorization and ownership must mirror bookmarks:

- List/create require the current user to have access to the book.
- Update/delete first find the highlight by id and current user id.
- Other users' highlights return `404` on direct id access.
- Users without book access receive the same forbidden behavior as bookmarks.

### Validation

Reuse `ShelfViewLocatorValidator` for `LOCATOR_JSON`.

Add a separate `ShelfViewHighlightSelectorValidator` for `SELECTOR_JSON`:

- `schemaVersion` must be 1.
- First implementation accepts only `TEXT_RANGE` and `MIXED_TEXT_RANGE`.
- Paragraph and character offsets must be integers greater than or equal to 0.
- The end position must be after the start position.
- `normalizationVersion` must match `SHELFVIEW_TEXT_NORMALIZATION_VERSION`.
- Mixed selectors must include a non-empty `itemId`, non-negative `itemIndex`
  and a nested `TEXT_RANGE`.
- Reject arbitrary DOM ranges, CSS selectors, XPath, server paths or raw EPUB
  reader objects.
- Reject arbitrary colors; accept only the approved enum values.
- Bound optional `note` and `textQuote` lengths.

Do not accept `EPUB` highlights until EPUB saved-state locators and the WebPub
reader adapter are explicitly implemented and tested. The `EPUB` Manifest TOC
locator added in SV-930k is not enough by itself for annotation storage.

### Frontend rollout

The first UI slice should:

1. Add frontend highlight DTOs and service methods.
2. Load highlights separately from bookmarks.
3. Render dedicated TXT and mixed-TXT highlights from `SELECTOR_JSON`.
4. Create a highlight from the selected TXT range, defaulting to `YELLOW`.
5. Keep the existing bookmark-backed paragraph highlights as legacy visual
   hints until a later cleanup path is chosen.

Do not silently migrate existing bookmark-backed highlights. They were created
as bookmarks, may have user notes, and can keep working as bookmarks. A later
manual "copy to highlight" path can be considered if needed.

## Consequences

Positive:

- Bookmarks remain navigation aids; highlights become annotations.
- Range/color highlights can be added without overloading bookmark labels.
- The API can later support fixed-page and EPUB annotation shapes without
  changing bookmark meaning.
- Per-user isolation, book access checks and content fingerprint checks stay in
  the ShelfView state model.

Negative:

- Adds a new table, repository and API surface.
- Frontend state loading needs another request or a combined state endpoint in
  the future.
- Legacy bookmark-backed highlights need compatibility handling during the
  transition.

## Rejected alternatives

- **Keep using bookmarks forever.** Rejected because range, color and annotation
  metadata would keep conflicting with bookmark labels and notes.
- **Store arbitrary browser selection objects.** Rejected because DOM ranges are
  unstable across fonts, widths, themes, TXT normalization and EPUB rendering.
- **Store highlighted source text only.** Rejected because repeated text and
  edits would make navigation unreliable, and storing large text snippets raises
  privacy and storage risk.
- **Auto-migrate old highlight bookmarks.** Rejected for the first slice because
  existing bookmark labels and notes are user data and the marker label is not a
  durable schema.
- **Create one highlight table per format.** Rejected because a single table
  with versioned selector JSON matches the existing ShelfView locator strategy.

## Implementation hook points

Future tickets should stay small:

1. `SV-930m`: add `SHELFVIEW_HIGHLIGHT` migration, jOOQ regeneration, domain
   model, repository methods and DAO tests.
2. `SV-930n`: add highlight API DTOs/controllers/selector validation and
   authorization tests.
3. `SV-930o`: add frontend highlight service/model tests.
4. `SV-930p`: replace new TXT highlight creation with the dedicated highlight
   API while still rendering legacy bookmark-backed highlights.
5. Later: add note/color editing, fixed-page region selectors or EPUB
   annotation support only with targeted tests.

## Revisit triggers

- EPUB is routed into the ShelfView reader and needs Readium/WebPub annotation
  storage.
- Fixed-page region highlights become a priority.
- A combined reader-state endpoint is needed to reduce startup requests.
- Highlight sync/export/import becomes a product requirement.
