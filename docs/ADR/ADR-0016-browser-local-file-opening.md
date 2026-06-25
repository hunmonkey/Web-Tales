# ADR-0016: Browser-local file opening

- Status: Accepted
- Date: 2026-06-25
- Ticket: deferred post-closure browser-local file opening with zip.js
- Related: `docs/BACKLOG.md`, `docs/ARCHITECTURE.md`,
  `docs/SECURITY_AND_LICENSE.md`, `docs/ADR/ADR-0003-zip-internal-mixed-content.md`

## Context

ShelfView already supports server-managed books through authenticated APIs. That
path must keep server-side archive validation, library permissions, reader state
and source-media read-only rules.

ADR-0003 rejected sending server library ZIP files to the browser and extracting
them there because that would download large archives in full and bypass the
server validation path. The rejected server-viewer approach is still rejected.

There is a separate owner-approved use case: a logged-in user chooses a file
from their current browser device and opens it without importing it into the
server library.

## Decision

Add a separate `/read-local` route for browser-local ZIP/CBZ image archives.

The route:

1. Uses the browser file picker. Files are not uploaded to the server.
2. Uses `@zip.js/zip.js` to read entries in the browser.
3. Supports ZIP/CBZ image pages with `jpg`, `jpeg`, `png`, `webp` and `gif`
   entries.
4. Creates temporary `blob:` URLs for visible pages and revokes them when the
   archive is cleared or the route is destroyed.
5. Does not save reading progress, bookmarks, dictionary state or local file
   metadata to the server.
6. Does not expose raw server paths because no server media path is involved.
7. Does not change the existing server-side ZIP, mixed-content or fixed-page
   reader APIs.

## Dependency Review

- Package: `@zip.js/zip.js`
- Version: `2.8.26` pinned in `tales-webui/package.json` and
  `tales-webui/package-lock.json`
- License: BSD-3-Clause
- Purpose: browser-local ZIP/CBZ entry listing and image extraction for files
  explicitly selected by the user
- Native binaries: none
- Network behavior: none added by this route
- Worker behavior: local reader calls zip.js with `useWebWorkers: false`
- Copied source: none
- Coverage: unit tests cover entry filtering, path safety, size limits, object
  URL cleanup and the local reader view wiring

## Validation Limits

The browser-local reader applies its own client-side limits before extracting
images:

1. Reject archives over 2000 entries.
2. Reject image-page sets over 1000 images.
3. Reject image entries over 80 MiB uncompressed.
4. Reject total selected image bytes over 512 MiB uncompressed.
5. Skip unsafe archive paths, including absolute paths, drive-letter paths and
   `.` or `..` segments.
6. Skip encrypted entries and non-image entries.
7. Do not extract nested archives in this slice.
8. Do not accept SVG as an image page because it can carry active content.

These client-side checks are not a replacement for the server library checks.
They only protect the local file-opening route.

## Consequences

Positive:

- Users can quickly inspect a local CBZ/ZIP without importing it.
- Server media libraries, permissions and source files are untouched.
- The package license is compatible with the existing dependency policy.
- The feature is isolated behind a dedicated route.

Negative:

- Local files are not synced across devices.
- Progress and bookmarks are not persisted.
- Large archives still consume browser memory while opened.
- TXT, PDF and nested archive local opening remain future work.

## Rejected Alternatives

- **Reuse the server `/read-v2/:bookId` route for local files.** Rejected
  because the existing route is built around authenticated server manifests,
  state APIs and page endpoints.
- **Upload the selected file to the server first.** Rejected because this ticket
  is local opening, not import/upload.
- **Use zip.js for server library archives.** Rejected by ADR-0003 and still out
  of scope.
- **Support local PDF/TXT in this first route.** Deferred because PDF text/link
  handling and TXT encoding both need separate validation paths.
