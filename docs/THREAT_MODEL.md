# ShelfView Threat Model (SV-800)

This is a code-grounded threat model for the ShelfView additions on top of Komga
`1.24.4`. It covers assets, trust boundaries, the attack surface, and the threats
and mitigations for the archive, TXT, API and cache paths, with residual risks.

Constants cited below are the defaults from
`ShelfViewArchiveProperties` (`shelfview.archive.*`) and
`ShelfViewTextProperties` (`shelfview.text.*`); they are operator-tunable.

## Method and scope

- In scope: the `org.gotson.komga.shelfview` backend (manifest, content delivery,
  position/bookmark/preference state) and the `komga-webui/src/shelfview` reader.
- Out of scope (inherited from Komga upstream and assumed sound): the
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
(`@AuthenticationPrincipal KomgaPrincipal`) and authorizes
(`ContentRestrictionChecker.checkContentRestriction`) — verified across all 9
controllers:

- `GET .../books/{id}/manifest`
- `GET .../books/{id}/text`, `GET .../books/{id}/mixed/{itemId}/text`
- `GET .../books/{id}/mixed/{itemId}/pages/{page}`
- `GET .../books/{id}/image-directory/pages/{page}`
- `GET/PATCH .../books/{id}/position`
- bookmark CRUD, preference get/save
- Reused Komga page API for plain fixed-page books: `/api/v1/books/{id}/pages/{n}`

## Threats and mitigations

### Archive content (ZIP/CBZ mixed: TXT/PDF/JPG/PNG/WEBP/GIF)

Implemented in `ShelfViewZipMixedContentProvider`.

| Threat | Mitigation |
| --- | --- |
| Zip-slip / path traversal via entry name | `normalizeEntryName` rejects absolute paths, Windows drive paths, `.`/`..` segments, control chars, backslashes; entries resolved by normalized name, never written by attacker-controlled path |
| Nested archive (archive bomb chaining) | `isNestedArchive` rejects `zip/cbz/cbr/rar/7z` entries (`SHELFVIEW_ARCHIVE_NESTED_ARCHIVE`) |
| Too many entries (entry-count DoS) | `maxEntries` = 10,000 (`SHELFVIEW_ARCHIVE_TOO_MANY_ENTRIES`) |
| Decompression bomb (oversized entry) | Streaming reads enforce caps even when the declared size is unknown: text `maxBytes` (5 MiB), image `maxImageBytes` (50 MiB), PDF `maxPdfBytes` (100 MiB) |
| Pixel-flood image | `maxImageWidth`/`maxImageHeight` = 20,000 and `maxImagePixels` = 100,000,000 |
| Oversized PDF page count | `maxPdfPages` = 1,000 (`SHELFVIEW_PDF_TOO_MANY_PAGES`) |
| MIME confusion (extension vs bytes) | `ContentDetector.detectMediaType` must match the extension allow-list; PDF re-detected before render (`SHELFVIEW_*_MIME_MISMATCH`) |
| Duplicate-entry ambiguity | Duplicate normalized names rejected (`SHELFVIEW_ARCHIVE_DUPLICATE_ENTRY`) |
| Item-reference forgery | Items addressed by SHA-256 `itemId` derived from book fingerprint + normalized name + size |

### TXT

Implemented in `ShelfViewTextContentProvider`.

- Size cap `maxBytes` = 5 MiB (`SHELFVIEW_TEXT_TOO_LARGE`), checked by stat and on read.
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
  `normalizationVersion`. Stale/forged locators are rejected.
- State is keyed by `principal.user.id`; one user cannot read or mutate another
  user's position/bookmarks/preferences.
- No raw server filesystem path is returned in any DTO; books are addressed by id
  and content fingerprint.

### Cache and temp

- Rendered mixed-ZIP PDF pages are cached under the system temp dir
  (`shelfview-mixed-pdf-cache`) via a Caffeine cache: bounded size (64), 30‑min
  expiry, and a removal listener that deletes the temp file. Temp files are also
  deleted on any failure path.
- Komga-managed thumbnails/derived output remain under `/config`.

## Cross-cutting controls

- Source media mounted read-only; ShelfView never writes to it.
- Container runs as non-root UID/GID 1000 (SV-701).
- DB changes are forward-only Flyway migrations (rollback handled at the appdata
  level, see `docs/UNRAID_DEPLOYMENT.md`).
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
3. **Scanner follows symlinks.** `FileSystemScanner` walks with `FOLLOW_LINKS`
   (upstream Komga behavior), while ShelfView content delivery does not follow
   links. *Recommendation:* document that library roots should not contain
   untrusted symlinks; consider revisiting scan link-following upstream-side.
4. **Native image libraries** (libjxl/libheif/libwebp/libarchive) in the image are
   an external attack surface for crafted images. *Recommendation:* rebuild the
   image regularly to pick up base-image security updates.
5. **Auth model is inherited** from Komga; ShelfView trusts it. Any Komga auth
   regression affects ShelfView. Tracked as an upstream dependency.

## Change triggers

Re-review this model (and write an ADR per `docs/SECURITY_AND_LICENSE.md`) when
adding: server-side archive extraction strategy changes, direct PDF.js
integration, public-internet uploads, an auth-model change, or source-media write
access.
