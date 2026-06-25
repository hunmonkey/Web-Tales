# ADR-0007: Image-quality processing scope

- Status: Accepted for planning
- Date: 2026-06-20
- Ticket: SV-950b (Phase 5 WebGL/Canvas image-quality spike decision)
- Supersedes: none
- Related: `docs/BACKLOG.md` SV-950/SV-951, `docs/ARCHITECTURE.md`, `docs/SECURITY_AND_LICENSE.md`

## Context

ShelfView already has content-only fixed-page display filters. SV-919 added an
`IMAGE_ENHANCE` preset, and SV-950a added a `FADED_SCAN` preset for low-contrast
or faded scans. These presets are cheap because they use CSS variables on page
content and do not change page URLs or source media.

The remaining image-quality ideas are heavier:

- WebGL sharpening
- background cleanup
- adaptive monochrome or binarization
- possible Canvas-based per-pixel transforms
- possible derived-image caches

Those features can improve poor scans, but they can also increase browser
memory, battery use, GPU crashes, mobile instability and maintenance cost. They
may also need workers, cancellation, feature flags, fallback paths and
dependency/license review.

### Verified code map

Read directly in the current fork:

1. `tales-webui/src/shelfview/functions/image-filter-presets.ts`
   - Defines the fixed-page image filter presets and converts them into CSS
     variables.
2. `tales-webui/src/shelfview/views/ShelfViewReaderShell.vue`
   - Applies image filter styles to page content only.
   - Existing tests verify filter changes keep the page image URL stable.
3. `tales/src/main/kotlin/org/tales/shelfview/interfaces/api/ShelfViewPreferenceController.kt`
   - Validates fixed-page `imageFilterPreset` values before saving them.
4. `SHELFVIEW_MASTER_DEVELOPMENT_PLAN.md`
   - Keeps sharpening, background removal and adaptive binarization behind a
     WebGL/Canvas performance spike.
5. `docs/SECURITY_AND_LICENSE.md`
   - Requires dependency/license review before adding packages and escalation
     before source-media write access or broad processing-strategy changes.

## Decision

Do **not** add a WebGL, Canvas, AI upscaling or derived-image image-quality
pipeline in the default app during SV-950.

SV-950 is allowed to expand the existing display-only filter presets because
that path is reversible, uses no new dependency, keeps original media read-only
and keeps page URLs stable.

Any future WebGL/Canvas image-quality implementation must be a separate,
owner-approved ticket and must satisfy these gates first:

1. Define the exact processing mode: sharpening, background cleanup, adaptive
   monochrome, denoise or another named transform.
2. Prove the transform can be disabled instantly and falls back to the normal
   image element if WebGL/Canvas fails.
3. Keep source media read-only.
4. Avoid returning raw server paths or raw archive internals.
5. Avoid full-book browser extraction or full-archive browser loading.
6. Apply processing only to visible or near-visible pages, matching existing
   virtualized-reader expectations.
7. Cancel stale processing when the user pages quickly or changes settings.
8. Bound canvas dimensions, pixel counts and memory.
9. Preserve animated GIF behavior unless the owner explicitly accepts losing
   animation for a specific mode.
10. Record browser/device test evidence before enabling the feature by default.
11. Add dependency and license review before introducing any image-processing
    package.
12. Keep the existing CSS preset path as the fallback.

### Owner-approved follow-up

On 2026-06-20, the owner approved a small Canvas prototype after SV-950 closed.
SV-951 may add one `CANVAS_SHARPEN` mode with these limits:

1. Use browser-local Canvas only; do not add WebGL, AI upscaling, server-side
   derived-image caches or image-processing dependencies.
2. Keep source media read-only and page URLs unchanged.
3. Apply only to the fixed-page single/double-page display path, not continuous
   scroll.
4. Bound total pixels before reading image data.
5. Cancel stale processing when page URLs, reload nonce or mode changes.
6. Fall back to the normal `<img>` page path if Canvas load, draw or pixel read
   fails.
7. Leave browser/device performance evidence as follow-up work before expanding
   the mode or enabling heavier WebGL/background-cleanup processing.

On 2026-06-25, after the Canvas evidence pass, the owner approved the next
browser-local expansion. The implemented follow-up may add `WEBGL_SHARPEN`,
`BACKGROUND_CLEANUP` and `ADAPTIVE_MONO` with the same source-media read-only
boundary, fixed-page-only scope, normal image fallback, bounded Canvas work and
no new dependency, AI upscaler or derived-image cache.

## Consequences

Positive:

- Keeps the current reader stable and lightweight.
- Avoids surprising GPU, memory and mobile regressions.
- Keeps self-hosted source media read-only.
- Gives the owner a clear checklist before approving heavier image processing.
- Preserves the existing reversible CSS filter path.

Negative:

- SV-950 does not deliver true sharpening, background removal, denoise,
  binarization or super-resolution.
- Poor scans that need real per-pixel restoration still need a future
  implementation.
- A later spike will need runtime/browser evidence, not only unit tests.

## Future implementation hook points

Small follow-up tickets should proceed in this order:

1. Add a browser capability helper that reports whether Canvas and WebGL are
   available, without changing rendering.
2. Extend SV-951's visible-page Canvas prototype with explicit browser
   capability reporting if it graduates beyond experiment status.
3. Add cancellation and memory-bound tests for quick page turns.
4. Add a manual browser validation matrix for desktop and mobile-class devices.
5. Decide whether any processing result is purely browser-local or cached under
   `/cache`. If cached output is considered, write a separate server-side cache
   ADR first.
6. Only then expose the mode in the reader UI.

## Rejected alternatives

- **Add WebGL sharpening directly to the reader now.** Rejected because there is
  no performance evidence, fallback plan or mobile/device validation yet.
- **Add Canvas background cleanup directly to all pages.** Rejected because
  full-page per-pixel transforms can be expensive and need cancellation and
  pixel bounds.
- **Pre-generate enhanced images on the server.** Rejected for this phase
  because it introduces cache/storage policy and server CPU questions.
- **Use an AI upscaler in the default app.** Rejected for this phase because it
  adds model/dependency/runtime cost and does not fit the current lightweight
  self-hosted reader scope.
- **Remove CSS presets and wait for WebGL.** Rejected because CSS presets are
  cheap, reversible and already useful for many scans.

## Revisit triggers

- The owner chooses a specific transform and accepts the performance validation
  work.
- Browser smoke tests show the current CSS filters are not enough for a common
  library type.
- A dependency with acceptable license, bundle size and maintenance profile is
  selected and documented.
- Server-side derived image caching becomes a product requirement.
