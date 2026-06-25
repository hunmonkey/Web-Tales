# ADR-0011: PDF text layer, search and document links

- Status: Accepted for planning
- Date: 2026-06-25
- Ticket: SV-995 (PDF text layer, search and document-link exploration)
- Supersedes: none
- Related: [ADR-0001](ADR-0001-tales-fork-and-reader-strategy.md), [ADR-0003](ADR-0003-zip-internal-mixed-content.md), [ADR-0005](ADR-0005-dedicated-highlight-model.md), [ADR-0007](ADR-0007-image-quality-processing-scope.md), `SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md`, `docs/SECURITY_AND_LICENSE.md`

## Context

ShelfView currently reads standalone PDFs and ZIP-internal PDFs as fixed-page
image sequences. This path is stable and already gives one-page, two-page,
continuous scroll, page jump, image filters, bookmarks and progress.

The missing PDF-specific features are text selection, document-wide search,
accessible text and PDF document links. The master plan says those require a
later ADR-backed PDF.js phase.

### Verified code map

Read directly in the current fork:

1. `tales/src/main/kotlin/org/tales/infrastructure/mediacontainer/pdf/PdfExtractor.kt`
   - Uses PDFBox to count pages, render page images and return a one-page raw
     PDF fragment through `getPageContentAsPdf()`.
2. `tales/src/main/kotlin/org/tales/interfaces/api/CommonBookController.kt`
   - Checks content restrictions before serving pages.
   - Can return a raw PDF page for PDF books when the client requests
     `application/pdf` with higher priority than image content.
3. `tales/src/main/kotlin/org/tales/shelfview/interfaces/api/ShelfViewManifestController.kt`
   - Exposes PDFs as `FIXED_PAGE_SEQUENCE` manifests with `sourceFormat: PDF`.
   - Adds PDF outline contents through `ShelfViewPdfContentsProvider`.
4. `tales/src/main/kotlin/org/tales/shelfview/interfaces/api/ShelfViewPdfContentsProvider.kt`
   - Converts PDF outline targets into bounded ShelfView `PAGE` locators.
   - Does not return raw PDF destinations or server file paths.
5. `tales-webui/src/shelfview/views/ShelfViewReaderShell.vue`
   - Renders PDF pages through the shared fixed-page reader path.
6. `tales-webui/package.json`
   - Does not list `pdfjs-dist` as a direct dependency.
7. `npm ls pdfjs-dist --depth=2`
   - Shows `pdfjs-dist@2.14.305` only as a transitive dependency of
     `@d-i-t-a/reader`.

## Decision

Adopt a staged **PDF text overlay**, not a PDF reader replacement.

1. Keep the current fixed-page image reader as the default PDF display and
   fallback. It remains the source for page layout, filters, progress and
   bookmarks.
2. If PDF text work proceeds, add a focused `PdfTextLayerAdapter` or equivalent
   ShelfView module that overlays selectable/searchable text on top of the
   current page images. Do not make PDF.js the default page renderer in the
   first implementation.
3. Use PDF.js only behind an owner-controlled rollout flag after a worker/build
   spike proves it works with Vue 2, TypeScript 4.9 and the current Vue CLI
   build.
4. Promote `pdfjs-dist` to an explicit direct dependency before importing it
   from ShelfView code. Do not rely on the transitive copy bundled through
   `@d-i-t-a/reader`.
5. Load only visible or near-visible PDF pages for text-layer extraction. Cancel
   stale work when the user pages quickly, changes layout or leaves the reader.
6. Prefer authorized per-page PDF bytes or authorized per-page text/link DTOs
   over whole-document browser loading. Do not send raw server file paths to the
   browser.
7. Document links start with internal page jumps only. Convert destinations into
   ShelfView `PAGE` locators before navigation. External URI links remain
   disabled until a separate owner-approved policy defines allowed schemes and
   confirmation UI.
8. Document-wide search should be incremental and bounded. It can search pages
   already extracted first, then expand in cancellable batches. The UI must
   report partial search state instead of freezing the reader.
9. ZIP-internal PDFs stay on the existing fixed-page image path until a separate
   endpoint and validation plan exists. Do not extract ZIP-internal PDFs in the
   browser.
10. Do not store raw PDF destinations, DOM ranges or PDF.js internals as
    ShelfView saved state. Stored positions and future annotations should use
    versioned ShelfView locators/selectors.

## Tentative Manifest Shape

The current PDF manifest can stay `FIXED_PAGE_SEQUENCE`. A future optional
`pdf` block may advertise text-layer capabilities without changing the base
reader kind:

```json
{
  "kind": "FIXED_PAGE_SEQUENCE",
  "sourceFormat": "PDF",
  "pdf": {
    "pagePdfEndpointTemplate": "/api/v1/books/{bookId}/pages/{page}?zero_based=true",
    "textLayerMode": "PDFJS_VISIBLE_PAGE",
    "linksEndpointTemplate": "/api/shelfview/v1/books/{bookId}/pdf/pages/{page}/links?zero_based=true"
  }
}
```

Exact DTO names can change during implementation. The important boundary is
that PDF-specific fields are opt-in capabilities, while the fixed-page image
contract remains valid.

## Required Gates Before Implementation

1. Dependency and license review for explicit `pdfjs-dist` use.
2. Vue CLI worker import spike for production build and unit-test mode.
3. Browser smoke evidence for Chrome and Edge on a real PDF sample.
4. Mobile or constrained-viewport evidence before enabling text extraction by
   default.
5. Cancellable visible-page extraction with memory and page-count limits.
6. Tests for malformed PDFs, access-denied PDFs and PDFs with no text layer.
7. Link policy tests showing internal links jump by `PAGE` locator and external
   links do not open by default.

## Security Notes

- PDF files are untrusted input. PDF.js and PDFBox parsing failures must fail
  closed and leave the image fallback usable.
- Keep all PDF endpoints authenticated and authorized through existing Tales
  checks.
- Keep source PDF files read-only.
- Do not expose server paths, raw PDF destinations or archive internals.
- Do not render PDF text through `v-html`.
- Bound worker lifetime, page count, text span count, link count and memory.
- Do not add external network loading from PDF documents.

## Consequences

- The first PDF text feature will be less ambitious than a full browser-native
  PDF reader, but it keeps the known-good PDF image reader intact.
- Document-wide search may be progressive at first because it should not load or
  parse a large book all at once.
- Internal PDF links become practical before external links because internal
  page jumps can reuse existing ShelfView `PAGE` locators.
- A future direct PDF renderer remains possible, but it needs its own evidence
  and rollback plan.

## Alternatives Considered

- **Replace PDF rendering with PDF.js immediately.** Rejected for the first
  slice because current fixed-page rendering already works and full replacement
  adds worker, memory, mobile and routing risk.
- **Server-side PDFBox text extraction only.** Rejected as the sole plan because
  browser text selection and visual text-layer alignment are the reason PDF.js
  was listed in the master plan. Server-side DTOs may still be useful for link
  sanitization or search indexing.
- **Use the transitive `pdfjs-dist` dependency directly.** Rejected. Direct
  imports must be declared directly so upgrades, license review and build
  failures are visible.
- **Enable external PDF links immediately.** Rejected until an owner-approved
  link policy exists.

## Reversal Triggers

- PDF.js worker setup cannot be made reliable in the current Vue 2 build.
- Text-layer alignment is poor enough that selection is misleading.
- Memory or battery cost is too high on mobile or constrained devices.
- PDF link handling creates security or user-trust risk.
- A maintained server-side extraction path meets the owner goals with less
  complexity.
