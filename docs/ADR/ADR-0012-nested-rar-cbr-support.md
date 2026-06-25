# ADR-0012: Nested RAR/CBR Image Chapters

- Status: Accepted for planning
- Date: 2026-06-25
- Ticket: SV-998 (Nested RAR/CBR support spike and ADR)
- Supersedes: none
- Related: [ADR-0003](ADR-0003-zip-internal-mixed-content.md), [ADR-0010](ADR-0010-nested-image-archives-in-fixed-zip.md), `docs/SV-996_ARCHIVE_EXPANSION_TRIAGE.md`

## Context

SV-980 and SV-997 support one-level nested ZIP/CBZ image chapters. Real owner
archives may also contain RAR/CBR chapter archives. RAR is riskier than ZIP in
this codebase because RAR4 and RAR5 use different libraries and RAR5 support is
conditional on the native-backed reader being available.

The current code map is:

1. `RarExtractor.kt`
   - Uses `com.github.junrar.Archive` for `RAR_GENERIC` and `RAR_4`.
   - Opens a top-level source `Path` as a file.
   - Rejects password-protected, solid and multi-volume RAR archives.
   - Reads direct entries into memory before MIME and optional dimension
     analysis.
   - Does not expose a bounded nested-entry reader or byte-backed archive
     opening path.
2. `Rar5Extractor.kt`
   - Uses `com.github.gotson.nightcompress.Archive` for `RAR_5`.
   - Registers only when `Archive.isAvailable()` succeeds.
   - Reads a top-level source `Path` and direct entries.
   - Depends on native/libarchive availability in the runtime image.
3. `ZipExtractor.kt`
   - Already has one-level nested ZIP/CBZ support for image-only fixed-page ZIP
     books.
   - Uses a memory-backed nested ZIP reader and bounded image bytes.
4. `ShelfViewZipMixedContentProvider.kt`
   - SV-997 now supports nested ZIP/CBZ image chapters in mixed ZIPs.
   - It still rejects unsupported nested archive formats such as RAR/CBR/7Z.
5. `docs/SECURITY_AND_LICENSE.md`
   - Requires path, size, ratio, nesting, entry-count and pixel limits for
     archive content.
   - Requires ADR/owner review for a server-side archive extraction strategy
     change.

## Decision

Plan nested RAR/CBR as a separate implementation slice, not as a side effect of
ZIP support.

The accepted direction is a bounded temp-file-backed nested archive service:

1. Only one nested level is allowed.
2. Supported nested formats for this follow-up are `rar` and `cbr`.
3. Nested RAR/CBR entries are image chapters only. TXT, PDF, EPUB, HTML or
   another archive inside the nested RAR/CBR is rejected.
4. The outer archive can be ZIP/CBZ or RAR/CBR, but the first implementation
   should prefer the narrower path that has test fixtures and owner examples.
5. The server reads only the selected nested archive entry from the outer
   archive, checks its declared and streamed byte limits, then writes it to a
   temporary derived file under cache/temp storage.
6. The temporary nested RAR/CBR file is opened through the existing RAR
   extractor path so the code does not fork separate RAR parsing rules.
7. RAR4 and RAR5 stay separate:
   - RAR4 uses `RarExtractor` and keeps the existing encrypted, solid and
     multi-volume rejection.
   - RAR5 uses `Rar5Extractor` only when the native-backed reader is available.
     If it is unavailable, nested RAR5 returns a clear unsupported error.
8. The nested temporary file must be deleted after request processing or stored
   in a bounded, evicting derived cache keyed by source fingerprint and nested
   item id.
9. API ids remain opaque. Raw outer or inner archive paths are never used as
   route parameters.

## Required Limits

A later implementation must enforce at least these limits before serving an
inner image:

- outer archive entry path validation
- nested archive extension allow-list: `rar`, `cbr`
- one nested level only
- nested archive byte limit
- nested archive entry-count limit
- inner image byte limit
- compression-ratio or expanded-size equivalent where the library exposes it
- MIME/extension match for inner image entries
- image width, height and pixel limits
- encrypted, solid, multi-volume or unreadable RAR rejection
- temp file lifetime and cache-size limit

If a RAR library cannot expose a meaningful compression-ratio signal, the
implementation must compensate with strict streamed byte caps, entry-count caps
and image dimension caps, and record that residual risk in tests and docs.

## Consequences

Positive:

- Reuses the existing RAR4/RAR5 extractors instead of inventing a second RAR
  parser.
- Keeps ZIP, mixed ZIP and RAR behavior aligned at the reader contract level:
  a nested chapter is just a fixed-page item.
- Avoids adding a new dependency in the planning slice.
- Keeps source media read-only.

Negative:

- Temp-file handling and cleanup make implementation more complex than nested
  ZIP/CBZ.
- RAR5 availability can differ between local development and Docker/runtime.
- Existing RAR extraction reads direct entry bytes into memory; the follow-up
  must add bounded reads before broadening support.
- Solid or multi-volume owner archives remain unsupported.

## Rejected Alternatives

- **Open nested RAR bytes directly in memory now.** Rejected because the current
  RAR code paths are path-oriented and RAR5 is native-backed/conditional.
- **Extract the whole outer archive.** Rejected because it increases disk use,
  cleanup risk and attack surface.
- **Treat nested RAR/CBR as ordinary files in mixed ZIP manifests.** Rejected
  because that would expose unsupported content as readable before validation.
- **Add nested RAR/CBR together with 7Z.** Rejected because 7Z needs its own
  scanner/media type/dependency decision.

## Follow-Up Implementation Shape

The next implementation ticket should add a small internal abstraction such as:

```kotlin
interface NestedImageChapterReader {
  fun supports(format: NestedArchiveFormat): Boolean
  fun listImages(source: Path): List<NestedImageChapterPage>
  fun readImage(source: Path, pageIndex: Int): TypedBytes
}
```

That shape is illustrative, not a committed API. The important part is to keep
ZIP/CBZ, RAR4 and RAR5 validation rules testable without spreading archive
format checks through controllers.

## Verification Targets

A later implementation should add or generate fixtures for:

- ZIP containing `ch01.cbr` with PNG/JPG pages.
- RAR4 containing `ch01.cbr` with PNG/JPG pages, if fixture creation is
  practical.
- Nested RAR with text-only content rejected.
- Nested RAR containing another archive rejected.
- Encrypted, solid or multi-volume nested RAR rejected.
- RAR5 nested chapter skipped or rejected clearly when native support is not
  available.
- Existing top-level RAR/CBR image tests unchanged.
- Existing SV-980 and SV-997 nested ZIP/CBZ tests unchanged.

## Revisit Triggers

- The RAR library cannot provide enough safety signals for bounded extraction.
- Real owner samples require solid or multi-volume RAR.
- Real owner samples require nested RAR inside mixed TXT/PDF books before the
  fixed-page path is ready.
- A future archive abstraction can support ZIP/RAR/7Z with consistent streaming
  bounds without temp files.
