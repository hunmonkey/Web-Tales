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
- `MIXED_CONTENT_SEQUENCE`: ZIP archives whose internal readable items mix TXT, PDF and image entries
- `EPUB_WEBPUB`: EPUB bridge to Komga's existing WebPub manifest and reader stack

The UI uses adapters:

- `FixedPageAdapter`
- `TextReflowAdapter`
- `MixedContentAdapter`
- planned `EpubWebPubAdapter`

The UI must not scatter file-extension checks across components.

Manifests can optionally expose a structured `contents` list. Each item has a
display label and a versioned ShelfView locator, so the reader can show
format-provided tables of contents before falling back to mixed-item or TXT
paragraph-derived contents.

## Location model

- Fixed pages: page index + offset + fingerprint
- TXT: paragraph + character offset + progression + normalization version + fingerprint
- Mixed ZIP: book-level fingerprint + opaque item id/index + nested fixed-page or TXT locator
- EPUB: versioned wrapper around a bounded Readium/WebPub locator
- Page indexes are zero-based internally and one-based only in presentation.

## Data

- Preserve existing Komga progress compatibility for fixed pages.
- Fixed-page ShelfView `PAGE` saves mirror into Komga `READ_PROGRESS` with Komga's one-based page number.
- If no newer ShelfView position exists, the ShelfView position endpoint can expose existing Komga `READ_PROGRESS` as a zero-based ShelfView `PAGE` locator.
- TXT and mixed ZIP locators remain ShelfView-only because Komga `READ_PROGRESS` cannot represent paragraph offsets or internal ZIP item identity.
- EPUB initially preserves Komga progression compatibility through the existing Readium progression path; the ShelfView EPUB locator wrapper is available for Manifest contents, while EPUB bookmarks or extended positions remain future work.
- ZIP files that contain TXT or PDF entries are excluded from fixed-page Komga progress fallback; image-only ZIP/CBZ, PDF and image-directory books keep fixed-page compatibility.
- Add separate ShelfView storage for extended positions, multiple bookmarks and scoped preferences.
- Dedicated highlights are separate ShelfView annotation state, not bookmark
  labels. [ADR-0005](ADR/ADR-0005-dedicated-highlight-model.md) defines the
  selector JSON policy; SV-930m adds the `SHELFVIEW_HIGHLIGHT` storage
  migration and repository layer, and SV-930n adds the authenticated highlight
  API with selector validation. SV-930o adds frontend DTOs and service methods
  for that API. SV-930p connects the TXT reader UI to the dedicated API for
  selected range highlights while keeping older bookmark-backed paragraph
  highlights visible as legacy annotations.
- Built-in dictionary data must follow
  [ADR-0006](ADR/ADR-0006-dictionary-data-source-policy.md). Third-party
  dictionary content is not bundled into the default app until the owner accepts
  the data source, license obligations and notice/update plan. Future
  dictionary packs or configured providers should keep source/license metadata
  separate from the application code. SV-930s adds only a frontend dictionary
  provider result shell and keeps the existing explicit web lookup as the
  fallback provider. SV-930t adds local provider result plumbing and safe text
  rendering for future owner-approved dictionary packs, but still ships no
  third-party dictionary data. SV-930u adds a bounded frontend JSON dictionary
  pack parser requiring source/license metadata; it does not add persistence,
  upload, external provider calls or bundled dictionary content. SV-930w adds a
  session-only frontend import button that lets a user load a local JSON pack
  into memory for the current reader session. SV-930y can remember that
  user-provided JSON pack in browser `localStorage`; this is not server-side
  storage, account sync, media write access or bundled dictionary content.
  SV-930 closes the current reading-aids phase at this browser-local dictionary
  scope. Server/account-synced dictionary provider storage remains deferred
  until the owner approves the storage, attribution and redistribution policy.
- Never edit existing Flyway migrations.

### ShelfView storage recovery

SV-300 uses a forward-only Flyway migration. If a restore is needed before the ShelfView state APIs are in use, stop the app and restore the SQLite database under `/config` from backup, or drop only the three `SHELFVIEW_*` tables added by the migration when losing ShelfView-only reader state is acceptable. Existing Komga read progress remains in `READ_PROGRESS`.

## Feature rollout

A new reader route is introduced behind a feature flag. The existing reader remains available as fallback until compatibility and recovery have been validated.

## Bookshelf experience

SV-940a adds a small Tales badge to ready book cards when the ShelfView reader
feature flag is enabled and the media profile is handled by the new reader
route. The badge is only a bookshelf hint; it does not change authorization,
library queries, collection membership, sorting, filtering, server API
contracts, stored state or media files. EPUB remains on the existing EPUB route
and does not receive this badge in the first bookshelf slice.

SV-940b adds a small in-progress continuation chip to non-deleted, non-oneshot
series cards when `booksInProgressCount` is greater than zero. The chip links to
the existing series page with the existing in-progress read-status filter; it
does not introduce a new recommendation API, ranking model, collection query or
server-side continuation endpoint.

SV-940 closes at these two display/navigation improvements. Richer bookshelf
features such as personalized recommendation ranking, custom shelf rows, server
continuation APIs or collection analytics remain future product decisions rather
than implicit Phase 4 work.

## Image-quality expansion

SV-950a adds a `FADED_SCAN` fixed-page image filter preset for low-contrast or
faded scans. It reuses the existing content-only CSS filter pipeline and the
existing fixed-page preference storage contract. The preset changes only the
browser display of page content; it does not rewrite media files, create derived
images, introduce AI upscaling, add dependencies, or change page URLs.

[ADR-0007](ADR/ADR-0007-image-quality-processing-scope.md) accepts that
decision for planning: SV-950 stays on reversible CSS display presets, while
true per-pixel WebGL/Canvas/AI/derived-image processing needs a separate
owner-approved ticket with fallback, cancellation, memory bounds, browser
evidence and dependency/license review.

SV-951 adds the first owner-approved Canvas prototype as `CANVAS_SHARPEN`. It is
browser-local, dependency-free and fixed-page only. The reader draws the current
page image into a bounded `<canvas>`, applies a simple sharpen kernel, and falls
back to the normal `<img>` path if Canvas loading or pixel access fails. It is
disabled for continuous scroll so the app does not process many pages at once.
Further WebGL sharpening, background cleanup, adaptive monochrome, caching or AI
upscaling remain deferred until the remaining ADR-0007 gates are intentionally
met.

## Reader recovery UX

- ShelfView reader errors are grouped into actionable states for missing content, forbidden access, payload limits, network interruptions, unsupported schema and generic unsupported media.
- Page image failures show a retry path that reloads the image element without reopening the whole reader.
- Manifest/text load failures show a full reader retry path.
- TXT decoding failures can retry with manual UTF-8, EUC-KR or automatic decoding.
- The existing Komga reader remains available from the error state as a fallback route.

## Reader responsive recovery

- The reader listens for browser resize and mobile orientation changes.
- For TXT and mixed TXT content, the reader restores the current paragraph and character-offset scroll position after the display size changes.
- For continuous fixed-page and mixed image/PDF content, the reader keeps the current page scrolled into view after the display size changes.
- These checks are covered at the DOM/unit-test level; SV-602 requires desktop browser validation before release.
- Mobile and iPhone/iOS-family validation was removed from the SV-602 completion gate by owner direction on 2026-06-19.

## ZIP internal content

Planned ZIP expansion is tracked as SV-450 through SV-453. In addition to the current fixed-page ZIP/CBZ image flow, ShelfView should support ZIP files that contain TXT, PDF, JPG and GIF entries.

- Do not send the whole archive to the browser for automatic extraction.
- Reuse server-side archive inspection and page/content delivery patterns where possible.
- Keep source archives read-only and do not expose raw server paths or unsafe internal entry names.
- Validate archive traversal, nested entries, entry count, decompressed size, line length, image dimensions and internal PDF bounds before serving content.
- ADR-0003 chooses a single-Book mixed content model: the ZIP remains one Komga book, and the ShelfView manifest exposes ordered internal items behind opaque item ids.
- Mixed ZIP support adds a `MIXED_CONTENT_SEQUENCE` manifest shape and a `MixedContentAdapter` that coordinates text and fixed-page item adapters.
- GIF animation is preserved by default when the original GIF passes validation; internal PDF entries reuse the existing PDF page-image path through a bounded derived cache instead of PDF.js.

SV-451 adds the first mixed ZIP content endpoint:

- `GET /api/shelfview/v1/books/{bookId}/mixed/{itemId}/text` returns the same plain-text paragraph contract as the root TXT endpoint for a ZIP-internal TXT item.
- `itemId` is an opaque, stable id derived from the book content fingerprint and normalized entry metadata; raw archive paths are not used as API ids.
- The ZIP text inspector rejects unsafe entry paths, duplicate normalized entry names, nested archive entries, excessive entry count and over-limit TXT entries before returning content.
- ZIP-internal TXT reuses the same UTF-8 BOM, strict UTF-8, strict EUC-KR, manual encoding, byte limit and line-length rules as standalone TXT.

SV-452 adds the first mixed ZIP fixed-page endpoint:

- `GET /api/shelfview/v1/books/{bookId}/mixed/{itemId}/pages/{pageNumber}` returns ZIP-internal image/GIF bytes or a rendered page image for a ZIP-internal PDF item.
- ZIP-internal JPG/JPEG/PNG/WebP/GIF entries are served only after extension allow-listing, MIME sniffing and image dimension/pixel limits pass.
- GIF page delivery preserves the original GIF bytes when validation passes.
- ZIP-internal PDF entries are copied one bounded entry at a time into a temporary derived cache, checked as `application/pdf`, checked against the configured page-count limit, then rendered through the existing PDF page-image extractor.
- The endpoint keeps the archive source read-only, rejects unsafe entry paths and nested archives through the same mixed ZIP inspector, and does not expose raw server paths or raw archive paths as API identifiers.

SV-453 adds mixed ZIP manifest and reader routing:

- Mixed ZIP manifests expose ordered `items` with opaque `itemId` values, sanitized labels, item-level content fingerprints and either a text endpoint or page endpoint template.
- Internal TXT items use `TEXT_REFLOW`; internal PDF/JPG/PNG/WebP/GIF items use `FIXED_PAGE_SEQUENCE`.
- The frontend `MixedContentAdapter` opens the ordered items and reuses the existing text and fixed-page behavior instead of putting extension checks into the reader component.
- Mixed locators wrap the current item id/index and the nested text or page locator, so last-position and bookmark APIs can persist mixed ZIP reading state.
- Mixed ZIP manifests also populate Manifest-level `contents` entries from the
  ordered internal item labels. Each contents entry points to the first
  PAGE/TEXT location inside the mixed item, so the generic contents drawer can
  use the same contract as future PDF/EPUB TOC extraction.
- Existing image-only ZIP/CBZ books keep the fixed-page manifest path.
- Cover-candidate exposure is not part of the current manifest contract and remains future UX/detail work.

## Image directories

MVP requires a `.shelfview-book` marker in the leaf folder. Automatic guessing is out of scope.

SV-500 adds the first image-directory gate:

- Libraries expose `scanImageDirectory`, defaulting to `false`, so existing libraries do not start treating ordinary folders as books.
- When the flag is enabled, the filesystem scanner recognizes only folders containing a regular `.shelfview-book` marker file as ShelfView image-directory book candidates.
- A marked folder is returned as one book under its parent series and the scanner does not descend into that folder as a separate series tree.
- Unmarked folders, hidden folders and excluded directory patterns keep their existing scan behavior.

SV-501 adds image-directory analysis:

- Marked image-directory books use internal media type `application/vnd.shelfview.image-directory` and media profile `IMAGE_DIRECTORY`.
- Direct child files with `jpg`, `jpeg`, `png`, `webp` or `gif` extensions become pages only when MIME detection also identifies them as images.
- Pages are naturally sorted by filename, so `2.jpg` comes before `10.jpg`.
- When dimension analysis is enabled, page dimensions and file sizes are stored in `Media.pages`.
- Non-page direct child files are kept in `Media.files`; image-extension files rejected by MIME validation are recorded in the media comment with `ERR_1007`.
- Directory hashes are computed from direct regular file names, sizes and content hashes, so image-directory books can still get a stable file fingerprint.

SV-502 adds image-directory page delivery:

- Image-directory manifests now use `FIXED_PAGE_SEQUENCE` with source format `IMAGE_DIRECTORY`.
- The page endpoint template is `/api/shelfview/v1/books/{bookId}/image-directory/pages/{page}?zero_based=true`.
- Page delivery reads only direct child files recorded in `Media.pages`; blank names, path separators, hidden names, nested paths and symbolic-link page files are rejected.
- Page bytes are revalidated on delivery with the same image extension/MIME and dimension/pixel limits used by ShelfView archive image pages.
- Responses use inline image content disposition, the detected image content type and Komga private `Last-Modified` revalidation headers.
- Byte-range responses are not implemented for image-directory pages; images are returned as bounded whole-page byte responses, matching the current ShelfView fixed-page reader needs.
- Source image folders remain read-only.

SV-503 adds image-directory change lifecycle handling:

- The scanner represents a marked image-directory book with aggregate size from direct non-marker, non-hidden regular child files.
- The scanner uses the newest timestamp among the directory and its direct non-marker, non-hidden regular child files as the image-directory book modified time.
- Symbolic-link marker files are not accepted as `.shelfview-book`; symbolic-link child files are excluded from image-directory state.
- Library rescans now treat either modified-time changes or file-size changes as on-disk book changes, so image-directory page add/delete/rename/content-size changes mark existing media outdated even when the parent series timestamp is unchanged.
- Removing the `.shelfview-book` marker makes the folder stop scanning as a book, so the previous image-directory book follows the normal missing-book soft-delete path.
- Moving or renaming a marked image-directory book uses the existing deleted-book restore path when aggregate size and directory hash match.

## PDF

MVP renders through the existing page delivery path. PDF.js is a later, ADR-backed phase for text layer, search, links and direct PDF rendering.

PDF fixed-page manifests can populate Manifest-level `contents` from the PDF
document outline. Outline targets are converted server-side into zero-based
ShelfView PAGE locators; raw PDF destinations and file paths are not returned
to the browser. If outline parsing fails, the PDF still opens without
structured contents.

## EPUB

EPUB integration is planned by [ADR-0004](ADR/ADR-0004-epub-integration.md).
The first integration path is a WebPub bridge, not a renderer rewrite.

- Komga's existing EPUB scanner, analyzer, WebPub manifest, resource endpoints,
  TOC/page-list/position extraction and `@d-i-t-a/reader` rendering remain the
  source of truth.
- ShelfView exposes a distinct `EPUB_WEBPUB` manifest kind with an `epub`
  block that points to the existing authorized WebPub manifest and positions
  endpoints.
- EPUB bridge manifests can populate Manifest-level `contents` from Komga's
  stored EPUB TOC entries. Each contents entry contains a bounded EPUB locator
  with `href`, optional title and content fingerprint; raw server file paths are
  not returned.
- A future `EpubWebPubAdapter` should keep Readium/WebPub details out of the
  generic ShelfView reader shell.
- Existing `read-epub` and legacy fixed-page EPUB routes remain fallback paths
  until the ShelfView EPUB bridge has targeted tests and browser validation.
- No PDF.js, new EPUB parser, browser-side EPUB extraction or new rendering
  dependency is part of the first EPUB integration slice.

## TXT

TXT uses a hybrid model decided in [ADR-0002](ADR/ADR-0002-txt-scanning-and-domain-model.md): native scan and identity, isolated analysis and rendering.

- A `.txt` is registered natively as `MediaType.TXT` / `MediaProfile.TXT` so it reuses Komga scanning, identity, permissions and progress.
- Scanning is gated by a new `scanTxt` library option (default off), with a forward-only Flyway migration adding `LIBRARY.SCAN_TXT`.
- The native analysis branch is thin: it records minimal metadata (size, encoding candidate, fingerprint) and does not parse pages.
- All decoding, normalization, pagination, escaping and the paragraph/character Locator live in the ShelfView namespace behind `/api/shelfview/v1/.../text` and the frontend `TextReflowAdapter`.
- Encoding is auto → utf-8 → euc-kr candidate, overridable and saved per book; `normalizationVersion` versions normalization so saved positions stay restorable.

SV-402 adds the first text-content contract:

- `GET /api/shelfview/v1/books/{bookId}/text` returns schema version, book id, content fingerprint, selected encoding, normalization version and plain-text paragraphs.
- The endpoint accepts `encoding=utf-8` or `encoding=euc-kr`; without that parameter it uses UTF-8 BOM, strict UTF-8, then strict EUC-KR fallback.
- The endpoint enforces configurable `shelfview.text.max-bytes` and `shelfview.text.max-line-length` limits before returning content.
- The response is JSON plain text, not HTML. Frontend code must render paragraphs as text nodes and must not use `v-html`.

SV-403 adds the first frontend TXT reader:

- `ShelfViewTextService` loads the manifest-provided text endpoint instead of letting Vue components call Axios directly.
- `TextReflowAdapter` opens only `TEXT_REFLOW` manifests, validates fingerprint and normalization version, and exposes paragraph locators.
- `ShelfViewReaderShell` renders TXT paragraphs with normal text interpolation, supports single-paragraph and continuous-scroll layouts, and offers paper/dark/OLED text themes.
- Fixed-page-only controls such as fit, direction and image filters stay hidden for TXT books.

SV-404 stabilizes the first TXT Locator behavior:

- `TextReflowAdapter` stores both `paragraph` and `characterOffset`, clamps too-large restored offsets to the current paragraph end, and rejects malformed negative or non-integer text locators.
- Text progression is calculated from paragraph index plus character offset, so restoring a long paragraph is more precise than a page-like paragraph-only position.
- `ShelfViewReaderShell` updates `characterOffset` from the visible scroll position and restores the same offset after text layout or theme changes.
- The server rejects TXT locators whose `normalizationVersion` does not match the current normalization version and continues to reject stale `contentFingerprint` values.
