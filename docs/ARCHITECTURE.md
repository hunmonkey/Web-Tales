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

## Image directories

MVP requires a `.shelfview-book` marker in the leaf folder. Automatic guessing is out of scope.

## PDF

MVP renders through the existing page delivery path. PDF.js is a later, ADR-backed phase for text layer, search, links and direct PDF rendering.
