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
