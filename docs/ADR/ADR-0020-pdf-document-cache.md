# ADR-0020: Cache open PDF documents for page rendering

- Status: Accepted
- Date: 2026-08-20
- Ticket: SV-1265
- Related: `docs/BACKLOG.md`, `docs/AI_PROGRESS.md`,
  `tales/src/main/kotlin/org/tales/infrastructure/mediacontainer/pdf/PdfExtractor.kt`

## Context

`PdfExtractor.getPageContentAsImage` opens the whole document on every call:

```kotlin
Loader.loadPDF(path.toFile()).use { pdf ->
  val page = pdf.getPage(pageNumber - 1)
  val image = PDFRenderer(pdf).renderImage(pageNumber - 1, page.getScale(), RGB)
```

Every page image request for a PDF book therefore re-parses the file's
cross-reference table and object structure before rendering a single page. It is
reached through `/api/v1/books/{id}/pages/{n}`, which is the endpoint the
ShelfView manifest hands the reader for PDF books.

The reader multiplies this. `SHELFVIEW_PREFETCH_PAGE_COUNT` is 4, and
`FixedPageAdapter` prefetches from offset -1 to +4, so one page turn can issue
six page requests at once — six concurrent full parses of the same file. Reading
a 500-page PDF front to back parses the document more than 500 times.

`/api/v1/books/{id}/pages/{n}/thumbnail` goes through the same call and then
resizes, so opening the reader's thumbnail strip on a PDF repeats it once per
page.

## Decision

Cache open `PDDocument` handles in `PdfExtractor`, keyed by path together with
the file's last-modified time and size, with a **`tryLock` fast path and a
load-per-request fallback**.

Concretely:

- A small Caffeine cache (2 entries, 10 minutes after last access) holds open
  documents. Its removal listener closes them.
- The key includes last-modified and size, so an ordinary replacement with
  changed metadata uses a different key; the old entry ages out and is closed.
  A replacement that deliberately preserves both values can reuse the old
  handle until eviction, so this is invalidation metadata rather than a content
  identity guarantee.
- Each cached document carries its own lock. A request takes the lock with
  `tryLock`. If it gets the lock, it renders from the cached document. **If the
  lock is already held, it falls back to loading its own document exactly as the
  code does today.**

## Why the tryLock fallback rather than blocking

`PDDocument` is not safe for concurrent rendering, so a cache has to serialise
access to a shared handle. That creates a real trade-off, and which side wins
depends on the file:

- Where parsing dominates — a large or badly built PDF — one parse plus
  serialised renders beats six parallel parses by a wide margin.
- Where rendering dominates — a small, simple PDF — serialising six renders that
  currently run in parallel would be **slower than what ships today**.

That trade-off has not been measured on this deployment, and no measurement was
possible while making this change: there is no JDK on the development machine,
so the backend is compiled only by the operations server's Docker build.

Rather than guess which side to optimise for, the design removes the choice. A
request never waits for the lock. Sequential reading — the normal case, where
each page turn's requests arrive after the previous ones finished — hits the
cache and pays one parse. A burst that finds the lock held degrades to exactly
today's behaviour. The change therefore cannot be slower than the current code
in any access pattern; at worst it is the same, plus one cached document's
memory.

## Consequences

- Memory: up to two open `PDDocument` instances are retained, each also holding
  the source file open. Caffeine expires them ten minutes after last access, but
  it does that work during later cache operations, so an idle server can hold
  them longer than ten minutes. `@PreDestroy` closes whatever is left on
  shutdown. This is bounded and small next to the deployment's `-Xmx2g`, but it
  is new resident memory and two new file descriptors that did not exist before.
- The removal listener uses a direct executor. Eviction and `@PreDestroy` do not
  return until the document is closed (and an in-flight render releases its
  lock), rather than leaving file handles on Caffeine's common-pool queue.
- The gain under concurrent prefetch is partial by design. If measurement later
  shows that parsing dominates for real books in this library, raising the
  benefit means blocking on the lock instead of falling back — a one-line change
  to this design, and one that should not be made without that measurement.
- `getPages`, `getPageContentAsPdf` and the ShelfView PDF contents provider keep
  loading per call. They are analysis-time and manifest-time paths, not per-page
  request paths, so they are not what makes reading slow.

## Alternatives rejected

- **Blocking lock on a single cached document.** Best case for large files,
  but can regress small ones, and nothing here can tell which the library holds.
- **A pool of open documents per file.** Keeps parallelism and amortises
  parsing, but multiplies the memory cost by the pool size and adds pool
  lifecycle code for a gain that is still unmeasured.
- **Caching rendered page images instead of documents.** Larger win where it
  hits, but page images are already cached in the browser for a day
  (`cacheImageContent`), so a server-side image cache mostly duplicates that for
  first-time reads while costing far more memory or disk.
