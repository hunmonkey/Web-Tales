# ADR-0010: Nested image archives in fixed-page ZIP books

- Status: Accepted
- Date: 2026-06-24
- Ticket: SV-980 (Nested ZIP/CBZ image archive support)
- Supersedes: none
- Related: [ADR-0003](ADR-0003-zip-internal-mixed-content.md), `0624.txt`

## Context

Some owner ZIP/CBZ comic files are structured as one outer archive containing
chapter archives such as `ch01.cbz` and `ch02.zip`. Each chapter archive then
contains normal image pages. The existing Divina ZIP analyzer only looked at
the immediate children of the outer ZIP, so these books had zero detected image
pages and became `ERR_1006`.

ADR-0003 intentionally rejected nested archives for mixed ZIP content. This
ticket is narrower: image-only nested chapter archives should open through the
existing fixed-page reader without changing the mixed TXT/PDF path or the
frontend reader.

## Decision

Support one level of nested `zip` or `cbz` entries in `ZipExtractor` for
fixed-page image books.

1. When the outer ZIP has no top-level TXT/PDF entries, `ZipExtractor` expands
   top-level `zip`/`cbz` entries by one level.
2. Only image entries inside the nested archive become `MediaContainerEntry`
   page candidates.
3. The nested archive container itself is not added to `Media.files`.
4. Page names use an internal composite key:

   ```text
   <outer-entry-name><U+001F><inner-entry-name>
   ```

   The U+001F unit separator is not exposed in HTTP route parameters. It is
   stored only as the existing server-side page `fileName` value used by
   `getPageContent`.
5. `getEntryStream` recognizes the composite key, opens the outer archive,
   then returns the requested inner image bytes.
6. Existing top-level image ZIP/CBZ behavior remains unchanged.
7. ZIP files with top-level TXT/PDF entries continue to use the existing mixed
   content path. Nested archives inside mixed ZIPs remain out of scope.
8. Nested CBR/RAR/7Z, nested archive depth greater than one, and nested
   non-image content are out of scope.

## Security Limits

- Skip entries containing the reserved composite separator.
- Reject unreadable or encrypted nested entries.
- Bound nested archive bytes to 100 MiB.
- Bound each nested image entry to 50 MiB.
- Bound nested entries and expanded nested pages to 10,000.
- Reject entries whose known compression ratio is over 100:1.
- Log nested-archive failures as warnings without logging source media paths or
  file contents.

These limits are deliberately close to the existing ShelfView archive defaults
while keeping the implementation local to the Divina ZIP extractor.

## Consequences

Positive:

- Chapter-packed comic archives can be analyzed as normal fixed-page books.
- The existing `/api/v1/books/{id}/pages/{page}` reader path is reused.
- No frontend, page API, database schema or mixed ZIP DTO change is required.

Negative:

- Serving a page from a nested chapter archive reopens the chapter archive for
  that request. This is acceptable for the first slice, but a bounded chapter
  byte cache may be useful later if real books show performance issues.
- The composite page name is an internal convention that future extractor work
  must preserve or migrate.

## Verification

- `ZipExtractorTest` covers nested CBZ page discovery, natural chapter/page
  order and composite-name page byte extraction.
- `ZipExtractorTest` covers a nested text-only archive being ignored.
- `BookAnalyzerTest` covers nested image ZIP analysis as `READY` with page
  count 3 and `getPageContent` returning the expected image bytes.
- `BookAnalyzerTest` covers nested non-image archive analysis still returning
  `ERR_1006`.

## Revisit Triggers

- Page serving from large chapter archives is too slow without caching.
- Real owner archives require nested RAR/7Z support.
- Real owner archives mix top-level TXT/PDF content with nested image chapters.
- The project chooses a broader archive-inspection abstraction shared by fixed
  ZIP and mixed ZIP paths.
