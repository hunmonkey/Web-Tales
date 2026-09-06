# ADR-0023: An entry removed from the library stays removed until restored

- Status: Accepted
- Date: 2026-09-06
- Ticket: SV-1299
- Related: ADR-0022 (library PIN, same table pattern), `docs/AI_PROGRESS.md`
  SV-1298 entry,
  `tales/src/main/kotlin/org/tales/domain/service/LibraryContentLifecycle.kt`

## Context

SV-1298 turned "delete" into "remove from library": the book or series is
soft deleted and no file on disk is touched. Soft delete is also the state the
scanner uses for "file went missing", and the scanner undoes it as soon as the
file is seen again - `LibraryContentLifecycle` re-adds the series, then
`tryRestoreSeries` / `tryRestoreBooks` match the soft-deleted rows back by size
and hash. With `Library.scanInterval` defaulting to `EVERY_6H`, a removed
entry returns within hours. The removal dialog says so and points at
`scanDirectoryExclusions`, which is substring-matched (`.../검신무` also
excludes `.../검신무2`) and cannot name a single file.

Two candidates were recorded in the SV-1298 entry:

- (a) append the series folder to `library.scanDirectoryExclusions` during
  removal - no migration, but wrong matching and no per-book form;
- (b) a new column on `SERIES` and `BOOK` plus scanner checks and an
  un-exclude UI - correct per item, but it edits upstream tables and models.

## Decision

**A removal records the entry's URL in a new table, and the scanner drops any
scanned series or book whose URL is in that table before it does anything
else.** The entry is therefore never re-added and never restored, and the
existing soft delete keeps doing what it already does for listings.

- Table `SHELFVIEW_REMOVED_ENTRY (LIBRARY_ID, URL)`, primary key on both,
  `ON DELETE CASCADE` from `LIBRARY` like `SHELFVIEW_LIBRARY_ACCESS`. Upstream
  tables and domain models are untouched.
- `BookLifecycle.removeBookFromLibrary` and
  `SeriesLifecycle.removeSeriesFromLibrary` insert the row, then soft delete
  as before.
- `LibraryContentLifecycle.scanRootFolder` filters the scan result by that
  set. A series whose every book was removed one by one is dropped too, so it
  is soft deleted as "not on disk" rather than kept as an empty series.
- Undo is all-or-nothing per library: `DELETE
  /api/shelfview/v1/libraries/{libraryId}/removed-entries` (admin) clears the
  table for that library and queues a scan, which re-adds the entries and lets
  the existing restore matching bring their read progress back. The library
  menu exposes it as "제거한 항목 복원" behind a confirmation dialog.

## Consequences

- Removal now survives scans, "empty trash" and the auto-empty-trash setting:
  those delete the soft-deleted rows, not the removal record, so the entry is
  simply re-created (without progress) if it is ever restored after that.
- Matching is by exact URL. Moving or renaming a removed file brings it back
  under the new path; that is the same rule `scanDirectoryExclusions` follows.
- Per-item restore is deliberately not built. It needs a list UI and a second
  endpoint; the all-at-once form is enough until someone asks for it.
- `deleteDuplicateBookFiles` shares `removeBookFromLibrary`, so a duplicate
  removed through it also stays out.
