# ShelfView Threat Model (SV-800)

This is a code-grounded threat model for the ShelfView additions on top of Tales
`1.24.4`. It covers assets, trust boundaries, the attack surface, and the threats
and mitigations for the archive, TXT, API and cache paths, with residual risks.

Constants cited below are the defaults from
`ShelfViewArchiveProperties` (`shelfview.archive.*`) and
`ShelfViewTextProperties` (`shelfview.text.*`); they are operator-tunable.

## Method and scope

- In scope: the `org.tales.shelfview` backend (manifest, content delivery,
  position/bookmark/preference state) and the `tales-webui/src/shelfview` reader.
- Out of scope (inherited from Tales upstream and assumed sound): the
  authentication/session/API-key stack, the library scanner identity model, TLS
  termination (delegated to the reverse proxy, see `docs/UNRAID_DEPLOYMENT.md`).
- Assumption: operators mount source media read-only and run the SV-701 image as
  the non-root user.

## Assets

- Source media files (books): confidentiality and integrity (read-only).
- Per-user reading state: positions, bookmarks (incl. labels), preferences.
- The server host: filesystem, memory, temp/cache space, process privileges.
- Service availability for legitimate logged-in users.
- Secrets: tokens, credentials (must never reach repo, logs or image layers).

## Trust boundaries

1. Browser ⇄ ShelfView API: every request is untrusted until authenticated and
   authorized. All Locator/Manifest/preference payloads are untrusted input.
2. ShelfView ⇄ stored media: file contents, archive entries, filenames, image
   bytes, TXT bytes and metadata are **untrusted data**, even though they live on
   the server.
3. ShelfView ⇄ host cache/temp: derived output (rendered PDF pages, thumbnails)
   is written to bounded, server-controlled locations only.

## Attack surface (entry points)

All under `/api/shelfview/v1` and each controller authenticates
(`@AuthenticationPrincipal TalesPrincipal`) and authorizes
(`ContentRestrictionChecker.checkContentRestriction`) — verified across all 9
controllers:

- `GET .../books/{id}/manifest`
- `GET .../books/{id}/text`, `GET .../books/{id}/mixed/{itemId}/text`
- `GET .../books/{id}/mixed/{itemId}/pages/{page}`
- `GET .../books/{id}/image-directory/pages/{page}`
- `GET/PATCH .../books/{id}/position`
- bookmark CRUD, preference get/save
- Reused Tales page API for plain fixed-page books: `/api/v1/books/{id}/pages/{n}`

## Threats and mitigations

### Archive content (ZIP/CBZ mixed: TXT/PDF/JPG/PNG/WEBP/GIF)

Implemented in `ShelfViewZipMixedContentProvider`.

| Threat | Mitigation |
| --- | --- |
| Zip-slip / path traversal via entry name | `normalizeEntryName` rejects absolute paths, Windows drive paths, `.`/`..` segments, control chars, backslashes; entries resolved by normalized name, never written by attacker-controlled path |
| Nested archive (archive bomb chaining) | `isNestedArchive` rejects `zip/cbz/cbr/rar/7z` entries (`SHELFVIEW_ARCHIVE_NESTED_ARCHIVE`) |
| Too many entries / metadata objects | `maxEntries` = 10,000; ZIP central directories are scanned in O(1) memory before Commons Compress construction, while RAR4 limits the exact header reads performed by junrar (`SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES`). Central-directory bytes default to 32 MiB and parser-construction reads to 64 MiB. |
| Decompression bomb (oversized entry) | Streaming reads enforce caps even when the declared size is unknown: text `maxBytes` (5 MiB), image `maxImageBytes` (50 MiB), PDF/nested archive `100 MiB`, cumulative uncompressed bytes `2 GiB`, and compression ratio `100:1`. |
| Pixel-flood image | `maxImageWidth`/`maxImageHeight` = 20,000 and `maxImagePixels` = 100,000,000 |
| Oversized PDF render | `maxPdfPages` = 1,000; scaled dimensions are checked before PDFBox allocates the rendered image and encoded output is capped at `maxImageBytes`. |
| MIME confusion (extension vs bytes) | `ContentDetector.detectMediaType` must match the extension allow-list; PDF re-detected before render (`SHELFVIEW_*_MIME_MISMATCH`) |
| Duplicate-entry ambiguity | Duplicate normalized names rejected (`SHELFVIEW_ARCHIVE_DUPLICATE_ENTRY`) |
| Item-reference forgery | Items addressed by SHA-256 `itemId` derived from book fingerprint + normalized name + size |

### TXT

Implemented in `ShelfViewTextContentProvider`.

- Size cap `maxBytes` = 5 MiB (`SHELFVIEW_TEXT_TOO_LARGE`). Standalone TXT uses
  one authorized no-follow channel for both the size fast-path and a bounded
  read of at most `maxBytes + 1`, so growth after the metadata query cannot
  trigger an unbounded allocation.
- The stored TXT path must resolve beneath its owning library's real root.
  Final symbolic links, escaped intermediate links, post-registration link
  replacement, and an authorization/raw-book library-id mismatch are rejected.
- Line-length cap `maxLineLength` = 20,000 (`SHELFVIEW_TEXT_LINE_TOO_LONG`).
- Strict decoding (`CodingErrorAction.REPORT`) for UTF‑8, then EUC‑KR fallback; an
  explicit unsupported/undecodable charset is rejected, not silently mangled.
- Output is a list of paragraphs (plain strings); the reader renders them as text
  interpolation, **never** `v-html` — XSS via TXT content is structurally avoided.

### Image directory books

Implemented in `ShelfViewImageDirectoryContentProvider`.

- Page filename must be a bare name (no `/`, `\`, no leading `.`); resolved path
  must stay directly under the normalized root (`imagePath.parent == root`).
- Symlinks are not followed (`LinkOption.NOFOLLOW_LINKS`) for the directory and
  the page file.
- MIME-vs-extension check and the same dimension/pixel/byte caps as archives.

### API, Locator and preferences

- `ShelfViewLocatorValidator` validates schema version (must be 1), kind
  (PAGE/TEXT/MIXED), `contentFingerprint` equality with the current book, numeric
  bounds (page within count, offsets in `[0,1]`, progression in `[0,1]`), and
  `normalizationVersion`. MIXED Locators are additionally bound to the current
  Manifest item's id and index, server-derived nested fingerprint, item kind,
  and nested page count before any position, bookmark, or highlight is stored.
  Item ids and fingerprints are length-bounded before archive inspection.
- Mixed Manifest metadata is cached by book fingerprint and file size,
  modification time, creation time, and filesystem key, with a 50,000-item
  weight bound and 30-minute write expiry. Normal and atomic replacements get a
  new cache key. On filesystems that expose no file key, a replacement that also
  preserves every tracked timestamp and size can remain stale until that fixed
  expiry; repeated reads do not extend the window.
- State is keyed by `principal.user.id`; one user cannot read or mutate another
  user's position/bookmarks/preferences.
- No raw server filesystem path is returned in any DTO; books are addressed by id
  and content fingerprint.

### Cache and temp

- Mixed-ZIP PDF originals are cached under a unique process-session directory
  below the system temp dir (`shelfview-mixed-pdf-cache`): bounded size (64),
  30‑min expiry, synchronous document close then file deletion, shutdown cleanup,
  and conservative stale-session cleanup on the next start. Cache roots and
  entries are checked without following symbolic links.
- Tales-managed thumbnails/derived output remain under `/config`.

## Cross-cutting controls

- Source media mounted read-only; ShelfView never writes to it.
- Container runs as non-root UID/GID 1000 (SV-701).
- DB changes are forward-only Flyway migrations (rollback handled at the appdata
  level, see `docs/UNRAID_DEPLOYMENT.md`).
- Servlet error dispatch is sanitized independently of operator-configurable
  `server.error` flags and returns only `{status, code}`. Controller exceptions
  expose typed/stable codes or status-derived `HTTP_*` codes, never exception,
  validation, SQL, parser, filesystem, or rejected-value text.
- Application logs pass opaque entity ids, counts, and exception type names
  instead of raw throwables, full media paths, archive/parser text, SQL details,
  plaintext passwords, or session identifiers. Common model `toString()` output
  is restricted to opaque ids so indirect interpolation cannot restore details.
- Secrets kept out of repo (`Rule_PW.txt` is git-ignored), logs and image layers;
  health probe uses the unauthenticated `/actuator/health` which exposes only
  status, not details.

## Residual risks and recommendations

1. **Per-request bounded, but no global concurrency/memory ceiling.** Each request
   is capped, yet many concurrent large-archive/TXT/PDF requests can still pressure
   heap and CPU (full-file in-memory reads up to the per-type caps; PDF rendered via
   PDFBox). *Recommendation:* front with a reverse proxy that enforces rate limits
   and connection caps; consider a future server-side concurrency/size budget.
2. **Untrusted PDF parsing/rendering** relies on PDFBox robustness against
   malformed PDFs. *Recommendation:* keep PDFBox patched; treat as a watch item.
3. **The scanner still follows symbolic links.** `FileSystemScanner` walks with
   `FOLLOW_LINKS` (upstream Tales behavior), so an escaped TXT can be indexed,
   although standalone delivery now rejects it with real-root and no-follow
   checks. *Recommendation:* keep source media read-only and revisit scanner
   link-following only as a compatibility-reviewed upstream change.
4. **Archive limits are per request, not a parser sandbox.** ZIP/RAR metadata,
   body bytes, dimensions and ratios are bounded, but format libraries still
   parse attacker-controlled structures in-process. NightCompress RAR5 support
   is unavailable on the required JDK 21 with the current optional native
   artifact, so such files fail closed instead of entering an unbounded path.
   Keep archive dependencies patched and add a global extraction concurrency
   ceiling before accepting public uploads.
5. **Native image libraries** (libjxl/libheif/libwebp/libarchive) in the image are
   an external attack surface for crafted images. *Recommendation:* rebuild the
   image regularly to pick up base-image security updates.
6. **Auth model is inherited** from Tales; ShelfView trusts it. Any Tales auth
   regression affects ShelfView. Tracked as an upstream dependency.
7. **Production `noclaim` bootstrap no longer prints its random initial
   password.** This closes a credential-in-logs defect, but operators using that
   non-default profile must use the supported password-reset startup arguments
   before the generated account is usable. The normal production claim flow is
   unaffected.
8. **Legacy frontend audit findings are bounded but not zero.** The maintained
   EPUB reader removes the reachable PDF.js 2.14.305 bundle. Ten remaining
   reader findings are Node-only install-tree leaves absent from the static
   browser/JAR artifact. Vue 2's reported parser sink is absent from the runtime
   build, and the application does not supply an untrusted Vuetify preset or a
   user-defined VDatePicker title formatter. The reader distribution also
   prebundles older sanitizer/nanoid code, but Tales disables its Search/TTS
   sanitizer modules and does not call the affected zero-size generator. A
   future upstream slim reader or framework migration should remove these
   dormant findings; do not treat root npm overrides as changing prebundled code.

## Change triggers

Re-review this model (and write an ADR per `docs/SECURITY_AND_LICENSE.md`) when
adding: server-side archive extraction strategy changes, direct PDF.js
integration, public-internet uploads, an auth-model change, or source-media write
access.
