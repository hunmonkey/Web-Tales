# ShelfView Architecture

## Goal

Extend Komga without replacing its proven library, identity, authorization, analysis, page streaming, metadata and deployment foundations.

## Boundaries

- Existing API remains under `/api/v1`.
- New endpoints live under `/api/shelfview/v1`.
- New backend code is isolated under a ShelfView namespace where practical.
- New frontend reader code is isolated under a ShelfView module where practical.
- `/data` is read-only; persistent state is in `/config`; derived data is in `/cache` or an existing Komga cache.

## Reader model

The server returns a versioned Manifest:

- `FIXED_PAGE_SEQUENCE`: CBZ, ZIP, PDF, IMAGE_DIRECTORY
- `TEXT_REFLOW`: TXT

The UI uses adapters:

- `FixedPageAdapter`
- `TextReflowAdapter`

The UI must not scatter file-extension checks across components.

## Location model

- Fixed pages: page index + offset + fingerprint
- TXT: paragraph + character offset + progression + normalization version + fingerprint
- Page indexes are zero-based internally and one-based only in presentation.

## Data

- Preserve existing Komga progress compatibility for fixed pages.
- Add separate ShelfView storage for extended positions, multiple bookmarks and scoped preferences.
- Never edit existing Flyway migrations.

### ShelfView storage recovery

SV-300 uses a forward-only Flyway migration. If a restore is needed before the ShelfView state APIs are in use, stop the app and restore the SQLite database under `/config` from backup, or drop only the three `SHELFVIEW_*` tables added by the migration when losing ShelfView-only reader state is acceptable. Existing Komga read progress remains in `READ_PROGRESS`.

## Feature rollout

A new reader route is introduced behind a feature flag. The existing reader remains available as fallback until compatibility and recovery have been validated.

## ZIP internal content

Planned ZIP expansion is tracked as SV-450 through SV-453. In addition to the current fixed-page ZIP/CBZ image flow, ShelfView should support ZIP files that contain TXT, JPG and GIF entries.

- Do not send the whole archive to the browser for automatic extraction.
- Reuse server-side archive inspection and page/content delivery patterns where possible.
- Keep source archives read-only and do not expose raw server paths or unsafe internal entry names.
- Validate archive traversal, nested entries, entry count, decompressed size, line length and image dimensions before serving content.
- Add an ADR before implementation because a mixed TXT/image archive may require a Manifest shape beyond a single `FIXED_PAGE_SEQUENCE` or `TEXT_REFLOW` adapter.
- Decide GIF behavior in the ADR: preserve animation when safe, or fall back to a static first-frame page if that is safer for the first release.

## Image directories

MVP requires a `.shelfview-book` marker in the leaf folder. Automatic guessing is out of scope.

## PDF

MVP renders through the existing page delivery path. PDF.js is a later, ADR-backed phase for text layer, search, links and direct PDF rendering.

## TXT

TXT uses a hybrid model decided in [ADR-0002](ADR/ADR-0002-txt-scanning-and-domain-model.md): native scan and identity, isolated analysis and rendering.

- A `.txt` is registered natively as `MediaType.TXT` / `MediaProfile.TXT` so it reuses Komga scanning, identity, permissions and progress.
- Scanning is gated by a new `scanTxt` library option (default off), with a forward-only Flyway migration adding `LIBRARY.SCAN_TXT`.
- The native analysis branch is thin: it records minimal metadata (size, encoding candidate, fingerprint) and does not parse pages.
- All decoding, normalization, pagination, escaping and the paragraph/character Locator live in the ShelfView namespace behind `/api/shelfview/v1/.../text` and the frontend `TextReflowAdapter`.
- Encoding is auto → utf-8 → euc-kr candidate, overridable and saved per book; `normalizationVersion` versions normalization so saved positions stay restorable.
