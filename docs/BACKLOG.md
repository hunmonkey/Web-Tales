# ShelfView Backlog

The detailed acceptance criteria are in `SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md`.
Work in order unless an ADR explicitly changes dependencies.

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
- [ ] SV-602 Desktop browser validation

## M7 Operations

- [x] SV-700 CI checks (workflow added; first GitHub Actions run validates it live)
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
