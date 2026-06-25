# SV-996: Archive Expansion Triage

Date: 2026-06-25

## Scope

This triage covers the owner-requested archive follow-ups after SV-980:

- nested RAR/7Z support
- mixed top-level TXT/PDF plus nested image chapters
- related archive expansion boundaries

It does not implement a new archive reader. The goal is to separate the next
safe implementation candidate from work that needs another ADR or dependency
review first.

## Current Support Map

| Case | Current state | Notes |
|---|---|---|
| Top-level image ZIP/CBZ | Supported | Existing Divina fixed-page path. |
| Top-level image RAR/CBR | Supported for RAR4, conditional for RAR5 | RAR5 is enabled only when the native-backed reader is available. |
| Top-level 7Z | Deferred by ADR-0013 | Plan is a later dedicated 7Z/CB7 fixed-page extractor behind the comic-archive scan gate. |
| Standalone TXT | Supported when library TXT scan is enabled | One TXT file becomes one book. |
| Standalone PDF | Supported when library PDF scan is enabled | One PDF file becomes one fixed-page PDF book. |
| Mixed ZIP with top-level TXT/PDF/image/GIF | Supported | Uses `MIXED_CONTENT_SEQUENCE` and `MixedContentAdapter`. |
| Image-only ZIP with one-level nested ZIP/CBZ chapters | Supported | ADR-0010 / SV-980, only when the outer ZIP has no top-level TXT/PDF entries. |
| Mixed ZIP with top-level TXT/PDF plus nested ZIP/CBZ image chapters | Supported by SV-997 | Nested ZIP/CBZ image chapters become fixed-page mixed items after validation. |
| Nested RAR/CBR inside ZIP/RAR | Deferred by ADR-0012 | Plan is a later temp-file-backed image-chapter implementation with stricter limits. |
| Nested 7Z | Not supported | Requires top-level 7Z support first. |
| Folder book mixing TXT/PDF files plus nested image chapter archives | Deferred by ADR-0014 | Plan is a later explicit `.shelfview-mixed-book` model, not an archive tweak. |

## Findings

1. `ZipExtractor` expands one level of nested `zip`/`cbz` only when the outer
   archive has no top-level TXT/PDF entries.
2. `ShelfViewZipMixedContentProvider` allows one-level nested ZIP/CBZ image
   chapters in mixed ZIPs after SV-997; unsupported nested archive entries
   remain rejected.
3. `RarExtractor` and `Rar5Extractor` read direct archive entries, but do not
   expose a bounded nested-archive abstraction like ADR-0010 added for ZIP.
4. 7Z is not part of the scanner extension list or `MediaType`; ADR-0013
   plans this as a later dedicated top-level extractor.
5. Existing security docs require path traversal, nested archive, entry count,
   decompressed size, compression ratio and image/PDF bounds before serving
   content.

## Triage Decision

### Next Feasible Candidate

Add mixed ZIP support for one-level nested ZIP/CBZ image chapters.

This should extend the existing `ShelfViewZipMixedContentProvider`, not the
generic frontend reader. A nested image chapter can appear as one ordered
`FIXED_PAGE_SEQUENCE` mixed item:

- item label: sanitized outer archive name, such as `ch01.cbz`
- item id: opaque id derived from book fingerprint, outer entry metadata and
  normalized nested entry list
- page count: number of validated image entries inside the nested archive
- page endpoint: existing mixed page endpoint shape,
  `/api/shelfview/v1/books/{bookId}/mixed/{itemId}/pages/{page}`
- page serving: reopen or cache the bounded nested archive bytes, then return
  the requested validated inner image

Security limits should mirror ADR-0010 and mixed ZIP limits:

- one nested level only
- nested archive extensions limited to `zip` and `cbz`
- reject encrypted, unreadable or over-limit nested entries
- reject nested archive entries containing nested archives
- bound nested archive bytes, inner image bytes, entry count, expanded pages
  and compression ratio
- keep source archives read-only
- never expose raw outer or inner entry names as API ids

### Deferred: Nested RAR/CBR

Nested RAR/CBR should not be added in the same slice. ADR-0012 accepts a later
temp-file-backed image-chapter implementation because:

- RAR4 and RAR5 use different libraries.
- RAR5 support is conditional on native-backed availability.
- The current direct RAR extractors do not enforce the same nested byte/cache
  policy as `ZipExtractor`.
- Serving nested RAR pages needs bounded temp extraction or an equivalent
  cache-backed reader path.

### Deferred: 7Z

ADR-0013 accepts a later top-level 7Z/CB7 implementation before nested 7Z can
be considered:

- Add scanner extension support behind the existing comic-archive scan gate.
- Add `MediaType` and a dedicated `SevenZExtractor`.
- Prefer an Apache Commons Compress spike first; revisit native/libarchive only
  if Commons Compress cannot satisfy safety requirements.
- Add encrypted, unsupported-compression, path, duplicate-entry and size-limit
  tests.

### Deferred: Folder Books With Mixed Files

A directory that contains top-level TXT/PDF files plus nested image chapter
archives is not the same thing as the existing `.shelfview-book` image-directory
model. Treating that folder as one mixed book would need a new ADR because it
changes scanning identity, file fingerprinting, ordering, progress and missing
file lifecycle. ADR-0014 now plans this as a later explicit
`.shelfview-mixed-book` model.

## Proposed Follow-Up Tickets

1. `SV-997` (complete): Mixed ZIP one-level nested ZIP/CBZ image chapters.
2. `SV-998` (complete): Nested RAR/CBR support spike and ADR.
3. `SV-999` (complete): 7Z top-level support spike and ADR.
4. `SV-1000` (complete): Mixed folder-book model ADR for TXT/PDF plus image chapter
   archives.

## Non-Goals

- Do not send archives to the browser for automatic extraction.
- Do not add 7Z scanning as a hidden side effect of ZIP work.
- Do not treat nested archive entry paths as public API identifiers.
- Do not support depth greater than one without a new ADR.
- Do not broaden mixed ZIP support beyond the tested one-level ZIP/CBZ image
  chapter path without another ADR or spike.

## Verification Targets For SV-997

- Mixed ZIP: `intro.txt`, `bonus.pdf`, `ch01.cbz`, `ch02.zip`.
- Nested chapter pages naturally sort by chapter name and inner image name.
- Nested non-image archive inside a mixed ZIP is rejected with a clear error.
- Nested archive over byte, entry-count, page-count or compression-ratio limits
  is rejected.
- Existing top-level mixed ZIP TXT/PDF/image/GIF tests still pass.
- Existing image-only nested ZIP/CBZ fixed-page tests still pass.
