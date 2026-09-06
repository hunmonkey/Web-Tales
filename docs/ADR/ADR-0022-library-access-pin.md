# ADR-0022: PIN-locked libraries enforced server-side per session

- Status: Accepted
- Date: 2026-08-30
- Ticket: SV-1290
- Related: `docs/BACKLOG.md`, `docs/AI_PROGRESS.md`,
  `tales/src/main/kotlin/org/tales/domain/model/TalesUser.kt`,
  `tales/src/main/kotlin/org/tales/shelfview/interfaces/api/ShelfViewLibraryLockFilter.kt`

## Context

The owner wants certain libraries to require a password: the library's name may
stay visible with a small lock mark, but what is inside must be unreadable —
not listed, not searchable, not readable — until the password is entered, and
the unlock should hold until logout. The lock must bind every account,
including admins, because the owner is the admin.

Tales already has a complete per-user library filter: `TalesUser.
getAuthorizedLibraryIds` / `canAccessLibrary` feed every repository query —
browse, search, dashboard sections, referential lists, OPDS, thumbnails, page
streaming (82 call sites across 9 controllers, plus `SearchContext`). Anything
routed through a `TalesUser` whose authorized set excludes a library is already
invisible in all of those places. Two things stand in the way: the filter is
per-user and permanent, not per-session; and `canAccessAllLibraries()` contains
an admin bypass (`sharedAllLibraries || isAdmin`).

## Decision

**Store a BCrypt PIN hash per library in a new ShelfView table, keep the set of
unlocked library ids in the HTTP session, and enforce by swapping the request's
`TalesUser` for a copy whose authorized libraries exclude locked ones.**

- Storage: new table `SHELFVIEW_LIBRARY_ACCESS (LIBRARY_ID PK/FK cascade,
  PIN_HASH, …)`. The upstream `LIBRARY` table, `Library` model, `LibraryDao`
  and `LibraryDto` are untouched, preserving upstream mergeability. The hash
  never appears in any DTO.
- Session state: unlocked ids live in a session attribute. Logout or session
  expiry relocks everything, which is the requested lifetime. Requests without
  a session (OPDS clients, API keys) can never unlock, so locked libraries are
  simply absent there.
- Enforcement: `ShelfViewLibraryLockFilter`, a servlet filter running after
  Spring Security, computes `locked = pinned − unlocked(session)`. When
  non-empty it replaces the thread-local security context with one whose
  `TalesPrincipal` wraps `user.copy(sharedAllLibraries = false,
  sharedLibrariesIds = baseAccessible − locked, lockedLibraryIds = locked)`.
  The stored session context is not modified. `baseAccessible` is materialized
  from the library table for all-access users, because `null` means "no
  filtering" throughout the query layer and must not survive when locks exist.
- `TalesUser` changes by one field and one expression: `lockedLibraryIds`
  (default empty, never persisted) and `canAccessAllLibraries() =
  (sharedAllLibraries || isAdmin) && lockedLibraryIds.isEmpty()`. That single
  `&&` is what suspends the admin bypass; the filter is the only writer of the
  field and always sets the three fields coherently.
- API, all under `/api/shelfview/v1/library-locks`: list lock states for
  base-accessible libraries (id, name, unlocked — the name visibility the owner
  chose); `POST {id}/unlock` verifies the PIN; `DELETE {id}/unlock` relocks the
  session; `PUT {id}` (admin) sets the PIN and unlocks it for the setter's
  session; `DELETE {id}` (admin) removes the lock. Unlock returns 404 for a
  library the base user cannot access, so lock rows reveal nothing sharing
  rules would hide.
- Brute force: BCrypt verification (~100ms) plus a failure throttle keyed per
  user and library (amended by SV-1300; it was per library, which let one
  guesser lock everyone out and handed out 5 fresh tries every 30 seconds).
  After 5 consecutive failures the user waits 30 seconds, and every further
  failure doubles the wait up to one hour. In-memory, reset only on a correct
  PIN or a PIN change.

## Consequences

- One choke point covers every content surface at once; no per-controller
  auditing. The cost is that the swap must run before any handler reads the
  principal — a servlet filter ordered after the security chain does that for
  REST, OPDS and page streaming alike.
- The sidebar cannot learn locked libraries' names from `/api/v1/libraries`
  any more (the effective user hides them), so the web UI merges the
  lock-state list into the sidebar and shows those entries with a lock mark;
  clicking one asks for the PIN instead of navigating.
- User persistence is untouched by the swap: `/api/v2/users/me` and every
  user-mutating endpoint reload the user from the repository (or use only the
  id), so the effective user's narrowed sharing fields are never written back.
- SSE events still broadcast opaque ids (library/series/book ids, no titles)
  to locked sessions, exactly as upstream already does for share-restricted
  users. Accepted as a bounded, upstream-matching leak.
- A long-lived SSE connection or an in-flight request keeps the user state it
  started with; a relock applies from the next request on.
- Locked libraries drop out of OPDS entirely rather than showing a lock, since
  OPDS has no unlock flow.
- Reversible: removing all PINs (or the `DELETE {id}` endpoint per library)
  restores exact pre-change behaviour; dropping the table and the two
  `TalesUser` lines reverts the code path. No upstream data is migrated.

## Alternatives considered

- **Client-side lock only.** No server change, but the API keeps serving the
  content; anyone with the account can read everything with a URL. Rejected by
  the owner in favour of server enforcement.
- **Reuse per-user `sharedLibrariesIds` directly** (flip the user's sharing
  rows on unlock). No new enforcement code, but it rewrites persistent user
  configuration from a session event, breaks for admins entirely (the bypass),
  and two sessions of the same user would fight over one row.
- **Per-request lookup inside `TalesUser` accessors** (service locator or
  thread-local read from the domain model). Avoids the filter but hides I/O
  and request state inside a data class used by scans and background tasks,
  where "locked" has no meaning.
