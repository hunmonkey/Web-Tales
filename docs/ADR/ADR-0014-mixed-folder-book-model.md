# ADR-0014: Mixed Folder Book Model

- Status: Accepted for planning
- Date: 2026-06-25
- Ticket: SV-1000 (Mixed folder-book model ADR for TXT/PDF plus image chapter archives)
- Supersedes: none
- Related: [ADR-0002](ADR-0002-txt-scanning-and-domain-model.md), [ADR-0003](ADR-0003-zip-internal-mixed-content.md), [ADR-0010](ADR-0010-nested-image-archives-in-fixed-zip.md), `docs/SV-996_ARCHIVE_EXPANSION_TRIAGE.md`

## Context

The current image-directory feature is intentionally narrow. A folder becomes a
book only when it contains a `.shelfview-book` marker, and the analyzer treats
direct child image files as fixed pages. Top-level TXT/PDF files plus nested
image chapter archives are a different model: one folder would contain multiple
ordered readable items, some text and some fixed-page.

The current code map is:

1. `FileSystemScanner.kt`
   - Uses `.shelfview-book` to opt a leaf folder into image-directory scanning.
   - Creates one `Book` for the marked folder and skips descending into it.
   - Computes image-directory identity from direct non-marker files.
2. `BookAnalyzer.kt`
   - Detects a marked image directory before MIME detection.
   - `analyzeImageDirectory()` only promotes direct image files to pages.
   - Non-image direct files become `Media.files`, not readable mixed items.
3. `ShelfViewImageDirectoryContentProvider.kt`
   - Serves direct child image files only.
   - Rejects hidden names, nested paths, symbolic links and invalid pages.
4. Mixed ZIP support
   - `MIXED_CONTENT_SEQUENCE` already models one ordered book with TXT and
     fixed-page items behind opaque item ids.
   - It is archive-entry based, not folder-entry based.
5. TXT support
   - Native TXT files become one book through the TXT scan path.
   - TXT decoding and rendering stay behind ShelfView text endpoints.

## Decision

Plan mixed folder books as a new explicit folder-book model, not as an
extension of `.shelfview-book` image directories.

The accepted direction is:

1. Use a new marker file, `.shelfview-mixed-book`.
2. Keep `.shelfview-book` image-directory behavior unchanged.
3. Do not auto-detect ordinary folders that happen to contain TXT/PDF/images.
4. A mixed folder book is one Tales `Book` whose ShelfView manifest is
   `MIXED_CONTENT_SEQUENCE`.
5. Direct child readable items become ordered manifest items:
   - TXT files use the ShelfView text content contract.
   - PDF files use the fixed-page PDF image contract.
   - Direct image files use the image-directory page validation contract.
   - Nested ZIP/CBZ image chapters use the SV-997 validation model.
6. RAR/CBR or 7Z image chapters remain unavailable until ADR-0012 or ADR-0013
   implementation work exists.
7. Nested subdirectories are rejected in the first slice. The folder model is
   direct-child only.
8. API ids are opaque and derived from the folder content fingerprint plus
   normalized child metadata. Raw filesystem paths are never route ids.
9. Original media remains read-only.

## Scan Gate

The first implementation should reuse the existing image-directory scan gate
instead of adding a new database flag:

- `scanImageDirectory = false` keeps both `.shelfview-book` and
  `.shelfview-mixed-book` inactive.
- `scanImageDirectory = true` enables explicit ShelfView folder-book markers.
- The admin UI label can later be widened from "image folder" to "folder
  books" without changing the stored field.

A separate `scanMixedDirectory` flag is only worth the migration and UI cost if
owner feedback shows that image folders and mixed folders need independent
switches.

## Ordering And Identity

The mixed folder order should be natural sort by normalized direct child name,
matching mixed ZIP item ordering and image-directory page ordering.

The book-level fingerprint should change when any direct readable child changes:

- child name
- child size
- child modified time
- child content hash when hashing is enabled
- marker presence

The first implementation should ignore hidden files and the marker file, reject
symbolic links and reject nested directories. Sidecar handling should remain
separate from the readable item list.

## Security Policy

A later implementation must enforce:

- explicit marker only
- direct child files only
- no symbolic-link item files
- no hidden item files
- no raw server path or raw filesystem path in API ids
- TXT byte and line-length limits
- PDF byte and page-count limits
- image byte, MIME, dimension and pixel limits
- archive path, byte, entry-count, compression-ratio and image limits for
  nested ZIP/CBZ image chapters
- readable item count limit
- source media read-only

## Consequences

Positive:

- Keeps existing image-directory books stable.
- Reuses the already built mixed-content reader model.
- Gives owner-organized folders a clear way to represent one book with mixed
  chapters.
- Avoids automatic folder guessing.

Negative:

- Requires a new media type/profile or an equivalent explicit mixed-directory
  branch, touching the same exhaustive profile sites as other new native media
  identities.
- Requires new folder-content endpoints; the ZIP mixed endpoints cannot safely
  be reused as-is because their lookup model is archive-entry based.
- Requires careful lifecycle tests for file add/delete/rename and marker
  removal.
- Existing OPDS, KOReader sync and WebPub paths need explicit unsupported or
  fallback behavior for the new folder model.

## Rejected Alternatives

- **Extend `.shelfview-book` to mean mixed folder.** Rejected because existing
  image-directory users may have notes or PDFs in a marked folder that should
  remain non-readable metadata.
- **Auto-detect folders with TXT/PDF/images.** Rejected because ordinary series
  folders often contain many unrelated files and sidecars.
- **Split each child file into a separate Tales book.** Rejected because the
  owner intent is one readable work with ordered internal items and one
  progress/bookmark state.
- **Reuse mixed ZIP endpoints for folders.** Rejected because folder paths have
  different security and lifecycle rules from archive entries.

## Follow-Up Implementation Shape

The next implementation ticket should:

1. Add a mixed-folder marker constant.
2. Add scanner recognition behind `scanImageDirectory`.
3. Add a media identity for mixed folder books.
4. Add an analyzer that records ordered direct child item metadata without
   reading large content into memory.
5. Add ShelfView content providers and endpoints for folder TXT, folder PDF,
   folder image and nested ZIP/CBZ image chapter items.
6. Add manifest tests proving `MIXED_CONTENT_SEQUENCE` item and contents
   locators.
7. Add lifecycle tests for marker add/remove and direct child changes.

## Verification Targets

A later implementation should verify:

- `.shelfview-mixed-book` folder with `00-intro.txt`, `01.pdf` and `ch01.cbz`
  scans as one book.
- Natural ordering is stable.
- TXT, PDF and nested ZIP/CBZ chapter items appear as mixed manifest items.
- Removing the marker makes the book follow the normal missing-book lifecycle.
- Direct child add/delete/rename changes the book state.
- Hidden files, symbolic links, nested directories and path traversal attempts
  are rejected.
- Existing `.shelfview-book` image-directory tests remain unchanged.

## Revisit Triggers

- Owner wants the same marker for image-only and mixed folder books.
- Mixed folder books need nested subdirectories.
- RAR/CBR or 7Z image chapters become required before their archive ADRs are
  implemented.
- The existing `scanImageDirectory` setting name causes too much admin UI
  confusion and needs a new stored flag.
