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

## Mobile usability on a Galaxy S25 Ultra, reported 2026-08-07

Owner report: sign-in, finding a book and the reader are all uncomfortable on a
phone. A six-area audit found the discomfort was not a pile of separate gaps but
three missing foundations: every full-screen height was measured in `vh`, the
only breakpoint in the app was `max-width: 600px`, and touch targets across the
reader sat between 20px and 36px.

### Foundations

- [x] SV-1120 Measure full-screen heights against the small viewport. Chrome for
  Android resolves `vh` against the address-bar-hidden viewport, so the reader
  shell, its four fit-to-screen `calc()`s, the control menu, both fallback
  readers and Vuetify's `.v-application--wrap` were all 40-90px taller than the
  screen. The reader now carries a `--sv-viewport-height` token; the rest use the
  two-declaration `vh`/`svh` fallback `global.css` already used.
- [x] SV-1121 Add height-based breakpoints. A phone held sideways is 915x400, too
  wide for `max-width: 600px`, so it fell back to desktop chrome at half the
  height. `@media (max-height: 500px)` compacts the reader chrome and the
  sign-in card. `.startup-shell` is excluded from the `align-items` change
  because it lays out as a column.
- [x] SV-1122 Bring reader touch targets to 48px: bottom dock buttons (36px,
  4px apart), control-menu rows (42px, touching), contents links and bookmark
  rows (44px), and the highlight swatches (20px, now a 28px dot with an
  invisible 48px pad and a gap wide enough that the pads cannot overlap). The
  toolbar's `gap: 0` on narrow screens goes back to 4px.
- [x] SV-1123 Withhold the double-page layout on an upright narrow screen, where
  each half is about 194px. Only the available list changes, so the saved
  preference is untouched and a wide screen still opens the book as a spread.
  `applyReaderViewportChange` now re-normalises, so rotating corrects it.

### Reader interaction

- [x] SV-1124 Let a zoomed page turn again. Refines SV-1080 and SV-1083 rather
  than reversing them: a drag that actually panned is still never a page turn,
  but a swipe or a side-zone tap pages once the stage has run out of travel in
  that direction. `ShelfViewTouchStageState` gains `atHorizontalStart` and
  `atHorizontalEnd`; the SV-1080 regression test still passes unchanged.
- [x] SV-1125 Put zoom in and zoom out in the bottom dock, reusing
  `stepFixedPageScale`. This is an interim answer to ADR-0018 Decision 6, not a
  choice between its options: it adds no gesture, so it does not touch the
  SV-1066/SV-1072 centre tap, and pinch zoom stays open as the recommendation.
  Double-tap was deliberately not attempted for the same reason. The dock's
  progress percentage steps aside for the two buttons on a fixed-page book,
  where the page counter says the same thing and the toolbar still shows it.
- [x] SV-1126 Make the settings and overflow panels bottom sheets on a narrow
  screen. At full width they covered the page, so line height or brightness had
  to be judged with the text hidden, and the only close button sat at the top of
  a 6.9" screen. Adds `overscroll-behavior: contain` to both panels and the three
  drawers, so reaching the end of a list no longer hands the scroll to the
  document where a further pull triggers pull-to-refresh and drops an unsaved
  reading position.
- [x] SV-1129 Guard the touch path against text selection. `onReaderPointerDown`
  returns early for anything that is not a mouse, so on a phone only the touch
  path runs and it never had the selection check the mouse path has: widening a
  selection by 48px turned the page out from under it. Adds `@touchcancel`, which
  Chrome fires when a long press becomes a selection, and pins the selection
  toolbar across the width on a narrow screen, where only its left edge had been
  clamped and the dictionary and read-aloud buttons fell off the right.

### Getting to a book

- [x] SV-1127 Book, oneshot and series detail: the cover was a fixed 212px inside
  a 129px column, which pushed the page into a sideways scroll (rule 7-3); the
  primary Read button was `small`, 28px; both Read buttons carried the same
  label with only a mouse-only `title` to tell them apart, so the wrong one
  silently stopped saving progress; and a Korean file name with no spaces ran
  out of its column.
- [x] SV-1128 Sign-in: the email field had no `type`/`inputmode`, so Android
  opened a keyboard without an "@" and auto-capitalised the address; the password
  field had no reveal control although the password-change dialog already has
  one; and a failed sign-in showed English developer text on a screen that forces
  Korean. Only the credentials call is now wrapped, so a failure after
  authentication is not reported as a wrong password.

### Deliberately not done in this pass

- Third-priority polish is recorded but unimplemented: `@media (hover: hover)`
  around the card hover styles, a bookmark-delete confirmation, desktop-only
  scrollbar rules, a maskable PWA icon, a `theme-color` meta and 44px pagination.
- SV-1130 edge-to-edge (`viewport-fit=cover`) is not a one-line change and needs
  an ADR. Verification of the audit finding corrected the analysis: without the
  meta tag Chrome insets the layout viewport inside the system bars, so nothing
  is currently drawn under the gesture bar. Adding it turns the whole app
  edge-to-edge and every top bar, drawer and dashboard inset needs review. The
  `env(safe-area-inset-*)` calls already in the reader therefore resolve to their
  fallbacks today rather than being broken in a visible way.
- The body-width slider's 520px floor still does nothing on a 412px screen while
  its label keeps changing. Lowering the floor needs a migration of stored
  preferences.
- `EpubReader.vue` keeps its two inline `height: 100vh` values on the D2Reader
  container; only its `.scrolltab-content` cap moved to `svh`. The third-party
  reader sizes its iframe from those and changing them needs EPUB testing.

## Full application redesign, requested 2026-08-16

Owner request: reduce the application to a folder tree and a reader. Keep the
side-menu-plus-main-area shape, keep the viewer's full feature set, delete
everything else, and restyle what remains with minimal icon and colour use.

Owner decisions taken in the same session:

1. "Folder" means the existing path-backed library. No new virtual folder model.
2. Removal reaches the backend API, not only the frontend screens.
3. Search moves to the top of the side menu; the app bar goes away.
4. This slice is planning only — nothing is implemented until the ticket list
   and `docs/ADR/ADR-0019-reduce-to-folder-and-reader-app.md` are approved.

Follow-up decisions, later the same day:

5. Keep reading stays, as its own side-menu entry — five entries, not four.
   The dashboard's recommendation shelves still go.
6. Library pinning and reordering stay.
7. OPDS is removed entirely, not just its collection and read-list feeds.
8. Losing existing collections and read lists is accepted. No export is built;
   a pre-migration database backup is the only safeguard.

One measurement corrected the earlier plan: removing OPDS does **not** make the
read-list or collection removals cheaper. Only 2 of the 58 read-list files and 2
of the 23 collection files live under `interfaces/api/opds`. The three removals
are independent.

ADR-0019 is **Proposed, not Accepted**. Phase D breaks public API compatibility
and deletes user data, so no SV-123x ticket starts before the owner accepts it.

### Phase A — Shell and screen reduction (frontend only, revertible)

- [x] SV-1200 Rebuilt the side menu around five entries: keep reading, folders,
  users, preferences, sign out. Search moved from the app bar to the top of the
  drawer; `.app-shell__search` lost its bar centring and gained drawer padding.
  Pinned and unpinned library groups and `ReorderLibraries` stayed (owner
  decision 6). Removed: the brand block with its task progress bar, the import
  group, the media-management group, the settings group and the account group,
  together with `taskCount`, `taskCountByType`, the four `expand*` flags they
  drove, and the orphaned `.app-shell__brand` rule. The routes behind the
  removed entries still exist and are unreachable from the menu until SV-1204.
  `libraries` in `computed` was already unused before this ticket and was left
  alone per the surgical-change rule. Files: `views/HomeView.vue`,
  `styles/global.css`.
- [x] SV-1201 Deleted the `v-app-bar` and made the main area start empty. The
  `/` route no longer redirects to `dashboard`: the parent route lost its name
  and redirect, and a new `path: ''` child named `home` renders
  `views/EmptyMainView.vue`. Guards that fall back to `{name: 'home'}` now land
  on the empty area rather than on the dashboard. Removing the bar took the
  drawer toggle with it, so a fixed menu button was added that hides itself
  while the drawer is open — the only icon this phase adds, with
  `navigation.open_menu` as its accessible name. Orphans removed with it:
  `LogoMark` from this view, `toggleDrawer`, the `.app-shell__topbar*` rules and
  their 600px media-query block.
  Left as-is and worth knowing: `noLibraryGuard` / `noLibraryNorPinGuard` still
  guard `dashboard` and the browse routes, so a server with no libraries no
  longer shows `welcome` on arrival — the user sees the empty area until they
  pick a menu entry. That follows the owner's "empty until selected" rule, but
  it does mean first-run guidance now depends on the folder entry.
- [x] SV-1202 Folder create, rename and delete from the side menu. No API was
  added: `dialogAddLibrary`, `LibraryEditDialog` and `LibraryActionsMenu`
  already covered all three, and the delete dialog already demands the library
  name for confirmation. The change is the exposure: the `+` icon button became
  a labelled "Add library" entry under the folder list, per `CLAUDE.md`'s rule
  that admin actions must be recognisable controls rather than bare icons.
  Pinning and reordering stay wired.
- [x] SV-1204a Deleted the five screens with no dependants: metrics,
  announcements, updates, history and self authentication activity. Each was
  referenced only by `router.ts`. `AuthenticationActivityTable` survives because
  `SettingsUsers.vue` uses it too; only the standalone self-activity view went.
  Orphans that fell with them: the `tales-announcements`, `tales-releases` and
  `tales-metrics` plugins/services/types, their `main.ts` registrations, the
  `json-feed` types, and the `announcements`, `actuatorInfo`, `releases` and
  `booksToCheck` store state with their getters and mutations. `$actuator`
  itself stays — `ServerManagement.vue` uses it for shutdown and the log file.
  `tests/unit/views/MetricsView.spec.ts` was deleted because its subject no
  longer exists; no assertion was weakened to make a suite pass.
  Side effect worth recording: `HomeView.created` made four admin requests on
  every sign-in (actuator info, a books-to-check search, announcements,
  releases) and all four fed screens or badges that are now gone. Signing in as
  an admin now issues none of them, which is most of what SV-1221 was for.
- [x] SV-1203 Merged preferences into one screen, **implemented before SV-1200**
  rather than after it. Doing it in backlog order would have meant either
  deleting the drawer's theme and locale selects with nowhere for them to go, or
  writing a throwaway link from a new "preferences" menu entry to whichever of
  the three old screens the user was allowed to see. Moving a control is one
  action; splitting it across two tickets only manufactures an intermediate
  state where the app is worse.
  New `views/PreferencesView.vue` at `/settings` with three tabs — display
  (theme, locale), account, and an admin-only server tab that composes the
  existing `UISettings`, `ServerSettings` and `ServerManagement` components. No
  new locale keys: the tab labels reuse `common.ui`, `account_settings.my_account`
  and `server.tab_title`, so none of the 38 locale files were touched.
  `settings-server`, `settings-ui` and `account-me` became redirects so existing
  bookmarks survive, following the `/libraries/:id/recommended` precedent.
  `views/SettingsServer.vue` was deleted — it was a two-component wrapper that
  this change orphaned. `checkRoute` moved from `includes('/settings/')` to
  `startsWith('/settings')` so the new path still matched while the settings
  group existed.
- [ ] SV-1204 **Split during implementation.** Written as one ticket, it turned
  out to span 40 files referencing collections and 51 referencing read lists —
  well past the 800-line split threshold in `CLAUDE.md`. Now:
  - [x] SV-1204a metrics, announcements, updates, history, self auth activity
  - [x] SV-1204b import. `ImportBooks`, `ImportReadList`, `FileImportRow`, the
        two transient-book dialogs, `FileNameChooserDialog`, and the
        `tales-transientbooks` plugin/service/types. `DropZone` and
        `FileBrowserDialog` stayed — five edit dialogs and `LibraryEditDialog`
        still use them.
  - [x] SV-1204c media management. The five views, `PageHashKnownCard`,
        `PageHashUnknownCard`, `PageHashMatchesTable`, the `tales-pagehashes`
        plugin/service/types, `enum-pagehashes`, and three now-orphaned URL
        builders in `functions/urls.ts` (`transientBookPageUrl` fell here too,
        orphaned by SV-1204b). `ThumbnailExplorerDialog` stayed — `DivinaReader`
        uses it. `components/PagesTable.vue` is referenced by nothing and was
        already dead before this work, so it was left alone per the
        surgical-change rule; it is a candidate for a separate cleanup.
  - [ ] SV-1204d collections. Split again on contact with the code: deleting the
        type first would break every reference at once, so this goes
        entry-points → screens → plumbing, each step compiling on its own.
    - [x] SV-1204d-1 entry points. The two routes; the `COLLECTIONS` case in
          `getLibraryRoute` (the enum value stays so a stored preference still
          parses, and now falls through to the default); the collections tab and
          its count/SSE plumbing in `LibraryNavigation`; "add to collection" in
          `SeriesActionsMenu`, `OneshotActionsMenu` and `MultiSelectBar`; and the
          `@add-to-collection` listeners and handlers in `BrowseLibraries`,
          `BrowseReadList`, `DashboardView` and `SearchView`. Collections are now
          unreachable from the UI while the code still compiles.
    - [x] SV-1204d-2 screens and components. Deleted `BrowseCollections`,
          `BrowseCollection`, `CollectionsExpansionPanels`, both dialogs and
          `CollectionActionsMenu`, and stripped collections out of
          `BrowseSeries`, `BrowseOneshot`, `ItemCard` and `ReusableDialogs`
          (panels, remove-from-collection actions, SSE subscriptions, context
          links, thumbnail-SSE handling and the union types).
          Two dead links surfaced while doing it and were fixed in the same
          step, because SV-1204d-1 had removed their routes: `SearchBox` pushed
          `{name: 'browse-collection'}` when a collection result was clicked,
          and `SearchView` still rendered a whole collections shelf whose cards
          route through `CollectionItem.to()` — the same dead route. Both
          sections are gone. Collections are now absent from the UI entirely,
          not merely unreachable.
    - [x] SV-1204d-3 plumbing. The `tales-collections` plugin/service/types and
          its `main.ts` registration; six store state fields with their six
          mutations and six actions; the three COLLECTION and two
          THUMBNAILCOLLECTION events and their `tales-sse.service` listeners and
          `tales-sse` DTOs; `CollectionItem`, `ItemTypes.COLLECTION` and the
          `createItem` branch; `persisted-state`'s collection filter;
          `tales-series.service.getCollections`; the two `urls.ts` thumbnail
          builders; `ThumbnailCard`'s collection branch; the dead
          `browse-collection(s)` entries in the `document.title` guard; and
          `ContextOrigin.COLLECTION`.
          `LIBRARY_ROUTE.COLLECTIONS` is deliberately kept: unlike the others it
          is a *persisted user preference*, and removing the enum value would
          break parsing for anyone whose stored library route is COLLECTIONS.
          It falls through to the browse default.
    - **Excluded on purpose:** `importComicInfoCollection` in
      `types/tales-libraries.ts` and `LibraryEditDialog`. It is a backend library
      DTO field, not a frontend concern; dropping it from the form would change
      what the client sends on library create/update. It belongs to Phase D with
      the backend, not to a frontend-only ticket.
  - [x] SV-1204e read lists. Complete.
    - [x] Entry points **and** their callers in one step, applying the SV-1204d-2
          lesson: both routes, the `READLISTS` case in `getLibraryRoute`, the
          `document.title` guard, the `LibraryNavigation` tab, "add to read
          list" in three action menus and `MultiSelectBar`, the handlers in
          `BrowseBooks`/`BrowseLibraries`/`BrowseSeries`/`DashboardView`, and —
          in the same pass, so no dead link ever existed — the read-list result
          and routing in `SearchBox`, the read-list shelf in `SearchView`, and
          `ReadListItem` in `types/items.ts`.
    - [x] `BrowseBook`: the read-list branch of sibling navigation collapsed to
          the series branch. Prev/next and the book-list menu still work inside
          a series; only the read-list context is gone.
    - [x] `BrowseOneshot`: its sibling navigation was read-list-only
          (`v-if="context.origin === ContextOrigin.READLIST"` on every control),
          because a oneshot's series holds a single book. That whole cluster —
          prev button, book-list menu, next button, context alerts, and the
          `siblings`/`siblingNext`/`siblingPrevious`/`contextName` state behind
          it — is therefore gone rather than simplified.
    - [x] `ItemCard`, `ReusableDialogs`, and deletion of `BrowseReadLists`,
          `BrowseReadList`, `ReadListsExpansionPanels`, both dialogs,
          `ReadListActionsMenu` and `ReadListMatchRow` (the last already
          orphaned by SV-1204b, which should have taken it).
    - [x] Plumbing: `tales-readlists` plugin/service/types and its `main.ts`
          registration; `tales-books.service.getReadLists`; six store state
          fields with six mutations and six actions; the three `READLIST_*` and
          two `THUMBNAILREADLIST_*` events with their SSE listeners and DTOs;
          `ThumbnailCard`'s read-list branch; three `urls.ts` builders;
          `persisted-state`'s read-list filter; and `ContextOrigin.READLIST`.
          The three readers' sibling navigation (`ShelfViewReaderShell`,
          `DivinaReader`, `EpubReader`) now takes the series path only;
          `DivinaReader` also loses its "set current page as read-list poster"
          menu entry.
    - Keep `LIBRARY_ROUTE.READLISTS` for the same reason `COLLECTIONS` was kept:
      it is a persisted user preference. `ContextOrigin.READLIST` can go with
      the plumbing, like `ContextOrigin.COLLECTION` did.
  Backend endpoints stay reachable throughout, so each sub-ticket is a
  `git revert` away from reversible.
- [ ] SV-1205 Reduce `DashboardView.vue` (774 lines) to the resume block only.
  **Assumption confirmed by the owner on 2026-08-17**: "이어보기는 유지" means the
  resume block; the recommendation shelves go.
  Scope established by reading the file:
  - Keep: the `dashboard__heading`, the `dashboard-resume` section and its seven
    `resumeBook*` computeds, `resumeThumbnailCacheBust`, and the
    `BOOK_*`/`READPROGRESS_*`/`THUMBNAILBOOK_*` subscriptions that refresh them.
  - Keep `loaderKeepReadingBooks` — this is the catch. `resumeBook` is
    `loaderKeepReadingBooks?.items[0]`, so the resume block is a view onto that
    loader, not independent state. Its `hasSection(RecommendedViewSection.KEEP_READING)`
    guard must become unconditional once the view config is gone.
  - Remove: the six other loaders (on-deck, recently added books/series,
    recently updated series, recently released, recently read); the
    `sections`/`SectionConfig`/`SectionType` machinery and `RecommendedViewSection`
    usage; the shelf `v-for` with its skeleton and `horizontal-scroller` blocks;
    the edit-view FAB and `EditRecommendedDialog`; and the two `multi-select-bar`
    instances with `selectedSeries`/`selectedBooks` and every handler they feed
    (mark read/unread, edit, bulk edit, delete) — all of which exist to act on
    shelf cards.
  - Decide while implementing: `empty-state` is currently gated on `allEmpty`,
    which is computed across all seven loaders. With one loader left it should
    mean "nothing in progress".
  - Check afterwards whether `ItemBrowser`, `ItemCardSkeleton` and
    `HorizontalScroller` still have consumers elsewhere before assuming they are
    orphaned; `SearchView` and the browse views also use them.

### Phase B — Visual redesign (revertible)

- [ ] SV-1210 Define the reduced palette and icon set. `Rule_UIUX` 1-3 already
  caps a screen at four colour groups; this ticket makes that explicit as tokens
  and removes decorative colour from the shell. Icons stay only where a label
  cannot replace them.
- [ ] SV-1211 Restyle the folder contents: series and book cards, list density,
  empty and loading states. `Rule_UIUX` 1-1 forbids a blank moment during
  loading, so the empty main area and a loading main area must be visually
  distinct.
- [ ] SV-1212 Align the reader chrome with the new visual language without
  removing any control. Every viewer setting — fit, layout, rotation, image
  filters, PDF handling, page number, bookmarks — must still be reachable; this
  is a restyle, not a feature review.

### Phase C — Speed (revertible)

- [ ] SV-1220 Measure a baseline before claiming any improvement: production
  bundle size per chunk, cold first paint, and time to first page of a book.
  Record the numbers in `docs/AI_PROGRESS.md`. Without this, Phase A's bundle
  reduction cannot be reported honestly.
- [ ] SV-1221 Reduce startup work. `HomeView.created` currently fires four admin
  requests (actuator info, books-to-check search, announcements, releases) on
  every load; three of those belong to features Phase A deletes.
- [ ] SV-1222 List and thumbnail loading policy for large folders, measured
  against SV-1220's baseline.

### Phase D — Backend removal (irreversible, needs ADR-0019 accepted)

- [ ] SV-1230 Dependency survey before any deletion. Map each removal candidate
  to its controller, service, repository, jOOQ tables, scanner hooks, tasks, SSE
  events and tests. Measured coupling today: read lists touch 58 Kotlin files
  (2 of them OPDS), collections 23 (2 OPDS), historical events 12, transient
  books 5, OPDS 11 files of its own plus 11 referencing it. Output is a
  per-feature removal order and a list of shared code that must not be deleted.
  No code changes in this ticket.
- [ ] SV-1231 Remove transient books and the import endpoints (5 files, the
  most self-contained candidate — a rehearsal for the larger removals).
- [ ] SV-1232 Remove metrics, announcements, releases and historical events.
  Historical events are an audit trail other services publish to, so this is a
  removal of publishers as well as of the reader.
- [ ] SV-1233 Remove page hashes and the media-management endpoints (analysis,
  missing posters, duplicate files, duplicate pages).
- [ ] SV-1234 Remove collections (23 files).
- [ ] SV-1235 Remove read lists (58 files). Last, and alone in its ticket:
  `BookController` sibling navigation, OPDS feeds and thumbnail handling all
  reference read lists, so book navigation needs a regression pass.
- [ ] SV-1236 Remove OPDS completely: the `interfaces/api/opds` v1 and v2 feeds,
  `OpdsGenerator`, `OpdsCommonController`, `OpdsLinkRel`, the OPDS entries in
  `Constants`, `OpdsAuthenticationEntryPoint` and its `SecurityConfiguration`
  chain, and the OPDS-specific `EtagFilterConfiguration`, `WebMvcConfiguration`
  and XML message-converter wiring where nothing else needs them. Independent of
  SV-1234/SV-1235; can run before or after either. Verify no `/opds` path stays
  reachable and no XML converter is left registered for a removed content type.
- [ ] SV-1237 Forward-only Flyway migrations dropping the now-unreachable
  schema, one per feature area. Existing migrations are never edited.
- [ ] SV-1238 Backup gate. The owner accepted the loss of existing collections
  and read lists, so no export is built and the pre-migration database backup is
  the only way back. This ticket takes and verifies that backup, and records in
  `AI_PROGRESS.md` where it is and that a restore was actually tested — an
  untested backup is not a safeguard. Blocks every SV-1237 migration deploy.

### Phase E — Reader verification (revertible)

- [ ] SV-1245 Decide the old readers' fate. `EpubReader.vue` cannot be deleted —
  the ShelfView EPUB bridge hands off to it. `DivinaReader.vue` is the
  documented fallback in `ARCHITECTURE.md` and the target of
  `shelfViewReaderGuard` when the flag is off; deleting it means making the
  ShelfView reader unconditional first.
- [ ] SV-1246 Run the viewer regression checklist across ZIP, CBZ, PDF, image
  directory, TXT, mixed ZIP and EPUB, covering fit, layout, rotation, filters,
  page number and bookmarks. This is the ticket that proves requirement 4 —
  "viewer stays feature-complete" — survived the redesign.

### Questions answered 2026-08-16

- Keep reading: **kept**, as its own side-menu entry (SV-1200, SV-1205). The app
  no longer opens onto it.
- Library pinning and reordering: **kept** (SV-1200, SV-1202).
- OPDS: **removed entirely** (SV-1236).
- Collection and read-list data loss: **accepted**, no export built. The backup
  gate in SV-1238 is what stands in its place.

### Still assumed rather than stated

- SV-1205 assumes "이어보기는 유지" means the resume block, not the dashboard's
  recommendation shelves. Confirm before implementing.
- Whether the ShelfView reader becomes unconditional, which is what deleting
  `DivinaReader.vue` would require, is still SV-1245's question.

## Type-check health, found 2026-08-16 during the M9 work

Not part of the redesign the owner asked for. Recorded because the measurements
came out of it and the condition affects every future build.

Measured facts:

- The webpack build takes **19 minutes** with the type check and **56 seconds**
  without, on the same tree. The check is ~98% of build time.
- At `fork-ts-checker`'s 2048 MB default the check runs out of memory partway
  through and aborts — and the build still prints `Build complete`. Several
  "green" builds today had run no type check at all.
- Given enough memory to finish, it reports **176 pre-existing type errors**
  across 42 files on HEAD before any of today's work (155 across 35 files
  after). The project has never had a passing type check.
- Most errors cascade from `TS2589: Type instantiation is excessively deep`
  against `Vue.extend({...})`. Once inference gives up, even `this.$router`
  resolves as a missing property. Same root cause as the OOM: the type graph is
  past what the compiler will carry.
- Plain `tsc --noEmit` does **not** check `.vue` files, and `vue-tsc` is a Vue 3
  tool that does not apply to this Vue 2.6 project. `fork-ts-checker` inside the
  build is the only `.vue` type check available here.

Done now (no dependency added, no default behaviour changed):

- [x] `vue.config.js` accepts `SHELFVIEW_TSCHECK_MEMORY` so the check can be
  given enough memory to complete when someone wants a real answer.

Open, needs an owner decision:

- [ ] SV-1250 Decide what to do about the 176 errors. Either fix them, or accept
  them formally and stop pretending the check is meaningful. Until one or the
  other, "the build passed" carries no type guarantee, and raising the memory
  limit by default would simply turn every build red.
- [ ] SV-1251 Consider splitting the type check out of `npm run build` so the
  normal build stays at ~56 s and the check is an explicit, separate command.
  Blocked on SV-1250: there is no point scheduling a check that always fails.

## Speed pass from 2026-08-20

Owner asked for ways to improve features or speed, then for all of them to be
done. Everything below was measured before it was changed; the numbers are from
this repository and, for compression, from the running server.

Baseline, first-load JavaScript and CSS, measured both locally and against
`[internal-host]:3333`:

| | raw | gzip |
|---|---:|---:|
| `chunk-vendors.js` | 2,017,858 | 431,590 |
| `app.js` | 1,433,841 | 394,337 |
| `chunk-vendors.css` | 612,209 | 83,379 |
| `app.css` | 8,105 | 2,306 |
| **total** | **4,072,013** | **911,614** |

After the five frontend and config tickets: **2,036,844 raw / 397,310 gzip**.
Because SV-1260 also turns compression on, what the browser actually downloads
goes from 4,072,013 bytes to 397,310 — a 90% reduction.

- [x] SV-1260 Enable HTTP response compression. `server.compression` was never
  configured, and Spring Boot defaults it to off. Confirmed against the running
  server: `chunk-vendors.js` came back as `Content-Length: 2017858` with no
  `Content-Encoding`, for a request that sent `Accept-Encoding: gzip, deflate,
  br`. Nothing compresses on the way out either — the app is exposed directly,
  not behind a compressing proxy. The mime types are Spring Boot's own defaults
  plus SVG, written out rather than inherited so it stays visible that page
  images and web fonts are excluded. **Not verified running**: there is no JDK on
  the development machine, so the backend was never compiled here. Verify after
  deploy with `curl -H 'Accept-Encoding: gzip' -I`.
- [x] SV-1261 Remove the dead charting stack. SV-1204a deleted the metrics view;
  `chart.js`, `vue-chartkick` and their two `@types` packages stayed behind, and
  the only remaining references were three lines of `main.ts`. `moment` was never
  imported anywhere in `src/` — it arrived as a transitive dependency of
  chart.js 2.9. Source-map attribution put the three at 462 KB of the 1,936 KB
  minified vendor chunk. Result: `chunk-vendors.js` −525,975 bytes (−26%), and
  none of the three appear anywhere in `dist` afterwards. `moment` stays in the
  lockfile because `r2-lcp-js` and `r2-shared-js` declare it, but nothing in the
  bundle pulls it in.
- [x] SV-1262 Load locales on demand. `i18n.ts` inlined all 38 locale files into
  `app.js` — 1,325 KB minified, of which one locale is ever read. The four the
  preferences picker offers (`ko`, `en`, `ja`, `zh-Hans`) stay eager, because the
  picker labels each entry in its own language with `$t('common.locale_name', x)`
  and needs those messages before the user has chosen. The other 34 became one
  chunk each. `app.js` −83%.
  Two call sites guarded on `i18n.availableLocales`, which is now only the loaded
  locales: `App.vue`'s watcher loads the chunk before switching, and
  `tales-settings.plugin` checks the new `KNOWN_LOCALES` instead.
  `PreferencesView` was deliberately left alone — its existing
  `availableLocales` filter now reads as "offered and eager", which is the
  correct guard.
  **Caught during implementation**: passing `require.context` a regex held in a
  const produces an empty context. Webpack only reads literal arguments. The
  first build emitted no locale chunks at all and `KNOWN_LOCALES` would have been
  empty at runtime, which would have stopped even Korean from applying. The
  regex is now written out at the call site with a comment saying why.
- [x] SV-1263 Subset the Material Design Icons stylesheet.
  `@mdi/font` ships 7,296 icon rules; the application names 130. That was 329 KB
  of the 612 KB vendor stylesheet and 51 KB of its 83 KB gzipped — and a
  stylesheet blocks the first paint. `tales-webui/webpack/mdi-subset-loader.js`
  is a `pre` loader that rebuilds the used-icon list from source on every
  compile. Result: 171 rules kept, 7,125 dropped, `chunk-vendors.css` −53% raw
  and −60% gzipped, with all 147 required icons verified present afterwards.
  Written as a loader rather than a committed generated stylesheet because a
  generated file goes stale the moment someone adds an icon, and the icon then
  renders as an empty box.
  **Two traps found while doing it**: Vuetify's own 33 icons are named in
  `vuetify/lib/services/icons/presets/mdi.js`, not in `src/`, so scanning only
  the application would have deleted the checkbox tick and the pagination
  chevrons. And `ReadMore.vue` assembles `mdi-chevron-{{ open ? 'up' : 'down' }}`
  at runtime, which a whole-name scan sees only as the stem `mdi-chevron-`;
  `mdi-chevron-up` is in neither `src/` nor the Vuetify preset, so it would have
  gone. The loader now treats a stem that runs into an interpolation as a prefix
  and keeps everything under it.
  **Not done**: the webfont itself. `materialdesignicons-webfont.woff2` is still
  396 KB for 171 glyphs, and the `@font-face` also names `.eot`, `.woff` and
  `.ttf`, so 3.5 MB of font files ship in the image for the one file browsers
  fetch. Subsetting a woff2 needs a font tool, which is a new dependency.
- [x] SV-1264 Run the canvas image filters in a worker. The 2D filters are
  per-pixel passes on the main thread, and `canvas-image-processing.ts` already
  said so: above `SHELFVIEW_CANVAS_CPU_MAX_PIXELS` (4 MP) it gave up and showed
  the page unfiltered rather than stall the page turn. The pixel pass now runs in
  a worker, pixels moving both ways as a transferred `ArrayBuffer`. This is a
  feature change as much as a speed one: pages between 4 MP and the reader's
  12 MP budget are filtered now where before they were silently left alone.
  Without workers the runner falls back to the main thread and keeps the old
  4 MP refusal exactly.
  `import.meta.url` — how webpack 5 finds a worker entry — is a syntax error once
  Jest rewrites a module to CommonJS, so `new Worker(...)` lives in its own file
  reached by a dynamic import that only runs when `Worker` exists. Under jsdom
  the file is never loaded, so it is never parsed.
- [x] SV-1265 Cache open PDF documents. `PdfExtractor.getPageContentAsImage`
  re-parsed the whole file on every page request, and `FixedPageAdapter`
  prefetches -1 to +4, so one page turn issued six concurrent full parses of the
  same PDF. [ADR-0020](ADR/ADR-0020-pdf-document-cache.md) has the design and,
  more importantly, why it uses `tryLock` with a load-per-request fallback rather
  than blocking: `PDDocument` cannot render concurrently, and whether "one parse
  plus serialised renders" beats "six parallel parses" depends on whether parsing
  or rendering dominates for a given file — which has not been measured here. The
  fallback removes the need to know: sequential reading hits the cache, a burst
  that finds the lock held behaves exactly as today, so no access pattern gets
  slower.
  **Verified 2026-08-20** with a temporary JDK 21: Kotlin compilation and
  ktlint passed, and the focused PDF extractor suite exercises rendering,
  cache shutdown, and non-`Exception` failure ownership.
- [x] SV-1266 Block non-EPUB media from the legacy D2Reader route. A caller
  could open `/book/{bookId}/read-epub` directly with a PDF book and reach the
  `pdfjs-dist 2.14.305` bundled by `@d-i-t-a/reader`, which is affected by
  [CVE-2024-4367](https://github.com/advisories/GHSA-wgrm-67xf-hhpq).
  `EpubReader.setup` now checks the loaded book before requesting its series,
  progression, manifest, or constructing D2Reader. Non-EPUB media is replaced
  with the normal fixed-page route (`read-book-v2` when ShelfView is enabled,
  otherwise `read-book`), preserving route parameters. EPUB behavior is
  unchanged. The teardown also tolerates the intentionally uninitialized
  reader on this early-return path.
- [x] SV-1267 Restore direction-specific side taps on horizontally overflowing
  fixed pages. The prior boolean enabled both side zones when either horizontal
  edge was reached, so a right tap at the left edge advanced even though the
  page still had content to pan towards. `shelfViewTapAction` now receives
  independent physical left/right edge availability. LTR/RTL only decides the
  resulting previous/next action after the tapped physical edge is eligible.
  Pure input tests cover both directions and both edges; the reader integration
  test now verifies that the right tap is ignored at the left edge and advances
  only after `scrollLeft` reaches the right edge.
- [x] SV-1268 Remove confirmed frontend orphan code. Repository-wide tracked
  reference searches and the explicit router/component registration paths found
  no consumers for `PagesTable.vue`, `BookPickerDialog.vue`,
  `SeriesPickerDialog.vue`, or `series-slim.ts`; their last consumers were
  removed by SV-1200..SV-1204. The four files are deleted. The reader test
  factory also no longer accepts or registers the already removed
  `$talesReadLists` service. Historical progress/backlog mentions remain as
  documentation, and the unused locale namespaces are recorded separately from
  this code-only cleanup rather than expanding the ticket across every locale.
- [x] SV-1269 Preserve OAuth login icons in the MDI subset. `LoginView` built
  `mdi-{{ provider.registrationId }}` at runtime, which the source scanner could
  not turn into concrete glyph names; Google, Facebook, and GitHub were removed
  from the generated CSS. Provider ids now go through a small explicit mapping,
  with `mdi-login-variant` for arbitrary OIDC registrations. All four concrete
  names are statically visible to the subset loader. A loader-level regression
  test proves those rules stay while an unused rule is removed; applying the
  loader to the full upstream CSS keeps 175 of 7,296 glyph rules and retains all
  four OAuth rules.
- [x] SV-1270 Harden canvas filter worker lifecycle. Concurrent first calls now
  share one creation promise. A single dispatcher transfers only the active
  image buffer while later jobs remain cancellable in a queue. Each canvas page
  aborts its prior job when its source/mode changes or it unmounts; aborting
  active synchronous worker work terminates that worker, then resumes valid
  queued work on a fresh one. `error`, `messageerror`, protocol mismatch, and
  `postMessage` failure reject and detach every pending listener/promise instead
  of hanging. Tests cover shared creation, both worker error channels, post
  failure, real active cancellation/restart, worker and synchronous paths.
- [x] SV-1271 Make PDF cache document ownership and shutdown deterministic. A
  locally loaded `PDDocument` now stays under a `finally` guard until Caffeine
  explicitly takes ownership, covering `Error` subclasses and cache insertion
  failures as well as `Exception`. The cache removal listener uses a direct
  executor, so eviction and `@PreDestroy` return only after the handle closes.
  Tests inspect the real cached PDFBox document immediately after shutdown and
  mock an `AssertionError` render path; both closure guarantees pass. ADR-0020
  now also states that mtime+size detects ordinary changes but is not content
  identity when both metadata values are deliberately preserved.

## Security and quality follow-ups from the 2026-08-20 audit

- [x] SV-1272 Bound archive enumeration and extraction across every served
  path. The legacy `ZipExtractor`, `RarExtractor`, and `Rar5Extractor` still
  materialize whole entry lists or entry bodies without a byte budget, and the
  mixed-ZIP path materializes entries before enforcing its count limit. Apply
  entry-count, per-entry uncompressed-byte, cumulative request, nested-content,
  and decoded-image limits before allocation. Add oversized/unknown-size and
  archive-bomb regression tests for both scanning and authenticated page
  delivery. Consider a separate global extraction concurrency budget after the
  per-request boundaries are reliable.
  Completed 2026-08-20: one `shelfview.archive` policy now bounds entry count,
  central-directory and parser-construction metadata, per-entry and cumulative
  uncompressed bytes, compression ratio, nested archives, decoded dimensions,
  PDF pages and rendered output. ZIP/EPUB use an O(1) central-directory
  preflight on the same channel handed to Commons Compress; RAR4 limits the
  exact channel reads performed by junrar; mixed nested ZIPs and PDFs spool to
  owned temp files with deterministic cleanup. Limit failures are stable HTTP
  413 codes without paths or parser messages. Unit tests cover declared,
  unknown and forged metadata limits, and authenticated legacy/mixed API tests
  exercise the real extractors. A global cross-request extraction concurrency
  ceiling remains a separate operational follow-up.
- [x] SV-1273 Confine standalone TXT reads to the authorized media root and
  make the byte cap allocation-safe. Reject symbolic-link targets and files
  whose resolved real path escapes the owning library root, open without
  following links, and stream at most `maxBytes + 1` rather than calling
  `Files.readAllBytes` after a separate size check. Cover link replacement,
  file growth between metadata/read operations, missing files, and exact-limit
  reads.
  Completed 2026-08-20: standalone TXT delivery now resolves the owning library
  from the stored book, verifies the authorized and raw-book library ids still
  agree, confines the real file to the real library root, rejects final links,
  and opens with `NOFOLLOW_LINKS`. The same channel is revalidated and read
  through a size-agnostic bounded view, so a file that grows after its size
  query is stopped after `maxBytes + 1` bytes. Tests cover the exact boundary,
  forged/growing size metadata, root escape, final and intermediate links,
  post-registration link replacement, missing files, and the authenticated API.
- [x] SV-1274 Sanitize production error responses. Change the production
  `server.error.include-message` policy from `always`, separate stable public
  validation codes from exception messages, and add API tests proving server
  filesystem paths, book `toString()` values, SQL details, and parser messages
  cannot appear in JSON error bodies or ordinary logs.
  Completed 2026-08-20: production error attributes now return only HTTP status
  and a stable code even when detail query flags or external error settings ask
  for more. Controller advice preserves framework status and protocol headers
  while discarding exception reasons and validation messages. Application logs
  use opaque ids, counts, and exception types instead of media paths, parser/SQL
  messages, throwable stacks, passwords, or session identifiers; model string
  forms used by logging are similarly restricted. Focused error, security,
  logging, analyzer, archive, PDF, user lifecycle, and style checks pass.
  Malformed JSON is explicitly retained as a sanitized `400 / HTTP_400`
  response instead of falling through the generic handler as a server error.
- [x] SV-1275 Prevent stale lazy-locale requests from overriding a newer user
  choice. Give locale loads an order token (or compare the store's current
  locale before applying the result), and test a slow first load resolving
  after a later eager and lazy selection.
  Completed 2026-08-20: the root locale watcher now gives each load a monotonic
  request number and rechecks the store before applying locale/RTL state. Tests
  deterministically resolve an older lazy request after newer eager and lazy
  choices, plus after a newer failed load; no stale result changes the UI.
- [x] SV-1276 Remove or patch the legacy reader's vulnerable production
  dependency chain. SV-1266 blocks non-EPUB media from the reachable PDF.js
  route, but `@d-i-t-a/reader` still installs `pdfjs-dist 2.14.305`. Inventory
  the 22 current production-tree audit findings by actual browser/server reach,
  upgrade or remove verified vulnerable code without a broad framework
  migration, and keep regression coverage for the EPUB fallback.
  Completed 2026-08-20: replaced the stale Git fork with official reader 2.5.3,
  whose shipped bundle contains PDF.js 5.5.207 instead of 2.14.305, and moved
  builds to Node 22.13+/npm 11. Compatible `qs`, `form-data`, `nanoid`,
  `brace-expansion`, and external sanitizer nodes were refreshed. Clean install,
  EPUB initialization/fallback tests, type/lint checks, and a production build
  pass; the deployed EPUB chunk contains no 2.14.305 code. Audit findings fell
  from 22 to 16. Ten remaining reader findings are Node-only installation-tree
  leaves absent from the static browser/JAR artifact; five are the Vue 2 runtime
  advisory and its dependents with no runtime-template compiler sink; one is
  Vuetify 2, whose vulnerable user-defined date formatter and untrusted preset
  paths are not used. The official reader still prebundles older sanitizer and
  nanoid code, but their affected Search/TTS/custom-size paths are disabled or
  unused; replacing that dormant code requires an upstream slim build rather
  than a misleading root override.
- [x] SV-1277 Bind MIXED Locators to the current Manifest item. Validation
  currently checks only that `itemId` and nested `contentFingerprint` are
  non-empty, not that the item exists or that its derived fingerprint matches.
  Validate the pair against the current book Manifest and bound identifier
  lengths before storing positions/bookmarks/highlights, with stale and forged
  locator tests.
  Completed 2026-08-20: Manifest responses and state writes now share one
  bounded mixed-item resolver. Positions, bookmarks, and highlights accept only
  the current item id/index, server-derived nested fingerprint, matching item
  kind, and in-range nested page; item ids and fingerprints are length-bounded
  before archive work. A weighted, write-expiring cache avoids repeating PDF or
  nested-archive inspection on the two-second position autosave path. Real-ZIP
  tests cover valid saves plus missing ids, forged indexes, stale fingerprints,
  oversized ids, and mismatched highlight selectors.

### Measured but not acted on

- The Korean webfonts are the largest remaining download: Pretendard in three
  weights is 2.3 MB and Gowun in two is 0.98 MB. `font-display: swap` is already
  set, so they do not block the first paint, but they are still the bulk of what
  a phone fetches. Splitting them into unicode-range subsets is the standard fix
  and needs a font tool.
- `ShelfViewReaderShell.vue` is 7,357 lines. Not a speed problem; recorded
  because everything in the reader has to be changed inside it.
