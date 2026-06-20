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

- [x] SV-600 Existing Komga progress compatibility
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

- [ ] PDF.js text layer, search and document links
- [ ] WebGL sharpening, background cleanup and adaptive monochrome
- [ ] Browser-local file opening with zip.js
- [ ] Offline PWA
- [ ] EPUB reader unification

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

## Deferred after SV-930 closure

- [ ] Owner-approved server/account-synced dictionary provider storage

## Deferred after SV-950 closure

- [ ] Browser/device performance evidence for Canvas sharpen and future WebGL image-quality modes
