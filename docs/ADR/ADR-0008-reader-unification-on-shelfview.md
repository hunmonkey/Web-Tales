# ADR-0008: Reader unification on ShelfView

- Status: Accepted
- Date: 2026-06-22
- Ticket: SV-961
- Supersedes: none
- Related: `docs/ARCHITECTURE.md`, `docs/BACKLOG.md`, `docs/SECURITY_AND_LICENSE.md`, `0622.txt`

## Context

Web-Tales currently has two user-facing reader routes for comic-style fixed
page content:

- legacy Divina reader: `/book/:id/read`
- ShelfView reader: `/read-v2/:id`

Most in-app entry points already use `getBookReadRouteFromMedia()`. When the
ShelfView reader feature flag is enabled and the media is not EPUB, that helper
routes to `read-book-v2`. This keeps normal bookshelf and detail-page reading
entry points on ShelfView.

However, old bookmarks, direct legacy URLs, explicit fallback buttons and
legacy-reader next/previous navigation can still enter the legacy reader. That
can make one series switch between reader experiences while moving from one
volume to another.

ADR-0001 keeps the old reader as a fallback until the ShelfView reader is
validated. This decision must therefore unify default user-facing fixed-page
reading on ShelfView without deleting the legacy reader.

## Decision

Use ShelfView `read-v2` as the default user-facing reader for supported
non-EPUB media when the ShelfView reader feature flag is enabled.

When `DivinaReader` loads a book from the legacy route, it checks
`getBookReadRouteFromMedia(book.media)`. If the helper returns `read-book-v2`,
the component replaces the current route with `read-book-v2` and preserves the
existing query parameters.

Keep a `forceLegacy=true` query escape hatch for the explicit fallback path.
ShelfView's existing legacy-reader fallback links add this query value. When
`DivinaReader` sees `forceLegacy=true`, it stays on the legacy reader and does
not redirect back to ShelfView.

EPUB remains on the existing EPUB or EPUB-compatible legacy reader routes
because ShelfView does not replace EPUB rendering in this slice.

## Consequences

Positive:

- Direct legacy fixed-page URLs converge on the same ShelfView reader as normal
  in-app entry points.
- Series next/previous navigation no longer changes reader type for supported
  non-EPUB books when the ShelfView flag is enabled.
- The explicit fallback button still works and cannot loop back to ShelfView.
- No backend API, database migration, media write path or dependency change is
  required.

Negative:

- Old legacy URLs for fixed-page books no longer stay on the legacy reader by
  default while the ShelfView feature flag is enabled.
- Manual troubleshooting must use the explicit fallback link or add
  `forceLegacy=true` to the URL.

## Rejected alternatives

- **Delete the legacy reader.** Rejected because ADR-0001 and the project
  architecture keep the existing reader as a fallback.
- **Add a global router guard.** Rejected for this slice because the decision
  needs loaded book media data, and the existing reader component already has
  the required authorized book fetch.
- **Change every caller separately.** Rejected because most callers already use
  `getBookReadRouteFromMedia()`. The remaining problem is direct legacy entry,
  so the legacy reader entry point is the narrowest place to converge it.

## Revisit triggers

- ShelfView becomes the only supported reader and the legacy fallback is
  removed by owner decision.
- EPUB is intentionally bridged into ShelfView as a default reader path.
- The feature flag is removed or replaced by a server-side rollout policy.
