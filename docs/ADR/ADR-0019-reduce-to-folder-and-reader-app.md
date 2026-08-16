# ADR-0019: Reduce Web-Tales to a Folder-and-Reader Application

- Status: Proposed — owner approval required before any SV-12xx implementation
- Date: 2026-08-16
- Tickets: SV-1200 … SV-1246
- Related: `docs/BACKLOG.md`, `docs/ARCHITECTURE.md`,
  `SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md`, `docs/AI_PROGRESS.md`,
  [ADR-0017](ADR-0017-remove-kobo-and-koreader-sync.md)

## Context

The owner asked for a full redesign of the e-book viewer application on
2026-08-16, with four decisions recorded in the same session:

1. **Folder = Library.** The existing path-backed Tales library is the "folder"
   concept. No new user-defined virtual folder model is introduced.
2. **Removal reaches the backend API**, not only the frontend screens.
3. **Search moves into the top of the side menu**; the app bar is removed.
4. **This slice is planning only.** No implementation starts until the owner
   approves the ticket list.

Four follow-up decisions were taken later the same day:

5. **"Keep reading" stays.** It becomes a side-menu entry of its own, so the
   menu has five entries rather than four. The rest of the dashboard — the
   configurable recommendation shelves — is still removed.
6. **Library pinning and reordering stay** in the new side menu.
7. **OPDS is removed entirely**, not just its collection and read-list feeds.
8. **Losing existing collections and read lists is accepted.** No export path is
   built; a database backup before the migration is the only safeguard.

The requested end state is narrow: a side menu with folders, user management,
preferences and sign-out; a main area that is empty until a side-menu item is
selected; and a viewer that keeps its full feature set. Everything else is to be
deleted.

The current application is much wider than that. It carries the upstream fork's
collections, read lists, book/read-list import, media management (analysis,
missing posters, duplicate files, duplicate pages), metrics, announcements,
release checks, historical events, authentication-activity views and a
dashboard — roughly 30 routes and 22 REST controllers.

ADR-0017 already established the precedent for this kind of deletion: an unused
upstream feature was removed completely, including its persistence, with the
upstream-merge cost accepted.

### Measured coupling

Removal cost is not uniform. A file-reference count over
`tales/src/main/kotlin/org/tales`:

| Feature | Kotlin files referencing it | Of which are OPDS | Removal character |
|---|---|---|---|
| Read lists (`ReadList`) | 58 | 2 | Deeply woven into the core: import, book siblings, thumbnails, tasks, search |
| Collections (`SeriesCollection`) | 23 | 2 | Woven: series detail, thumbnails, referential filters |
| Historical events | 12 | 0 | Cross-cutting audit trail; other services publish to it |
| Transient books (import) | 5 | 0 | Nearly self-contained |
| OPDS itself | 11 in `interfaces/api/opds` + 11 referencing | — | Self-contained enough to remove as one ticket |

Read lists in particular are not a leaf feature. `BookController` sibling
navigation, thumbnail handling and tasks all reference them. Removing them is a
multi-day change with real regression risk to book navigation, not a
delete-the-controller change.

**Removing OPDS first does not make that cheaper.** Only 2 of the 58 read-list
files and 2 of the 23 collection files are under `interfaces/api/opds`; the
coupling is 56 and 21 files of core code either way. The two removals are
independent, and OPDS is worth removing on its own terms — it takes an XML
message converter, a separate authentication entry point, ETag and MVC
configuration and a public feed surface with it.

## Decision

Reduce the application to folders + reader, in five ordered phases, with the
irreversible work last.

### Scope kept

- Libraries as "folders", including create / rename / delete from the side menu,
  and their existing pinning and reordering
- "Keep reading" as its own side-menu entry
- User management and preferences
- Authentication, authorization and per-user reading state
- Series and book browsing (the main area content for a selected folder)
- Search, relocated into the side menu
- The complete ShelfView reader: ZIP, CBZ, PDF, image directories, TXT, mixed
  ZIP, the EPUB bridge, and every existing viewer setting — fit mode, layout,
  rotation, image filters, PDF handling, page numbers and bookmarks
- The EPUB renderer (`EpubReader.vue` / `@d-i-t-a/reader`), which the ShelfView
  EPUB bridge hands off to and therefore cannot be deleted

### Scope removed

Frontend first (Phase A), backend after (Phase D):

- Collections and read lists
- Book import and read-list import
- Media management: analysis, missing posters, duplicate files, duplicate pages
- Metrics, announcements, release checks, historical events
- Authentication-activity views
- OPDS, completely — feeds, generator, XML converter, authentication entry point
- The dashboard's recommendation shelves and their per-user view configuration.
  The "keep reading" block inside that view survives as its own destination; the
  app no longer opens onto it, because the main area starts empty.

### Phase order

| Phase | Content | Reversible? |
|---|---|---|
| A | Side menu, empty main area, folder CRUD, merged preferences, frontend route deletion | Yes — `git revert` |
| B | Visual redesign: colour/icon reduction, list and reader chrome | Yes |
| C | Performance: bundle baseline, lazy routes, list virtualisation | Yes |
| D | Backend controller/service/schema removal | **No** — data loss |
| E | Reader regression verification and old-reader fallback decision | Yes |

Phase D does not start until Phase A has run in production and the owner has
confirmed the removed features are genuinely unwanted. Deleting the screens
first makes the features unreachable, which delivers most of the benefit at zero
irreversibility.

### Phase D constraints

- One feature area per ticket. Read lists are their own ticket and are done last
  among the removals, because of the 58-file coupling.
- Each removal ticket ends with a green backend test run, not only compilation.
- Schema removal is one forward-only Flyway migration per feature area. Existing
  migrations are never edited (`CLAUDE.md`).
- A database backup is taken before deploying any Phase D migration. This is the
  only safeguard against the accepted data loss, so it is a hard gate, not a
  recommendation.
- OPDS is removed as its own ticket, in whichever order is convenient — it is
  not a prerequisite for the read-list or collection removals.

## Consequences

### Breaking changes requiring explicit owner acceptance

- **Public API compatibility is broken.** `CLAUDE.md` states "기존 공개 API 호환성을
  유지한다". Phase D deliberately violates that rule, which is why this ADR must
  be approved before SV-1230 and later tickets run. Approving this ADR amends
  that rule for the listed endpoints only.
- **Existing user data is deleted.** Collections and read lists a user has built
  are dropped by the Phase D migrations. The owner accepted this on 2026-08-16
  and chose not to have an export path built, so a pre-migration database backup
  is the only way back.
- **Every OPDS client stops working.** Any e-reader or app subscribed to this
  server's catalogue loses access with no replacement; the web UI and the
  reader become the only way in.
- **Upstream Tales merges effectively end.** After Phase D, upstream changes to
  read lists, collections, import or media management arrive as delete/modify
  conflicts across 90+ files. The fork becomes a permanent divergence rather
  than a maintained downstream. Security fixes to the remaining shared code
  (auth, scanner, page streaming) must then be picked individually.

### Not affected

- Source media stays read-only.
- Reading positions, bookmarks, highlights and reader preferences are untouched.
- Existing Flyway migrations are not edited; only new ones are added.
- Git history retains the deleted code.

## Alternatives considered

- **Frontend removal only (Phases A–C, stop before D).** Rejected by the owner
  on 2026-08-16, but retained as the natural stopping point if Phase D's cost or
  risk proves higher than expected during SV-1230's dependency survey. Phase A
  alone already delivers the requested UI and most of the perceived speed gain,
  because the removed routes are lazily loaded chunks.
- **New user-defined virtual folders.** Rejected by the owner in favour of
  reusing libraries, which avoids a new table, a new API surface and a new
  permission model.
- **Deleting the old readers immediately.** Deferred to SV-1245. `EpubReader.vue`
  is a hard dependency of the ShelfView EPUB bridge; `DivinaReader.vue` is the
  documented fallback path in `ARCHITECTURE.md`. Removing either needs its own
  verification pass, not a line in a cleanup ticket.
- **Hiding menu entries only.** Rejected: it leaves the reachable routes and the
  full bundle, so it serves neither the removal goal nor the speed goal.
