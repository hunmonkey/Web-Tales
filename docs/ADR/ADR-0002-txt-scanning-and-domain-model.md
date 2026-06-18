# ADR-0002: TXT scanning and domain model integration

- Status: Accepted for planning
- Date: 2026-06-18
- Ticket: SV-400 (TXT scanner/domain design spike)
- Supersedes: none
- Related: [ADR-0001](ADR-0001-komga-fork-and-reader-strategy.md), `SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md` §5.3, appendix B

## Context

Milestone M4 adds plain-text (`.txt`) books as a `TEXT_REFLOW` publication. SV-400 is a design-only spike: decide **how** TXT files become books and how they are modeled, **before** writing scan/analysis code (SV-401+). The master plan appendix B requires an ADR before changing the Komga domain model or choosing a large-text strategy.

### Verified code map (read directly, Komga 1.24.4 fork)

How a file becomes a book today, and every place TXT would touch:

1. **Extension gate** — `komga/src/main/kotlin/org/gotson/komga/domain/service/FileSystemScanner.kt`, `scanRootFolder(...)`. It builds `scanForExtensions` from three flags: `scanCbx` → `cbz/zip/cbr/rar`, `scanPdf` → `pdf`, `scanEpub` → `epub`. `visitFile()` turns a file into a `Book` only when `scanForExtensions.contains(file.extension.lowercase())`.
2. **Library options** — `komga/src/main/kotlin/org/gotson/komga/domain/model/Library.kt` defines `scanCbx`, `scanPdf`, `scanEpub: Boolean`. These flags are threaded through 9 files: `Library.kt`, `infrastructure/jooq/main/LibraryDao.kt`, the three `interfaces/api/rest/dto/Library{Creation,Update,}Dto.kt`, `interfaces/api/rest/LibraryController.kt`, `domain/service/LibraryLifecycle.kt`, `domain/service/LibraryContentLifecycle.kt`, and `FileSystemScanner.kt`. They are persisted in the `LIBRARY` table.
3. **Media type** — `komga/src/main/kotlin/org/gotson/komga/domain/model/MediaType.kt` is an enum of `(type=mime, profile, fileExtension, exportType)`: `ZIP/RAR_* → DIVINA`, `EPUB → EPUB`, `PDF → PDF`. `fromMediaType(mime)` looks up by mime.
4. **Media profile** — `komga/src/main/kotlin/org/gotson/komga/domain/model/MediaProfile.kt` = `{ DIVINA, PDF, EPUB }`. `Media.profile` (in `Media.kt`) is derived lazily as `MediaType.fromMediaType(mediaType)?.profile`.
5. **Analysis dispatch** — `komga/src/main/kotlin/org/gotson/komga/domain/service/BookAnalyzer.kt`. `analyze()` calls `contentDetector.detectMediaType(book.path)`; if `MediaType.fromMediaType(...)` returns null the book is stored as `Status.UNSUPPORTED` (so an unregistered `.txt` is silently unsupported today). It then dispatches on `mediaType.profile` with an **exhaustive `when`** over `DIVINA/PDF/EPUB` (no `else`). Four more profile `when` expressions exist in the same file: `getPoster` cover, `getPageContent`, `getFileContent`, and the PDF-only `synthetic pages` path.
6. **Media model** — `komga/src/main/kotlin/org/gotson/komga/domain/model/Media.kt` is page-centric: `pages: List<BookPage>`, `pageCount`. `analyzeDivina()` returns `Status.ERROR` ("ERR_1006") when a book has zero pages. TXT has no intrinsic pages, so it cannot reuse the paged analyzers.
7. **Content access** — analyzers read the raw file via `book.path` (a `Path`). A future ShelfView TXT content endpoint can reuse that same path-from-book pattern; it must not reuse the paged page-streaming path.

### The core tension

A `.txt` can only be scanned as a book by editing upstream files (items 1–6). Komga's `MediaType` requires a non-null `MediaProfile`, and `analyze()`'s `when` is exhaustive, so introducing TXT forces either a new `MediaProfile` branch or an upstream-touching workaround. Full isolation (a parallel ShelfView scanner + book/series model) avoids upstream edits but duplicates scanning, permissions, progress, and the library UI — disproportionate for MVP.

## Decision

Adopt a **hybrid: native scan & identity, isolated analysis & rendering.**

1. **Register TXT natively for identity only.** Add `TXT("text/plain", MediaProfile.TXT, "txt")` to `MediaType`, and add `TXT` to `MediaProfile`. This lets Komga's existing scanner, `Book`/`Series`, library, permission, and progress infrastructure treat a `.txt` as a normal book — no parallel book model.
2. **Gate scanning behind a new library option `scanTxt: Boolean = false`** (default off, mirroring `scanCbx/scanPdf/scanEpub`). This requires the same 9-file thread-through plus a forward-only Flyway migration adding a `SCAN_TXT` column to `LIBRARY`, after the current highest migration, and jOOQ regeneration. Never edit existing migrations.
3. **Keep the TXT analysis branch thin.** The new `MediaProfile.TXT` branch in `BookAnalyzer.analyze()` (and the cover branch) does **not** parse text into pages. It records `Status.READY` with minimal metadata only (file size, detected-encoding candidate, content fingerprint), and `pageCount = 0` (or a single synthetic placeholder if a non-zero count is required downstream — to be confirmed in SV-401). The paged `when` branches (`getPageContent`, `getFileContent`) reject TXT with the existing `MediaUnsupportedException` pattern.
4. **Do all real text work in the ShelfView namespace.** Decoding (encoding detection, normalization), pagination, escaping, and the stable paragraph/character `Locator` live behind `/api/shelfview/v1/.../text` (SV-402) and the frontend `TextReflowAdapter` (SV-403/404). The already-shipped manifest contract (`kind = TEXT_REFLOW`, `text` endpoint, TEXT locator) is the integration seam — no new core endpoint.

This keeps upstream edits to a small, well-identified set (`MediaType`, `MediaProfile` + its ~5 `when` sites, `Library` option plumbing, `FileSystemScanner` extension gate) while all text-handling logic stays isolated and upstream-merge-safe.

### Large-file policy

- Enforce a configurable maximum TXT size (managed via a ShelfView config object + tests, not a hardcoded literal — master plan §10.2). Files over the limit scan as a book but the text endpoint returns a clear, bounded error instead of loading them.
- Enforce a maximum line length; defend against pathological single-line files.
- Analysis (SV-401) computes only size + encoding candidate + fingerprint — it must not load the whole file into memory.
- MVP may read within-limit files fully on the content request; chunked/streamed delivery for very large TXT is deferred to a follow-up (open question below), consistent with master plan §7.2.

### Encoding policy

- Prefer BOM when present; else validate UTF-8; else offer `euc-kr` as a candidate (Korean legacy).
- Auto-detection yields a **candidate**, not a final value; the user can override and the choice is saved per book.
- A `normalizationVersion` (already in the TEXT locator contract) versions newline/whitespace/paragraph normalization so saved positions remain restorable when normalization rules change.

### Implementation hook points (for SV-401+, not this ticket)

- `MediaType.kt`: add `TXT` entry.
- `MediaProfile.kt`: add `TXT`. Adding a profile value forces a new branch in **every exhaustive `when (profile)` in the codebase** — verified during SV-401 to be more than BookAnalyzer alone: `BookAnalyzer.kt` (`analyze`, `getPoster`, `getPageContent`, `getFileContent`; the PDF-only synthetic-pages guard is an `if`, unaffected), `CommonBookController.getWebPubManifestInternal`, `KoreaderSyncController` (2 sites), `OpdsController.toOpdsEntry`, `WebPubGenerator.mediaProfileToWebPub`, and `BookLifecycle.markProgression`. The Kotlin compiler enumerates them all, so this is mechanical but wide — confirming the ADR's "high upstream impact" note.
- `FileSystemScanner.kt`: add `scanTxt` param and `if (scanTxt) add("txt")`.
- `Library.kt` + DTOs + `LibraryController.kt` + `LibraryDao.kt` + lifecycle services: add `scanTxt`.
- New forward-only Flyway migration: `LIBRARY.SCAN_TXT`, after the current highest version; then `:komga:generateJooq`.
- ShelfView: TXT analysis helper + `/api/shelfview/v1` text endpoint (SV-402), `TextReflowAdapter` (SV-403).

## Consequences

Positive:

- Reuses Komga scanning, identity, permissions, progress, and the library UI — no duplicate book model.
- All text decoding/pagination/Locator logic is isolated in the ShelfView namespace and behind `/api/shelfview/v1`.
- Existing `/api/v1` responses and the paged formats are unchanged.
- The change set is small and enumerated, so upstream merges stay tractable.

Negative:

- `MediaType`, `MediaProfile`, `BookAnalyzer`, `FileSystemScanner`, and the `Library` option plumbing are upstream files, so each Komga merge must re-check these edits.
- Adding a `MediaProfile` value makes the exhaustive `when` sites a recurring merge touch-point.
- A new `LIBRARY` column requires a forward-only migration and jOOQ regeneration.
- TXT books carry `pageCount = 0`, which downstream Komga UI (series/book cards, progress %) may render oddly; SV-401/SV-600 must verify.

## Rejected alternatives

- **Full ShelfView isolation (parallel scanner + book/series model):** upstream-impact none, but duplicates scanning, permissions, progress, and the library list — disproportionate for MVP and hard to keep consistent. Rejected.
- **Reuse an existing `MediaProfile` (e.g. treat TXT as EPUB or a 1-page DIVINA):** pollutes profile semantics, breaks cover/page analyzers, and risks the EPUB/DIVINA `when` branches mis-handling TXT. Rejected.
- **Virtual single-"page" media reusing the paged model:** forces a fake page and image pipeline for text; conflicts with the reflow/Locator model and the zero-page `ERR_1006` guard. Rejected.
- **No library gate (always scan `.txt`):** would change behavior for existing Komga libraries that happen to contain `.txt` sidecars/notes. Rejected; `scanTxt` defaults off.

## Revisit triggers

- SV-401 finds that `pageCount = 0` breaks core series/book/progress flows badly enough to need a synthetic page.
- Large-TXT memory pressure forces a streamed/chunked content design (promote the deferred item to its own ADR).
- A future upstream Komga version adds native text support, changing the merge calculus.

## Open questions for the owner

- TXT books with `pageCount = 0`: acceptable, or require one synthetic page for Komga UI/progress consistency? (Resolve in SV-401.)
- Default maximum TXT size and maximum line length values.
- Whether very large TXT needs streamed delivery in MVP or can be deferred.
