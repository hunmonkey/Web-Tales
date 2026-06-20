# ADR-0003: ZIP internal mixed content

- Status: Accepted for planning
- Date: 2026-06-19
- Ticket: SV-450 (ZIP internal TXT/PDF/JPG/GIF design spike)
- Supersedes: none
- Related: [ADR-0001](ADR-0001-tales-fork-and-reader-strategy.md), [ADR-0002](ADR-0002-txt-scanning-and-domain-model.md), `SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md` sections 3.4, 5.1 and M4.5

## Context

The owner goal includes ZIP files that contain readable TXT, PDF, JPG and similar entries. Existing Tales ZIP/CBZ handling is optimized for image-only archives: the archive becomes one `Book`, image entries become fixed pages, and non-image entries are stored as `Media.files` metadata.

SV-450 is a design-only spike before implementing ZIP-internal TXT/PDF/JPG/GIF support. The master plan requires an ADR because mixed archives combine the fixed-page reader and text reader inside one physical source file.

### Verified code map

Read directly in the current Tales 1.24.4 fork:

1. `tales/src/main/kotlin/org/tales/infrastructure/mediacontainer/divina/ZipExtractor.kt`
   - Opens a ZIP with Apache Commons Compress `ZipFile`.
   - Iterates all non-directory entries, detects media type from entry content, optionally analyzes image dimensions, records file size and naturally sorts by entry name.
   - `getEntryStream(path, entryName)` uses `getZipEntryBytes`.
2. `tales/src/main/kotlin/org/tales/domain/service/BookAnalyzer.kt`
   - `analyzeDivina()` partitions ZIP entries into image entries and others.
   - Image entries become `BookPage` records and are served by the fixed-page pipeline.
   - Other entries become `MediaFile` records.
   - A ZIP with no image pages currently becomes `Status.ERROR` / `ERR_1006`.
   - `getFileContent()` can extract a named file from a DIVINA archive.
3. `tales/src/main/kotlin/org/tales/domain/model/Media.kt`, `MediaFile.kt`, `MediaContainerEntry.kt`
   - `Media.pages` already stores image pages.
   - `Media.files` can store non-page archive entries, but has only EPUB-specific subtype values today.
4. `tales/src/main/kotlin/org/tales/infrastructure/util/ZipFileUtils.kt`
   - Existing helper reads a single ZIP entry by name, including a slower Unicode-extra-field fallback.
   - It reads the full entry into memory and does not by itself enforce ShelfView mixed-content limits.
5. Current ShelfView manifest contracts:
   - `FIXED_PAGE_SEQUENCE` for CBZ/ZIP/PDF/image directories.
   - `TEXT_REFLOW` for TXT.
   - There is no mixed-content Manifest shape yet.

## Decision

Adopt a **single Book, ordered internal content model** for mixed ZIP archives.

1. A ZIP file remains one Tales `Book`. ZIP entries do **not** become separate books or series.
2. ShelfView adds a mixed publication shape, tentatively `kind = MIXED_CONTENT_SEQUENCE`, with an ordered `items`/`spine` array. Each item points to a safe server endpoint and has a stable opaque `itemId`.
3. The frontend adds a `MixedContentAdapter` that coordinates existing fixed-page and text behavior instead of scattering extension checks in Vue components.
4. Entry paths are never exposed as raw server paths or trusted identifiers. The server may expose a sanitized display label, but API calls use opaque `itemId` values derived from normalized entry metadata and the book content fingerprint.
5. ZIP-internal TXT reuses the ShelfView text decoding policy: UTF-8 BOM, strict UTF-8, strict EUC-KR fallback, optional override, size limit, line length limit, JSON plain text only, no `v-html`.
6. ZIP-internal JPG/JPEG/PNG/WebP/GIF entries use fixed-page item delivery. GIF animation is preserved by default when the original GIF passes validation; generated thumbnails may use a static first frame later, but the reader page endpoint should not silently replace animation with a still image.
7. ZIP-internal PDF entries are supported as fixed-page items without PDF.js in MVP. The server extracts the single bounded PDF entry to derived cache storage keyed by book fingerprint and `itemId`, then reuses the existing PDF page analysis/rendering path. It must not extract the whole archive.
8. Mixed ZIP locators use a wrapper shape, tentatively:

```json
{
  "schemaVersion": 1,
  "kind": "MIXED",
  "itemId": "opaque-item-id",
  "itemIndex": 2,
  "itemLocator": {
    "schemaVersion": 1,
    "kind": "TEXT",
    "paragraph": 12,
    "characterOffset": 44,
    "progression": 0.31,
    "normalizationVersion": 1,
    "contentFingerprint": "tales-file-state:..."
  },
  "contentFingerprint": "tales-file-state:..."
}
```

The nested locator is either `PAGE` or `TEXT`. Overall reader progression is computed from item index plus nested item progression.

### Tentative Manifest shape

The exact DTO names can change during SV-451/SV-453, but the shape should stay close to:

```json
{
  "schemaVersion": 1,
  "bookId": "book-id",
  "title": "Archive title",
  "kind": "MIXED_CONTENT_SEQUENCE",
  "sourceFormat": "ZIP",
  "contentFingerprint": "tales-file-state:...",
  "items": [
    {
      "itemId": "opaque-text-id",
      "index": 0,
      "kind": "TEXT_REFLOW",
      "label": "chapter.txt",
      "text": {
        "endpoint": "/api/shelfview/v1/books/{bookId}/mixed/{itemId}/text",
        "normalizationVersion": 1
      }
    },
    {
      "itemId": "opaque-image-id",
      "index": 1,
      "kind": "FIXED_PAGE_SEQUENCE",
      "label": "001.jpg",
      "pageCount": 1,
      "pages": {
        "endpointTemplate": "/api/shelfview/v1/books/{bookId}/mixed/{itemId}/pages/{page}"
      }
    },
    {
      "itemId": "opaque-pdf-id",
      "index": 2,
      "kind": "FIXED_PAGE_SEQUENCE",
      "label": "bonus.pdf",
      "sourceFormat": "PDF",
      "pageCount": 4,
      "pages": {
        "endpointTemplate": "/api/shelfview/v1/books/{bookId}/mixed/{itemId}/pages/{page}"
      }
    }
  ],
  "capabilities": {
    "search": false,
    "textSelection": true,
    "doublePage": true,
    "continuousScroll": true,
    "rtl": false,
    "imageFilters": true
  }
}
```

## Security policy

Mixed ZIP support must add a ShelfView-specific archive inspector before serving any entry:

- Reject absolute paths, Windows drive paths, `..` traversal after normalization, empty names, duplicate normalized names and suspicious control characters.
- Reject symlink-like entries or entries whose ZIP external attributes indicate links or non-regular files when detectable.
- Reject encrypted archives and nested archive entries as readable content in MVP.
- Enforce maximum entry count, maximum per-entry uncompressed size, maximum total selected uncompressed size and maximum compression ratio.
- Enforce TXT byte and line-length limits before returning content.
- Enforce image MIME sniffing, maximum dimensions and maximum pixel count before exposing image pages.
- Enforce PDF maximum bytes and maximum page count before caching and rendering an internal PDF item.
- Stream or bound reads; do not rely on the existing `getZipEntryBytes` helper for large or untrusted mixed entries without extra limits.
- Do not log raw text contents, credentials, tokens, full source paths or full untrusted entry names.
- Source archives remain read-only. Derived PDF cache files live under cache/config and are safe to delete/rebuild.

## Implementation hook points

SV-451:

- Add a ShelfView ZIP inspector under the ShelfView namespace.
- Reuse existing `ZipExtractor` behavior where safe, but add the mixed-content security limits above.
- Add endpoints for internal TXT content.
- Reuse the TXT content provider by extracting a bounded entry stream/byte source rather than duplicating decoding rules.

SV-452:

- Add page delivery for internal image and PDF items.
- Internal JPG/JPEG/PNG/WebP/GIF pages should use a ShelfView endpoint because the existing `/api/v1/books/{bookId}/pages/{page}` cannot address mixed item/page pairs.
- Internal PDF items require a bounded single-entry extraction cache before calling the existing PDF analyzer/rendering path.

SV-453:

- Add the mixed Manifest DTO and frontend model.
- Add `MixedContentAdapter`, item navigation, item-aware locators, bookmarks and last-position restore.
- Add regression tests proving existing image-only ZIP/CBZ manifests still use `FIXED_PAGE_SEQUENCE`.

## Consequences

Positive:

- One ZIP stays one book in the library, matching user expectations.
- Existing image-only ZIP/CBZ behavior remains the fast path.
- TXT, image, GIF and PDF entries can be read in a stable archive order.
- No browser-side ZIP extraction is required.
- PDF.js remains deferred.

Negative:

- Manifest, Locator and adapter complexity increases.
- Internal PDF requires a bounded cache/extraction path.
- Mixed archives need more validation than current image-only ZIP analysis.
- Existing `MediaFile.SubType` is EPUB-oriented and may need ShelfView-specific metadata or a new DTO rather than broadening core metadata too early.

## Rejected alternatives

- **Split ZIP entries into separate Tales books.** Rejected because one archive would fragment into many books, create awkward series ordering, duplicate permission/progress state and surprise users.
- **Send the ZIP to the browser and use zip.js.** Rejected for the server viewer because large archives would be downloaded in full and client extraction would bypass server-side validation. `zip.js` remains only for future local-file/open-offline features.
- **Treat every mixed ZIP as a fixed-page image archive.** Rejected because TXT and PDF entries would be ignored or forced into fake pages.
- **Extract the entire ZIP to a server directory.** Rejected because it increases disk usage, cleanup risk and attack surface. Only bounded per-entry derived extraction is allowed, and only when a downstream renderer such as the PDF path needs a file.
- **Introduce PDF.js for internal PDFs now.** Rejected for MVP. Internal PDFs reuse the existing page-image PDF path.

## Test fixture list

Add or generate small fixtures during SV-451/SV-452:

- `mixed-txt-jpg-gif.zip`: `001.txt`, `002.jpg`, `003.gif`.
- `mixed-pdf-jpg.zip`: `001.pdf`, `002.jpg`.
- `nested-folder-mixed.zip`: content under one harmless folder.
- `natural-sort-mixed.zip`: `1.txt`, `2.jpg`, `10.jpg`.
- `traversal.zip`: `../evil.txt` rejected.
- `absolute-path.zip`: `/evil.txt` or `C:\evil.txt` rejected.
- `duplicate-normalized-name.zip`: duplicate after path normalization rejected.
- `nested-archive.zip`: inner `.zip` ignored or rejected as readable content.
- `oversized-text.zip`: TXT over byte or line limit rejected.
- `oversized-image.zip`: image over pixel limit rejected.
- `oversized-pdf.zip`: PDF over byte/page limit rejected.
- `corrupt-mixed.zip`: clear user-facing error.

## Revisit triggers

- Existing PDF rendering cannot safely consume a bounded cached internal PDF file.
- GIF animation causes memory or browser stability issues.
- Real user archives require EPUB, HTML or nested archive support inside ZIP.
- Mixed ZIP navigation becomes too complex for a single reader shell and needs a separate table-of-contents design.
