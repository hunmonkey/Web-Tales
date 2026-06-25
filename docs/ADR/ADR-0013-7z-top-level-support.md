# ADR-0013: Top-Level 7Z/CB7 Archive Support

- Status: Accepted for planning
- Date: 2026-06-25
- Ticket: SV-999 (7Z top-level support spike and ADR)
- Supersedes: none
- Related: [ADR-0010](ADR-0010-nested-image-archives-in-fixed-zip.md), [ADR-0012](ADR-0012-nested-rar-cbr-support.md), `docs/SV-996_ARCHIVE_EXPANSION_TRIAGE.md`

## Context

Top-level 7Z/CB7 comic archives are currently unsupported. This is not just a
scanner-extension gap. A real implementation would affect scan gates, media
type detection, extractor selection, page serving, test fixtures and security
limits.

The current code map is:

1. `FileSystemScanner.kt`
   - `scanCbx` currently scans `cbz`, `zip`, `cbr` and `rar`.
   - It does not scan `7z` or `cb7`.
2. `MediaType.kt`
   - Has `ZIP`, `RAR_GENERIC`, `RAR_4`, `RAR_5`, `EPUB`, `PDF`, `TXT` and
     `IMAGE_DIRECTORY`.
   - Has no 7Z/CB7 media type.
3. `BookAnalyzer.kt`
   - Routes `MediaProfile.DIVINA` books through registered `DivinaExtractor`
     instances by media type string.
4. `DivinaExtractor.kt`
   - A new 7Z extractor can fit the existing `getEntries` and `getEntryStream`
     contract if it can provide bounded reads.
5. Existing extractors:
   - `ZipExtractor.kt` handles `MediaType.ZIP`.
   - `RarExtractor.kt` and `Rar5Extractor.kt` handle RAR variants.
   - There is no `SevenZExtractor`.
6. Dependencies:
   - `tales/build.gradle.kts` already includes
     `org.apache.commons:commons-compress:1.27.1`, which is the same library
     used by the ZIP extractor.
   - `nightcompress` and `libarchive-dev` are present for the conditional RAR5
     path, but the current code uses them only for RAR5.
7. Tests:
   - `BookAnalyzerTest` currently asserts `7zip.7z` and
     `7zip-encrypted.7z` are detected as `application/x-7z-compressed` but
     remain `UNSUPPORTED`.

## Decision

Plan top-level 7Z/CB7 support as its own implementation slice.

The accepted direction is:

1. Add a dedicated 7Z/CB7 `MediaType` with `MediaProfile.DIVINA`.
2. Add a `SevenZExtractor` instead of folding 7Z logic into ZIP or RAR
   extractors.
3. Treat 7Z/CB7 as a comic archive under the existing `scanCbx` library gate
   after implementation tests pass. Do not add another library database flag
   unless owner feedback shows that 7Z needs separate opt-in behavior.
4. Prefer a no-new-dependency first spike using Apache Commons Compress
   `SevenZFile`, because Commons Compress is already a direct dependency.
5. If Commons Compress cannot expose enough safety information or reliable
   bounded reads for the test cases, revisit NightCompress/libarchive before
   implementing runtime support.
6. First support is top-level image archives only. Mixed 7Z, nested 7Z, 7Z
   inside ZIP/RAR and 7Z containing TXT/PDF remain out of scope.
7. Existing unsupported 7Z tests must be replaced with explicit ready/error
   tests only when the extractor is implemented.

## Required Limits

A later implementation must enforce at least these limits:

- accepted extensions: `7z`, `cb7`
- detected media type: `application/x-7z-compressed` or an explicitly recorded
  equivalent from the local detector
- entry path validation: no absolute path, drive path, blank segment, `.` or
  `..`
- duplicate normalized entry rejection
- encrypted archive rejection
- unsupported compression method rejection with a clear error
- entry count limit
- per-image expanded byte limit
- total selected expanded byte or page-count limit
- compression-ratio or equivalent expanded-size guard where exposed
- MIME/extension image match
- image width, height and pixel limits
- no raw server path or raw archive path as API id

If the extractor library cannot expose solid/encrypted/compression-ratio
signals consistently, the first implementation must stay conservative and
return `UNSUPPORTED` for that case rather than guessing.

## Consequences

Positive:

- Keeps 7Z support in the same fixed-page reader contract as ZIP/RAR comic
  archives.
- Avoids a new dependency unless the extractor spike proves Commons Compress
  cannot meet the limits.
- Keeps scanner behavior simple by using the existing comic archive scan gate.
- Leaves nested 7Z and mixed 7Z out of the first slice.

Negative:

- Adding 7Z under `scanCbx` means libraries that already scan comic archives
  may start seeing valid 7Z books after the implementation ships.
- 7Z page serving may need to reopen and iterate the archive to reach a page,
  which can be slower than ZIP random access.
- Some real 7Z archives may remain unsupported if encryption, compression
  method or solid-archive safety signals are insufficient.

## Rejected Alternatives

- **Add `7z` to `scanCbx` immediately without an extractor.** Rejected because
  it would only create unsupported or broken media records.
- **Treat 7Z as ZIP.** Rejected because the archive format and safety controls
  are different.
- **Add nested 7Z at the same time.** Rejected because top-level support must
  be proven before 7Z can safely appear inside another archive.
- **Add a new `scan7z` database flag now.** Rejected for the planning slice.
  A separate flag is only worth the extra migration and UI surface if owner
  feedback shows that 7Z needs different scan behavior from other comic
  archives.

## Follow-Up Implementation Shape

The next implementation ticket should be small:

1. Add `MediaType.SEVEN_Z` or equivalent naming.
2. Add scanner extensions `7z` and `cb7` behind the `scanCbx` gate.
3. Add `SevenZExtractor` with bounded reads and natural sort.
4. Add fixture tests for ready image-only 7Z, encrypted 7Z, bad entry path,
   duplicate normalized names, non-image-only content and over-limit images.
5. Update ShelfView manifest source-format mapping only after the media type
   and extractor tests pass.

## Verification Targets

A later implementation should verify:

- `7zip.7z` with image pages becomes `READY`.
- `7zip-encrypted.7z` becomes `UNSUPPORTED` or a clear controlled error.
- `cb7` extension scans under `scanCbx`.
- Existing ZIP/RAR tests still pass.
- Existing SV-980/SV-997 nested ZIP/CBZ behavior is unchanged.
- 7Z does not appear as nested content until a separate ADR accepts it.

## Revisit Triggers

- Commons Compress cannot safely read the needed owner 7Z samples.
- Real owner 7Z archives require compression methods not supported locally.
- Users need 7Z scanning separately switchable from ZIP/RAR comic scanning.
- Nested 7Z becomes a real owner requirement after top-level 7Z is stable.
