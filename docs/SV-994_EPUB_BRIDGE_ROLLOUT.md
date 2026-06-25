# SV-994: EPUB Bridge Rollout

Date: 2026-06-25

## Scope

SV-994 continues the ADR-0004 WebPub bridge path. EPUB rendering still belongs
to Tales's existing EPUB reader stack; this slice only prevents a direct
ShelfView EPUB bridge manifest from landing on an empty reader screen.

## Current Rollout State

1. Normal book-card routing still keeps EPUB on the existing reader routes:
   `read-epub` for standard EPUB and `read-book` for Divina-compatible EPUB.
2. When both `VUE_APP_SHELFVIEW_READER` and
   `VUE_APP_SHELFVIEW_EPUB_BRIDGE` are enabled, book-card and sibling routing
   can send EPUB books through `/read-v2/:bookId` first.
3. Direct `/read-v2/:bookId` entry for an `EPUB_WEBPUB` manifest now validates
   the bridge endpoints and shows a single action that opens the existing EPUB
   reader path.
4. ShelfView does not save EPUB positions in this slice. Tales's existing EPUB
   progression path remains the source of truth.

## Boundaries

- No new EPUB parser, PDF.js path, browser-side EPUB extraction or rendering
  dependency was added.
- No source EPUB file write path changed.
- EPUB routing to `read-book-v2` is opt-in behind
  `VUE_APP_SHELFVIEW_EPUB_BRIDGE`; the normal reader flag alone still keeps
  EPUB on existing reader routes.
- EPUB Manifest contents are intentionally not shown as an in-shell table of
  contents until a real ShelfView EPUB navigation adapter exists.

## Next Gate

Before enabling the EPUB bridge flag in production by default, run targeted
browser validation against real EPUB samples. The validation should cover normal
EPUB, Divina-compatible EPUB, access-denied books, missing bridge endpoints and
existing EPUB progression writes.
