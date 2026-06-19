# Third-Party Notices

This file records the distribution license, runtime dependencies, and any directly
incorporated third-party source for the ShelfView / Web-Hanji fork.

## Distribution license (SV-802, confirmed)

- ShelfView is distributed under the **MIT License** (see `LICENSE`).
- The original Komga `LICENSE` and copyright (`Copyright (c) 2019 Gauthier
  Roebroeck`) are preserved unchanged.
- All ShelfView-original code (`org.gotson.komga.shelfview`,
  `komga-webui/src/shelfview`, `komga/docker/render-shelfview-dockerfile.sh`, CI
  workflow, docs) is contributed under the same MIT terms.
- No GPL/AGPL code is included, so MIT carries no copyleft obligation. Revisiting
  the distribution license is an owner decision and would require re-checking every
  dependency below.

## Dependency audit (vs base tag `1.24.4`)

ShelfView added **no new runtime dependencies**. Verified by diffing the
dependency manifests against the base tag — all unchanged (0 changed lines):

- `gradle/libs.versions.toml`
- `build.gradle.kts`, `komga/build.gradle.kts`
- `komga-webui/package.json`, `komga-webui/package-lock.json`

ShelfView features are built entirely on dependencies already shipped by Komga
`1.24.4`, whose licenses are tracked by Komga's own dependency set.

| Component | Version/Commit | License | Use | Source copied? | Notice/action |
|---|---|---|---|---|---|
| Komga | 1.24.4 / 9c84690a07aa7dcc5c54b58dd67de3fb8b9deb5d | MIT | Fork foundation | Yes, full fork | Preserve `LICENSE` and original copyright |

### Existing Komga runtime libraries reused by ShelfView (no version change)

Listed for transparency; all were already present in Komga `1.24.4` and are not new
to this fork:

- Apache Commons Compress (Apache-2.0) — ZIP/CBZ entry reading.
- Apache PDFBox (Apache-2.0) — PDF page count and rendering.
- Apache Commons IO (Apache-2.0) — filename/extension handling.
- Caffeine (Apache-2.0) — bounded temp PDF-page cache.
- `net.greypanther.natsort` natural-order comparator — natural sort of entries
  (existing Komga dependency; license tracked upstream).
- axios, Vue 2, vue-router (frontend) — reused for the ShelfView reader services.

## Directly incorporated source

- **None.** ShelfView source imports only existing Komga infrastructure and
  standard JDK/Kotlin/Vue APIs (frontend third-party imports are limited to
  `axios`, `vue`, `vue-router`). No third-party source files were copied into the
  codebase, and no AI-generated code was identified as mirroring a specific
  third-party implementation.

## CI / build tooling (not distributed in the product image)

Used only by `.github/workflows/shelfview-ci.yml`; not part of the runtime
artifact:

- `actions/checkout`, `actions/setup-java`, `actions/setup-node` (MIT)
- `gradle/actions/setup-gradle` (Apache-2.0)
- `dorny/paths-filter` (MIT)
- `docker/setup-qemu-action`, `docker/setup-buildx-action`,
  `docker/build-push-action` (Apache-2.0)

The container base images (`eclipse-temurin`, `ubuntu`) and the `kepubify` binary
are pulled at image-build time and are governed by their own upstream licenses.

## Future / reference-only (not adopted)

These are candidates recorded by earlier planning; none are currently used. Review
license and purpose before adopting any of them.

| Component | License | Status |
|---|---|---|
| PDF.js | Apache-2.0 | Not adopted (future PDF text layer) |
| zip.js | BSD-3-Clause | Not adopted (future local archive open) |
| foliate-js | MIT | Not adopted (TXT pagination research) |
| Koodo/Kookit, Kavita, BookLore | (various, incl. copyleft) | Reference-only; do not copy without owner review |
