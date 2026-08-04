# ShelfView Backlog

The detailed acceptance criteria are in `SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md`.
Work in order unless an ADR explicitly changes dependencies.

## Project closure

Owner decision on 2026-06-19: the implemented ShelfView/Web-Tales feature set is
enough for the current project. Remaining release-hardening checklist items are
closed as "not pursued for now" rather than treated as active development work.

## M0 Foundation

- [x] SV-000 Repository audit and code map
- [x] SV-001 Base tag, remotes, upstream sync policy, license preservation
- [x] SV-002 Local backend/frontend baseline and test results
- [x] SV-003 Feature flag, `/read-v2/:bookId` shell and old-reader fallback

## M1 Manifest and shell

- [x] SV-100 Versioned Manifest domain, DTO and one fixed-page response
- [x] SV-101 TypeScript Manifest model and API service
- [x] SV-102 Unified reader loading/error shell and one page display

## M2 Fixed page reader

- [x] SV-200 FixedPageAdapter
- [x] SV-201 Single/double/continuous/LTR/RTL/fit modes
- [x] SV-202 Content-only CSS filters and presets
- [x] SV-203 Keyboard, touch, focus and accessibility

## M3 Position, bookmarks and preferences

- [x] SV-300 New Flyway migration and repositories
- [x] SV-301 Extended position API
- [x] SV-302 Multiple bookmark API
- [x] SV-303 Scoped preference API
- [x] SV-304 Frontend restore, debounce save and bookmark/settings UI

## M4 TXT

- [x] SV-400 Scanner/domain design spike and ADR
- [x] SV-401 TXT scan and analysis
- [x] SV-402 TXT content and encoding API
- [x] SV-403 TextReflowAdapter and themes
- [x] SV-404 Stable paragraph/character Locator

## M4.5 ZIP internal mixed content

- [x] SV-450 ZIP internal TXT/PDF/JPG/GIF design spike and ADR
- [x] SV-451 ZIP internal TXT content and encoding integration
- [x] SV-452 ZIP internal PDF/JPG/GIF page delivery and validation
- [x] SV-453 Mixed ZIP manifest, reader routing and regression tests (cover-candidate exposure remains future UX/detail work)

## M5 Image-directory books

- [x] SV-500 Library setting and `.shelfview-book` domain rule
- [x] SV-501 Scan, natural sort, MIME/dimension validation and metadata
- [x] SV-502 Page delivery and cache integration
- [x] SV-503 Change, move, delete and marker lifecycle

## M6 Compatibility and UX

- [x] SV-600 Existing Tales progress compatibility
- [x] SV-601 Corrupt/missing/network/encoding recovery UX
- [x] SV-602 Desktop browser validation closed by owner; Chrome/Edge automation exists, Firefox manual gate is not pursued for now

## M7 Operations

- [x] SV-700 CI checks (workflow green on GitHub Actions: change-detection, backend tests, frontend lint/test/build, Docker image)
- [x] SV-701 Versioned multi-arch Docker image (render script + CI build; live image build validated on GitHub Actions)
- [x] SV-702 Unraid install, backup, upgrade and rollback guide

## M8 Release

- [x] SV-800 Threat model
- [x] SV-801 Performance baseline closed by owner; detailed measurements and owner targets are not pursued for now
- [x] SV-802 Dependency/license/notice audit
- [x] SV-803 v1.0 release candidate closed by owner; live Unraid build/deploy verification is the final acceptance point for now

## Deferred after closure

- [x] PDF.js text layer, search and document links closed by owner-declined
  reader/PDF recommendation items 7-8; ADR-0011 remains future planning only
  until explicit owner approval
- [x] WebGL sharpening, background cleanup and adaptive monochrome
- [x] Browser-local file opening with zip.js
- [x] Offline PWA
- [x] EPUB reader unification

## Reopened Quick Wins from `0620.txt`

Owner request on 2026-06-20: use `0620.txt` as a scoped improvement roadmap and
continue one `SV-xxx` ticket at a time.

- [x] SV-910 QW-1 fixed-page mouse wheel zoom and drag panning
- [x] SV-911 QW-2 fixed-page 90-degree rotation and `R` shortcut
- [x] SV-912 QW-3 TXT client-side search and jump
- [x] SV-913 QW-4 TXT reader brightness slider
- [x] SV-914 QW-5 fixed-page thumbnail filmstrip and direct page jump
- [x] SV-915 QW-6 bundled OFL Korean web fonts
- [x] SV-916 QW-7 double-page cover-alone toggle
- [x] SV-917 QW-8 fixed-page slideshow mode
- [x] SV-919 QW-10 fixed-page image enhancement filter preset

## 0620 phased roadmap

The immediate Quick Wins from `0620.txt` are complete. Larger roadmap phases
remain tracked as future work because they require component decomposition,
ADRs, dependency/license review, or broader product decisions.

- [x] SV-900 Phase 0 reader core decomposition
- [x] SV-901 Phase 0 viewport controller extraction
- [x] SV-902 Phase 0 toolbar and drawer component split
- [x] SV-920 Phase 2 EPUB integration ADR
- [x] SV-930 Phase 3 reading aids: TOC, highlight, memo, dictionary and TTS
  - [x] SV-930a Bookmark memo editing using the existing bookmark `note` field
  - [x] SV-930b Mixed-content TOC panel using Manifest item labels
  - [x] SV-930c Text paragraph read-aloud using browser speech synthesis
  - [x] SV-930d Bookmark-backed TXT paragraph highlighting
  - [x] SV-930e TXT paragraph contents panel
  - [x] SV-930f TXT heading-based contents extraction
  - [x] SV-930g Selection dictionary lookup handoff
  - [x] SV-930h Manifest-provided structured TOC support
  - [x] SV-930i Mixed ZIP Manifest contents backend
  - [x] SV-930j PDF outline Manifest contents backend
  - [x] SV-930k EPUB WebPub bridge Manifest and TOC contents backend
  - [x] SV-930l Dedicated highlight model ADR
  - [x] SV-930m Dedicated highlight storage migration and repository
  - [x] SV-930n Dedicated highlight API and selector validation
  - [x] SV-930o Dedicated highlight frontend service and model
  - [x] SV-930p TXT range highlight UI using the dedicated highlight API
  - [x] SV-930q Selected TXT text read-aloud using browser speech synthesis
  - [x] SV-930r Built-in dictionary data source and license ADR
  - [x] SV-930s Dictionary provider UI shell with web lookup fallback
  - [x] SV-930t Local dictionary provider result plumbing without bundled data
  - [x] SV-930u Dictionary pack JSON import parser without bundled data
  - [x] SV-930v TXT read-aloud speed control
  - [x] SV-930w Session-only dictionary pack import UI
  - [x] SV-930x TXT read-aloud browser voice selection
  - [x] SV-930y Browser-local dictionary pack persistence
  - [x] SV-930z Reading-aids browser-local scope closure and server-sync deferral
- [x] SV-940 Phase 4 Tales bookshelf and collection experience
  - [x] SV-940a Bookshelf Tales-reader readiness badge
  - [x] SV-940b Collection/series continuation surface
  - [x] SV-940c Bookshelf/collection polish review and closure
- [x] SV-950 Phase 5 format, performance and image-quality expansion
  - [x] SV-950a Faded-scan display boost preset
  - [x] SV-950b WebGL/Canvas image-quality spike decision
  - [x] SV-950c Phase 5 scope closure
- [x] SV-951 Owner-approved Canvas sharpen prototype

## Reopened reader unification from `0622.txt`

- [x] SV-961 Route supported fixed-page legacy reader entries into ShelfView
  read-v2 while preserving the explicit existing-reader fallback

## Reopened Honeyview-style reader simplification from `0623.txt`

- [x] SV-970 Immersive reader chrome state, guarded Escape toggle and center
  tap discovery
- [x] SV-975 Shrink-only default fixed-page fit when no saved preference exists
- [x] SV-972 Left/center/right tap-zone navigation with reading-direction rules
- [x] SV-973 Toolbar surface reduction and overflow menu
- [x] SV-974 Primary image-filter simplification
- [x] SV-971 Reader chrome idle auto-hide
- [x] SV-976 Text reflow immersive settings simplification
- [x] SV-977 Shortcut help update for the new input map
- [x] SV-978 Reader comfort follow-up: hidden page strip, show-only center tap
  and global keyboard paging

## Reopened nested image archive support from `0624.txt`

- [x] SV-980 One-level nested ZIP/CBZ image archive support for fixed-page ZIP
  books

## Reopened feature/UIUX investigation from 2026-06-25

Docs-only investigation. Pick one ticket at a time if the owner chooses to
implement any of these candidates.

- [x] SV-990 Remove orphaned account API-key and per-user UI settings frontend
  screens after owner confirmation
- [x] SV-991 Batch or summarize user latest-authentication activity for the
  Users screen instead of one request per listed user
- [x] SV-992 Metrics page polish: reduce per-library metric request fan-out,
  add visible empty/error states and decide whether read-progress metrics belong
  on the page
- [x] SV-993 Browser/device evidence pass for Canvas sharpen and future
  image-quality modes
- [x] SV-994 EPUB bridge rollout planning and first implementation slice
- [x] SV-995 PDF text layer, search and document-link exploration with ADR
- [x] SV-996 Archive expansion triage for nested RAR/7Z and mixed top-level
  TXT/PDF plus nested image chapters

## Deferred after SV-996 archive triage

- [x] SV-997 Mixed ZIP one-level nested ZIP/CBZ image chapters
- [x] SV-998 Nested RAR/CBR support spike and ADR
- [x] SV-999 7Z top-level support spike and ADR
- [x] SV-1000 Mixed folder-book model ADR for TXT/PDF plus image chapter
  archives

## Deferred after SV-930 closure

- [x] SV-1001 Server/account-synced dictionary provider storage ADR
- [x] Owner-approved server/account-synced dictionary provider storage
  implementation

## Deferred after SV-950 closure

- [x] Browser/device performance evidence for Canvas sharpen and future WebGL image-quality modes

## Reader improvement and defect sweep from 2026-07-25

Owner request: review the document/comic reader for feature improvements and
defects. The sweep found five items and the owner approved all of them.

- [x] SV-1010 Lazy page and thumbnail image loading so continuous scroll and the
  thumbnail strip stop requesting every page at once
- [x] SV-1011 Mixed ZIP page HTTP cache headers matching the other page APIs
- [x] SV-1012 Reset the failed-page-image state when paging inside mixed and text
  books so a load failure no longer keeps the error screen
- [x] SV-1013 Prefetch a full double-page spread ahead instead of one page
- [x] SV-1014 Restore a clean `tsc --noEmit` pass so the build type-check is a
  real safety net again

## UI/UX pass from 2026-07-25

Owner request after the reader defect sweep. Scored against the applicable parts
of `Rule_UIUX.txt` (1-4 accessibility, 1-5 Korean text, 6-1 plain wording).

- [x] SV-1020 Restore keyboard focus rings that `outline: none` removed, and stop
  focus from looking identical to hover or to the active row
- [x] SV-1021 Stop showing raw English server/browser/archive error text in the
  reader; local archive failures now carry a code the view translates
- [x] SV-1022 Fill the one missing Korean key (`common.image_directory`)
- [x] SV-1023 Add `aria-label` to every icon-only `v-btn` (142 buttons across 54
  files, including 4 in `ShelfViewLocalReader`)

## Codebase audit from 2026-07-25

Owner request after the UI/UX pass. Tests, lint, type-check, auth coverage,
resource handling and Flyway integrity were clean; two problems were found.

- [x] SV-1030 Raise the dependencies that had a non-breaking fix, most
  importantly `axios` (authentication bypass via `validateStatus` prototype
  pollution)
- [x] SV-1031 Repair the `import` ordering that the `org.komga` → `org.tales`
  debrand broke, so `ktlintCheck` passes again (257 files)

### Audit findings deliberately not fixed

- `vue` / `vuex` / `vuetify` advisories need a Vue 3 / Vuetify 3 migration,
  which this project forbids without explicit owner approval.
- `pdfjs-dist` (arbitrary JS execution from a malicious PDF) is pinned through
  `@d-i-t-a/reader`. ShelfView renders PDFs to images server-side, so the
  exposure is limited to the existing EPUB reader route.
- `brace-expansion` and its dependents (`minimatch`, `file-js`, `filehound`,
  `recursive-readdir`, `r2-utils-js`) have no non-breaking fix available.
- CI does not run ktlint (only `:tales:test` and `:tales:bootJar`), which is why
  SV-1031 stayed invisible. Adding it to CI is a separate decision.

### Known follow-up not implemented

- Mixed ZIP books have no thumbnail endpoint, so the thumbnail strip falls back
  to full-size page images (`shelfViewThumbnailUrl` only receives a thumbnail
  template for plain fixed-page books). SV-1010 lazy loading limits this to the
  visible strip, but a real mixed-item thumbnail endpoint would need a new API
  and an ADR.

## Visual identity and main shell refresh from 2026-07-25

Owner request: finish the existing uncommitted Tales visual identity and main
application shell refresh as one scoped ticket.

- [x] SV-1040 Refresh the Tales logo/install/tray assets, light and dark theme
  palette, login/startup/welcome screens, navigation shell, dashboard, empty
  state and book-card surfaces; refresh the offline cache key and cover the
  shared brand assets and primary read action with tests

## Full UI/UX sweep from 2026-07-26

Owner request: audit the whole interface, unify it, make it feel refined, focus
on the reader, and keep the interaction short. SV-1040 refreshed the application
shell but stopped at the reader, which still carries 66 hardcoded colours and a
Material Blue accent that does not match the Tales palette.

- [x] SV-1050 Unify the ShelfView reader with the application design language:
  token-driven surfaces, a minimal top bar (back, title, paging, fit,
  fullscreen, settings), and the unlabelled 20-icon overflow popover rebuilt as
  a labelled settings panel grouped into view / page / text / tools. No feature
  is removed and every existing `data-testid` is preserved, so the toolbar
  contract asserted by `ShelfViewReaderShell.spec.ts` keeps holding.
- [x] SV-1051 Unify icons across the app on the outline family where filled and
  outline variants of the same meaning are currently mixed, and move the
  remaining fallback readers (`DivinaReader`, `EpubReader`,
  `ShelfViewLocalReader`) onto the same tokens.

## Perceived-usability plan from 2026-07-26

Owner request after SV-1050/SV-1051 shipped: plan the next round of work on how
the app *feels* to use, not on how it looks. Recorded as a plan only; nothing
below is implemented. Each item lists the evidence that motivated it and the
check that would close it.

Ordered by how much each one shortens the path to reading, which is the stated
priority.

- [x] SV-1060 Show content-shaped placeholders while a shelf is loading.
  Evidence: `v-skeleton-loader` appears in zero components, and
  `DashboardView.vue` only renders a section when
  `section.loader.items.length !== 0`, so every dashboard row is invisible
  until its request resolves and the page then jumps. `ItemBrowser.vue` has no
  loading state at all. Close when a cold dashboard load shows card-shaped
  placeholders in the final layout positions and the content no longer shifts
  when it arrives.

- [x] SV-1061 Make "carry on where I left off" the first thing on the home
  screen. Evidence: the `KEEP_READING` section already exists
  (`types/tales-clientsettings.ts`, `DashboardView.vue`), and `ItemCard.vue`
  already renders a continuation chip, but reaching either still costs
  dashboard load, scroll, card hover and then the read action. Close when a
  returning reader can resume the most recent book from the first screen
  without scrolling, and when having nothing in progress degrades cleanly.

- [x] SV-1062 Return the reader to wherever the reader came from.
  Evidence: `closeBook()` in `ShelfViewReaderShell.vue` always pushes
  `browse-book` / `browse-oneshot`, so leaving a book opened from a library
  grid or a search result lands on the book detail page and costs another
  click to get back to the list. Close when leaving the reader returns to the
  originating list with its scroll position, while a direct URL visit still
  falls back to the book page.

- [x] SV-1063 Give the reader a thumb-reachable paging control on phones.
  Evidence: paging lives only in the top toolbar, which auto-hides after
  `CHROME_AUTO_HIDE_DELAY_MS`. Side-tap zones exist, so this is comfort rather
  than a blocker, and it must not cover the page. Close when paging and the
  page counter are reachable one-handed on a phone-sized viewport without
  reaching the top of the screen, and when the control hides with the rest of
  the chrome.

- [x] SV-1064 Add the few keyboard shortcuts that browsing lacks.
  Evidence: shortcut handling exists only in the readers
  (`DivinaReader`, `EpubReader`, `ShelfViewReaderShell`, `PagedReader`) and
  `ShortcutHelpDialog` is only opened from readers. Browsing has no focus-search
  or close-panel key. Close when `/` focuses search and `Esc` closes the open
  panel from any browse screen, the existing help dialog lists them, and no
  shortcut fires while typing in an input.

- [x] SV-1065 Fill the empty sections of `Rule_UIUX.txt` (1-1, 1-2, 1-6, 1-7,
  1-8, 3 and 7). Evidence: those headings have no body, so screen-level
  decisions have no written standard; SV-1050/SV-1051 had to write 1-9 and
  1-10 while working. This is a decision-capture ticket, not a code ticket, and
  it needs owner input rather than implementation.

## Reader chrome recovery, reported 2026-07-27

- [x] SV-1066 Make the auto-hidden reader chrome reachable again on a computer.
  Evidence: owner reported that the toolbar, the settings and the back button
  all disappear in `/read-v2` and nothing brings them back. `chromeVisible`
  starts `false` and `CHROME_AUTO_HIDE_DELAY_MS` was 2500, while the only two
  ways back were the 35–65% centre tap zone (`reader-input.ts`) and `Escape`;
  mouse movement did nothing, because `noteChromeActivity()` only extends the
  timer for chrome that is already visible. The centre click was also unreliable
  on fixed-page books: `onReaderPointerDown` opened a pan candidate even when the
  page already fitted the stage, and `shelfViewShouldStartPanning` treated 3px of
  ordinary click drift as a pan, which made `onReaderPointerUp` drop the tap.
  Owner chose to keep the auto-hide and strengthen recovery rather than pin the
  toolbar on desktop. Close when moving the mouse brings the chrome back and
  keeps it up while the pointer keeps moving, and when a centre click with
  normal hand drift always toggles the chrome on a page that fits.

## Duplicated Keep Reading on the dashboard, reported 2026-07-27

- [x] SV-1067 Show "Keep Reading" once on the dashboard. Evidence: owner
  reported the same book appearing twice on `/dashboard`, above and below each
  other. The SV-1061 resume banner and the `KEEP_READING` shelf are both driven
  by `loaderKeepReadingBooks`, and the banner is just `items[0]` of that shelf,
  so with one book in progress the dashboard showed it twice under the same
  title. Owner chose to keep the banner and drop the shelf, accepting that only
  the most recently read book is reachable from the dashboard. Close when the
  dashboard renders exactly one Keep Reading surface and the section switch in
  the recommended-view dialog still turns it off.

## Perceived slowness, reported 2026-07-27

- [x] SV-1068 Stop revalidating every image on every view. Evidence: owner
  reported the whole site feels slow, equally from inside and outside the
  house. Measured on the production host: container CPU 0.09%, 675MB of a 2GB
  heap, SQLite on the SSD pool reading at 1.2GB/s, a cold 20MB read from the
  book array in 20ms, a ZIP page extracted in 40ms, HTTP `/` answered inside
  the container in 2ms, and JS/CSS already Brotli-compressed and edge-cached by
  Cloudflare. The server is not the bottleneck. What remained is that every
  poster and page image was served `max-age=0, must-revalidate`, so each one
  costs a round trip on every render even when the browser already holds it.
  Two earlier suspicions were checked and dismissed: Vuetify's `v-img` already
  defers off-screen images, and `EtagFilterConfiguration` already gives `/api/*`
  an ETag, so nothing was being re-downloaded in full. Close when posters and
  page images are reused from the browser cache without a revalidation request,
  while the rest of the API still revalidates.

### Deliberately not planned

- Reworking navigation into a bottom bar on mobile. The drawer plus
  `LibraryNavigation`'s existing `v-bottom-navigation` already covers the
  common paths, and a second bottom surface would collide with SV-1063.
- Any Vue 3 / Vuetify 3 dependent behaviour. Still forbidden without explicit
  owner approval.

## Loading-state defect sweep from 2026-07-27

Owner asked for a bug fix and chose a codebase sweep over a reported symptom.
The sweep read the SV-1060..SV-1069 diff and found four defects, all in the
loading states those tickets introduced. The owner approved fixing all four.

- [x] SV-1070 Stop the dashboard from getting stuck in its loading state, and
  stop the resume banner from flickering and from showing a stale poster.
  Evidence, three defects in one file:
  1. `loadAll()` in `DashboardView.vue` runs
     `Promise.all([...]).then(() => this.loading = false)` with no `catch`.
     `PageLoader.loadNext()` rethrows whatever the API service threw, and
     `tales-books.service.ts` turns any failure into `throw new Error`. One
     failing loader out of seven leaves `loading` true forever, which pins the
     SV-1060 skeletons on screen, hides the SV-1061 resume banner permanently
     (`resumeBook` returns undefined while loading) and suppresses the
     `allEmpty && !loading` empty state. It also leaves an unhandled rejection.
     This only became visible when SV-1060/SV-1061 made `loading` drive the
     whole screen; before that it only gated the empty state.
  2. Reading a book and returning to the dashboard fires a read-progress SSE
     event, which calls the throttled `reload()` → `loadAll(true)` →
     `loading = true`. The shelves survive because they are gated on
     `items.length !== 0`, but the banner unmounts and remounts, so the page
     jumps up and back down. SV-1060 exists to remove exactly that shift.
  3. The banner builds its poster with `bookThumbnailUrl(id)` and no
     cache-busting query, unlike `ItemCard.vue`, which appends
     `thumbnailCacheBust` on every thumbnail SSE event. SV-1068 now serves
     posters `max-age=86400, private`, so a replaced cover stays stale in the
     banner for up to a day. The `WebMvcConfiguration` comment that justifies
     the poster cache assumes the web UI always cache-busts; the banner is the
     one place that does not.
  Close when a failing dashboard request still resolves the loading state and
  shows whatever loaded, when reloading an already-loaded dashboard leaves the
  banner in place, and when a thumbnail event refreshes the banner poster.

- [x] SV-1071 Stop the browse screens from getting stuck in their loading
  state. Evidence: `BrowseBooks.vue`, `BrowseSeries.vue` and
  `BrowseLibraries.vue` pass `:loading="totalElements === null"` to
  `ItemBrowser`, and `totalElements` is only ever assigned on the success path,
  so a failed load holds the skeletons on screen with no way out. Lower impact
  than SV-1070 because the pre-SV-1060 behaviour was an empty grid rather than
  a stuck one. Close when a failed load leaves the skeleton state and shows the
  existing empty slot.

## Reader toolbar and page stability, reported 2026-07-29

- [x] SV-1072 Pin the reader toolbar and stop the page from moving under it.
  Evidence: owner reported that the top menu never appears in the reader and
  that the page shifts up and down while reading. Two findings, one cause.
  First, `chromeVisible` started `false` on every `setup()`, the only ways in
  were the 35–65% centre tap, mouse movement and `Escape`, and
  `CHROME_AUTO_HIDE_DELAY_MS` took the toolbar away again after five seconds;
  the `shelfview_reader.chrome_hint` snackbar that taught the centre tap only
  ever showed once per browser (`shelfview.chromeHintSeen.v1`), so a reader who
  missed it had no way to learn the gesture. Second, the toolbar is already a
  `position: fixed` overlay, yet `.shelfview-reader-shell--chrome-visible` also
  pushed `.shelfview-reader-shell__stage` from `padding: 16px` to
  `padding-top: 56px` (and to `104px` with the search bar open, in both the
  desktop rules and the narrow-screen media query), so every auto show/hide
  cycle resized the stage by 40px and re-fitted the page. Owner chose a pinned
  toolbar over the auto-hide, and chose to let it overlay the page rather than
  reserve space for it, accepting that the top ~48px of a page sits under the
  translucent toolbar. Close when the toolbar is up as soon as a book opens and
  stays up until the reader hides it with `Escape` or a centre tap, and when
  showing or hiding it never changes the size or position of the page.

## Remove Kobo Sync and KOReader Sync, requested 2026-08-03

- [x] SV-1076 Delete both external device-sync integrations completely, with no
  leftover traces. Owner statement: neither feature is used, the server has no
  reason to reach anything outside itself, and the removal should leave nothing
  behind. See `docs/ADR/ADR-0017-remove-kobo-and-koreader-sync.md`.
  Scope covered: the `/kobo/**` and `/koreader/**` HTTP surfaces and their DTOs;
  the two extra security filter chains and the anonymous `/koreader/users/create`
  exception; the Kobo store reverse proxy and sync-token generator; sync points
  end to end (domain model, repository, lifecycle, jOOQ DAO, the
  `DELETE /api/v1/syncpoints/me` endpoint, and the frontend service and plugin);
  the `KOBO_SYNC` and `KOREADER_SYNC` roles; the KOReader partial-MD5 hash and
  its library option, book column and task; `koboSpan`, `isKepub`,
  `Media.epubIsKepub`, `KepubConverter` and the `kepubify` path setting together
  with the binary's Docker and Conveyor downloads; the `tales.kobo.*` properties;
  and the matching keys across all 38 locale files.
  One correction worth recording: `koboSpan` looked like shared reader
  infrastructure because it is produced inside `EpubExtractor.computePositions`,
  but position count and progression are always the Readium 1024-byte
  approximation and the span value is read only by the Kobo controller. Removing
  it changes no reader behaviour; keeping it would have meant running a full
  external EPUB conversion on every analysis to populate a field nothing reads.
  Closed when the sync endpoints no longer exist, the schema no longer carries
  the sync tables and columns, and a repository-wide search finds only the
  documented historical mentions.

## Reader feature and design sweep from 2026-08-04

Owner request: analyse the reader for feature and design improvements, then
implement priorities 1 through 4. The analysis ran five of six planned
dimensions (fixed-page, text reading, design system, input/accessibility,
performance); the reading-journey dimension and the automated verification pass
did not run, so twenty of the highest-severity claims were verified by hand
against the code instead. Six items were not implemented and are recorded in
`docs/ADR/ADR-0018-reader-sweep-deferred-decisions.md`.

### Defects hit during ordinary reading

- [x] SV-1080 Stop a touch drag on a zoomed page from turning the page, and
  ignore multi-touch gestures. The mouse path guarded this through
  `readerStageCanPan`; touch had no pan handler because the stage scrolls
  natively, so `shelfViewTouchAction` now takes the stage state.
- [x] SV-1081 Make a failed position save visible in the pinned toolbar instead
  of only inside the settings panel, and save through a keepalive fetch on
  `pagehide`/hidden `visibilitychange` so an unload does not cancel it.
- [x] SV-1082 Let Escape close the innermost open panel before it reaches the
  chrome (`closeTopmostChromePanel`).
- [x] SV-1083 Move wheel zoom onto Ctrl/Cmd so a width-fitted page can be
  scrolled, and decide tap paging from the real horizontal overflow rather than
  a scale threshold. Correction to the analysis: `fixedPageScale` is stored per
  book (`scopeType: 'BOOK'`), not globally, so it was left persistent.
- [x] SV-1084 Subtract the bottom dock and thumbnail strip reservations from the
  fit calculation via `--sv-stage-bottom-chrome`.
- [x] SV-1085 Map HTTP 401 to `UNAUTHENTICATED` and offer sign-in instead of a
  retry that can never succeed.
- [x] SV-1086 Narrow a page image failure to that page. `pageImageFailed` became
  a computed over `failedPageIndexes`, so a spread keeps its other half and
  continuous scroll keeps its place.
- [x] SV-1087 Give a TXT inside a mixed archive the same page budget as a
  standalone TXT (the ADR-0009 revisit trigger).
- [x] SV-1088 Carry read-aloud on to the next paragraph and the next page, and
  add pause/resume.
- [x] SV-1089 Add a settings button to the bottom dock and drop the toolbar
  labels under 600px, so the settings entry cannot scroll out of reach.
- [x] SV-1090 Drop `role="application"` for prose so a screen reader keeps browse
  mode, and translate the five hardcoded English screen-reader strings.

### Getting to the page faster

- [x] SV-1091 Fetch the book and the manifest together and load the series and
  next-book lookups in the background: four serial round trips became two.
- [x] SV-1092 Convert the reading fonts to woff2: 15.6MB of TTF became 981KB.
  The TTF sources are still tracked; removing them is the owner's call.
- [x] SV-1093 Coalesce scroll and resize into one animation frame and skip
  repagination when the budget is unchanged.
- [x] SV-1094 Keep prefetches that stay inside the new window instead of
  cancelling and reissuing them, reach one page backwards, stop cancelling with
  `src = ''` (which refetches the document URL), and match the canvas path's
  `crossOrigin`.
- [x] SV-1095 Reserve a placeholder height for continuous-scroll pages.
- [x] SV-1096 Give the CPU filters their own 4M pixel budget so a large page
  shows untouched rather than freezing the page turn.
- [x] SV-1097 Debounce the TXT search so a 5MB file is not rescanned per
  keystroke.
- [x] SV-1099 Add the book's `contentFingerprint` to page and thumbnail URLs so a
  replaced file is not served from the one-day image cache.
- [x] SV-1100 Show a placeholder while a page image is still arriving.

### Feature gaps

- [x] SV-1101 Keyboard zoom (`+`/`-`/`0`).
- [x] SV-1102 Keyboard access to contents, bookmarks, settings and page jump,
  using the same letters the fallback readers already use.
- [x] SV-1103 Show a reading percentage, which survives a font-size change in a
  way the page number does not.
- [x] SV-1104 Refit the page after a quarter turn.
- [x] SV-1105 Apply the text brightness filter to the reading surface rather than
  the glyphs, and widen its floor to 0.5.
- [x] SV-1106 Show a wide double-spread page on its own instead of pairing it,
  detected from the loaded image.
- [x] SV-1107 Highlights drawer with the five colours, notes and delete, all of
  which the backend already supported and no screen reached.
- [x] SV-1108 Inline selection toolbar so highlight, dictionary and read-aloud
  act on the selection instead of losing it.
- [x] SV-1109 Optional joining of hard-wrapped TXT lines, done at render time
  one character for one character so stored offsets do not move.

### Design consistency

- [x] SV-1110 Panel transitions, `overscroll-behavior`/`touch-action` on the
  stage, one name for bookmark-add and for the image filter, outline icon
  family, `aria-current` and a focus ring on the thumbnail strip, reader tokens
  on the fallback readers' chrome, and reader tokens in place of Vuetify's
  primary/secondary/error/grey inside the reader.

### Not implemented, see ADR-0018

- SV-1098 Local CBZ lazy extraction — blocked on the ADR-0016 object-URL
  lifecycle; enabling zip.js web workers is blocked by the recorded dependency
  review.
- Two-device position conflict, EPUB inside the shell, real offline reading,
  automatic margin trimming, and touch zoom (which collides with the SV-1066 and
  SV-1072 centre tap) each need an owner decision first.
