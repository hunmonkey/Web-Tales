# ADR-0004: EPUB integration through the existing WebPub reader stack

- Status: Accepted for planning
- Date: 2026-06-20
- Ticket: SV-920 (Phase 2 EPUB integration ADR)
- Supersedes: none
- Related: [ADR-0001](ADR-0001-tales-fork-and-reader-strategy.md), `SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md` appendix B

## Context

The current ShelfView reader covers fixed-page books, TXT reflow, image-directory
books and mixed ZIP content. EPUB is still handled by Tales's existing EPUB
reader route or by the legacy fixed-page reader for Divina-compatible EPUB.

SV-920 is a design ticket before any EPUB implementation work. The master plan
kept "EPUB renderer replacement" outside the MVP and requires an ADR for new
renderer/dependency choices. The safe question for this ticket is therefore not
"replace EPUB now", but "how should EPUB join the Tales/ShelfView experience
without throwing away the proven Tales EPUB path?"

### Verified code map

Read directly in the current Tales 1.24.4 fork:

1. `tales-webui/src/functions/book-format.ts`
   - `getBookReadRouteFromMedia()` routes non-EPUB books to `read-book-v2`
     when the ShelfView reader flag is enabled.
   - EPUB is intentionally excluded from that route.
   - Legacy EPUB routing sends Divina-compatible EPUB to `read-book` and other
     EPUB to `read-epub`.
2. `tales-webui/src/views/EpubReader.vue`
   - Uses `@d-i-t-a/reader` to load the existing Tales WebPub manifest.
   - Loads `bookManifestUrl(bookId)` and `bookPositionsUrl(bookId)`.
   - Saves progress through `createR2Progression(location)` and Tales's normal
     book progression API.
   - Stores EPUB display settings under `persistedState.epubreader`.
3. `tales/src/main/kotlin/org/tales/domain/service/BookAnalyzer.kt`
   - `analyzeEpub()` extracts EPUB resources, table of contents, landmarks,
     page list, positions, fixed-layout metadata and Divina-compatible pages.
   - `getFileContent()` serves EPUB resources from the original EPUB archive.
4. `tales/src/main/kotlin/org/tales/interfaces/api/CommonBookController.kt`
   and `WebPubGenerator.kt`
   - Provide the existing WebPub manifest and EPUB resource flow.
   - Check content restrictions before returning the EPUB manifest/resources.
   - Return EPUB resources with a restrictive content-security policy.
5. `tales/src/main/kotlin/org/tales/shelfview/interfaces/api/ShelfViewManifestController.kt`
   - Returns ShelfView manifests for TXT, fixed-page, image-directory and mixed
     ZIP books.
   - Explicitly rejects EPUB for the ShelfView fixed-page manifest today.
6. `tales/src/main/kotlin/org/tales/shelfview/interfaces/api/ShelfViewPositionController.kt`
   - Mirrors fixed-page ShelfView progress into Tales `READ_PROGRESS` only for
     compatible fixed-page profiles.
   - EPUB is currently excluded from that fixed-page mirror path.

## Decision

Adopt a **WebPub bridge, not an EPUB rewrite**.

1. Keep Tales's existing EPUB scan, analysis, WebPub manifest generation,
   resource serving and `@d-i-t-a/reader` rendering path as the EPUB source of
   truth.
2. Add a future ShelfView manifest bridge for EPUB instead of parsing EPUB into
   TXT paragraphs or fixed pages. The manifest should add a distinct kind,
   tentatively `EPUB_WEBPUB`, with an `epub` block pointing to the existing
   authorized WebPub manifest and positions endpoint.
3. Add a future frontend adapter, tentatively `EpubWebPubAdapter`, under
   `tales-webui/src/shelfview/adapters`. It should wrap the existing
   `@d-i-t-a/reader` flow and keep Readium/WebPub details out of generic
   ShelfView components.
4. Use a versioned EPUB locator wrapper when ShelfView-owned positions or
   bookmarks are needed. The nested payload should preserve the Readium locator
   (`href`, `locations`, total progression and optional title data) instead of
   trying to translate EPUB into page or paragraph locators.
5. Preserve Tales EPUB progression compatibility first. The first implementation
   should continue writing the existing Tales progression through the same
   `createR2Progression()` path. Mirroring EPUB progress into ShelfView's
   extended position table can be a later step once the EPUB locator wrapper is
   covered by tests.
6. Keep the existing `read-epub` and legacy fixed-page EPUB routes as fallback
   until the ShelfView EPUB path has targeted tests and browser validation.
   Routing EPUB to `read-book-v2` must be feature-flagged or otherwise easy to
   turn off during rollout.
7. Do not add PDF.js, a new EPUB parser, browser-side EPUB extraction, server
   extraction of full EPUB archives, or a new rendering dependency as part of
   the first EPUB integration slice.

### Tentative ShelfView manifest shape

Exact DTO names can change during implementation, but the shape should stay
close to:

```json
{
  "schemaVersion": 1,
  "bookId": "book-id",
  "title": "Book title",
  "kind": "EPUB_WEBPUB",
  "sourceFormat": "EPUB",
  "contentFingerprint": "tales-file-state:...",
  "epub": {
    "webpubManifestEndpoint": "/api/v1/books/book-id/manifest/epub",
    "positionsEndpoint": "/api/v1/books/book-id/positions"
  },
  "capabilities": {
    "search": false,
    "textSelection": true,
    "doublePage": false,
    "continuousScroll": true,
    "rtl": true,
    "imageFilters": false,
    "toc": true,
    "publisherStyles": true
  }
}
```

This is a bridge manifest: ShelfView owns routing, shell behavior and future
state integration, while Tales's WebPub endpoint still owns EPUB publication
data and resource delivery.

### Tentative EPUB locator wrapper

```json
{
  "schemaVersion": 1,
  "kind": "EPUB",
  "href": "chapter-01.xhtml",
  "locations": {
    "progression": 0.42,
    "totalProgression": 0.18,
    "position": 34
  },
  "title": "Chapter 1",
  "contentFingerprint": "tales-file-state:..."
}
```

The implementation should accept only a bounded, schema-checked subset of the
Readium locator shape. Do not store arbitrary reader objects.

## Security and license policy

- Continue to authenticate and authorize EPUB manifests and resources through
  the existing Tales controllers.
- Do not expose raw server file paths or raw archive internals.
- Keep source EPUB files read-only.
- Preserve the restrictive EPUB resource content-security policy.
- Do not render untrusted EPUB HTML with Vue `v-html`; EPUB content remains
  isolated inside the existing reader iframe/WebPub flow.
- Do not add a new EPUB rendering dependency without dependency and license
  review. Prefer the already present `@d-i-t-a/reader` dependency for the first
  integration.
- Do not copy GPL/AGPL EPUB reader code into the default codebase.

## Implementation hook points

Future tickets after this ADR should be small and reversible:

1. Add TypeScript/Kotlin manifest DTO support for `EPUB_WEBPUB` and an optional
   `epub` manifest block.
2. Update `ShelfViewManifestController` to return the EPUB bridge manifest for
   `MediaProfile.EPUB`, after checking the same ready/auth/content-restriction
   gates used by the current endpoint.
3. Add frontend model and service support for `EPUB_WEBPUB`.
4. Extract the reusable WebPub reader setup from `EpubReader.vue` into a
   ShelfView EPUB adapter or a focused ShelfView EPUB shell.
5. Route EPUB to the ShelfView path only behind a rollout flag, keeping
   `read-epub` and legacy fixed-page EPUB routes available.
6. Add tests proving:
   - EPUB bridge manifests are returned only for authorized, ready EPUB books.
   - Non-EPUB ShelfView manifests are unchanged.
   - EPUB resources continue to flow through existing Tales resource endpoints.
   - EPUB progress still writes Tales-compatible progression data.
   - Unsupported/malformed EPUBs still show a fallback path.

## Consequences

Positive:

- Reuses the EPUB support Tales already has: scanning, metadata, WebPub
  manifest, resources, TOC, positions and current reader engine.
- Avoids a large parser/renderer replacement.
- Keeps EPUB HTML isolated in the existing iframe/WebPub flow.
- Lets the Tales/ShelfView shell gradually gain EPUB routing, settings and
  future bookmark/highlight integration.
- Keeps the old reader path available during rollout.

Negative:

- EPUB remains different from TXT and fixed-page books internally because it is
  WebPub/Readium-based.
- A wrapper adapter is needed so generic ShelfView UI does not learn Readium
  details.
- ShelfView bookmarks/highlights for EPUB need a dedicated EPUB locator policy,
  not reuse of the existing PAGE/TEXT locators.
- There will be a temporary two-path period: existing `EpubReader.vue` fallback
  plus the new ShelfView EPUB bridge.

## Rejected alternatives

- **Rewrite EPUB as TXT reflow.** Rejected because EPUB is structured HTML with
  spine, resources, CSS, images, landmarks and navigation. Flattening it to TXT
  would lose too much and create poor position stability.
- **Convert EPUB to fixed pages.** Rejected because reflowable EPUB should adapt
  to user font, width and theme settings. Rendering everything as images would
  remove text selection and accessibility.
- **Replace `@d-i-t-a/reader` immediately.** Rejected because that is a new
  renderer/dependency decision with high regression and license-review cost.
- **Send EPUB files to the browser for local extraction.** Rejected because it
  bypasses server authorization/resource controls and downloads whole books.
- **Leave EPUB outside ShelfView forever.** Rejected because the Tales reader
  roadmap needs one consistent route/shell and future reading aids.

## Revisit triggers

- The existing `@d-i-t-a/reader` stack blocks required EPUB features such as
  stable highlights, search, accessibility fixes or browser support.
- EPUB resource CSP or iframe isolation prevents required UI integration.
- The owner decides to accept a new EPUB renderer dependency after license,
  maintenance and bundle-size review.
- Future reading-aid tickets require a common annotation model across EPUB, TXT
  and fixed-page formats.

## Open questions for future implementation

- Should the first rollout route all EPUB through the ShelfView bridge, or only
  non-Divina-compatible EPUB while Divina-compatible EPUB keeps the legacy
  fixed-page route until separate validation?
- Which EPUB settings should be unified with ShelfView TXT settings, and which
  should remain EPUB-specific?
- Should EPUB bookmarks be stored first in ShelfView's bookmark table, Tales's
  existing progression only, or both?
