# ShelfView Backlog

The detailed acceptance criteria are in `SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md`.
Work in order unless an ADR explicitly changes dependencies.

## M0 Foundation

- [x] SV-000 Repository audit and code map
- [x] SV-001 Base tag, remotes, upstream sync policy, license preservation
- [x] SV-002 Local backend/frontend baseline and test results
- [ ] SV-003 Feature flag, `/read-v2/:bookId` shell and old-reader fallback

## M1 Manifest and shell

- [ ] SV-100 Versioned Manifest domain, DTO and one fixed-page response
- [ ] SV-101 TypeScript Manifest model and API service
- [ ] SV-102 Unified reader loading/error shell and one page display

## M2 Fixed page reader

- [ ] SV-200 FixedPageAdapter
- [ ] SV-201 Single/double/continuous/LTR/RTL/fit modes
- [ ] SV-202 Content-only CSS filters and presets
- [ ] SV-203 Keyboard, touch, focus and accessibility

## M3 Position, bookmarks and preferences

- [ ] SV-300 New Flyway migration and repositories
- [ ] SV-301 Extended position API
- [ ] SV-302 Multiple bookmark API
- [ ] SV-303 Scoped preference API
- [ ] SV-304 Frontend restore, debounce save and bookmark/settings UI

## M4 TXT

- [ ] SV-400 Scanner/domain design spike and ADR
- [ ] SV-401 TXT scan and analysis
- [ ] SV-402 TXT content and encoding API
- [ ] SV-403 TextReflowAdapter and themes
- [ ] SV-404 Stable paragraph/character Locator

## M5 Image-directory books

- [ ] SV-500 Library setting and `.shelfview-book` domain rule
- [ ] SV-501 Scan, natural sort, MIME/dimension validation and metadata
- [ ] SV-502 Page delivery and cache integration
- [ ] SV-503 Change, move, delete and marker lifecycle

## M6 Compatibility and UX

- [ ] SV-600 Existing Komga progress compatibility
- [ ] SV-601 Corrupt/missing/network/encoding recovery UX
- [ ] SV-602 Desktop/mobile/browser validation

## M7 Operations

- [ ] SV-700 CI checks
- [ ] SV-701 Versioned multi-arch Docker image
- [ ] SV-702 Unraid install, backup, upgrade and rollback guide

## M8 Release

- [ ] SV-800 Threat model
- [ ] SV-801 Performance baseline
- [ ] SV-802 Dependency/license/notice audit
- [ ] SV-803 v1.0 release candidate

## Later, not MVP

- [ ] PDF.js text layer, search and document links
- [ ] WebGL sharpening, background cleanup and adaptive monochrome
- [ ] Browser-local file opening with zip.js
- [ ] Offline PWA
- [ ] EPUB reader unification
