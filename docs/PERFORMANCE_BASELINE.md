# ShelfView Performance Baseline — Methodology (SV-801)

This document defines **how** to measure ShelfView performance and records the
performance-relevant controls already in the code. Actual numbers are filled in
from a real run (see the results tables — values marked `TBM` are "to be
measured"). Acceptance thresholds marked `TBC` are "to be confirmed by owner".

> Scope per SV-801: large archives, large TXT, mobile memory, and page-request
> cancellation / prefetch behavior.

## Performance-relevant controls already in place

These bound work per request and are the design baseline the measurements should
confirm:

- **Per-request size/shape caps** (`ShelfViewArchiveProperties`,
  `ShelfViewTextProperties`): entries ≤ 10,000; image ≤ 50 MiB and ≤ 20,000 px per
  side and ≤ 1e8 pixels; PDF ≤ 100 MiB and ≤ 1,000 pages; TXT ≤ 5 MiB and line ≤
  20,000 chars.
- **Streaming reads with running byte ceilings** (`readEntryBytes`,
  `copyEntryToFile`, `readImageBytes`): memory per request is bounded by the cap,
  not by the declared entry size.
- **Bounded temp PDF cache** (`ShelfViewZipMixedContentProvider`): Caffeine,
  max 64 entries, 30-min expiry, deletes the temp file on eviction.
- **Frontend prefetch with cancellation** (`FixedPageAdapter.prefetchNextPage` /
  `cancelPrefetch`, `defaultShelfViewPagePreloader`): only the next page is
  prefetched; moving page or destroying the adapter cancels the in-flight image
  load (`image.src = ''`).
- **Stale-response guards** (`ShelfViewReaderShell`): `setupRequestId` ignores late
  setup results after navigation/destroy; `pageImageReloadNonce` controls image
  reloads. The `TextReflowAdapter` loads the whole text document once (≤ 5 MiB).

## Measurement environment

Record for every run so numbers are comparable:

- Build/commit (`git describe`), JDK, Node, browser + version.
- Host: CPU, RAM, storage type; container limits if applicable.
- Network throttling profile (none / "Fast 3G" / "Slow 4G" for mobile cases).
- Data set used (see scenarios).

## Tooling

- **API latency / throughput**: `curl -w` for single-request timing; a small
  `k6`/`ab` script for concurrency (no new repo dependency — run ad hoc).
- **Server memory/GC**: `/actuator/metrics/jvm.memory.used`,
  `/actuator/metrics/jvm.gc.pause` (actuator is exposed; health is unauthenticated,
  metrics require ADMIN).
- **Browser**: DevTools Performance panel (frame/timing), Memory panel (JS heap
  snapshot), Network panel (request cancellation), and `performance.memory` where
  available.

## Scenarios and metrics

### S1 — Large archive (mixed ZIP)

Data: a ZIP near the limits (e.g. ~9,000 image entries, a 1,000-page PDF item, a
large image near 50 MiB / high megapixels).

| Metric | Target | Result |
| --- | --- | --- |
| Manifest latency (cold) | TBC | TBM |
| First page image latency | TBC | TBM |
| Server peak heap during manifest+page | TBC | TBM |
| Rejected oversize entry returns 4xx fast | yes | TBM |

### S2 — Large TXT

Data: a TXT near 5 MiB (UTF-8 and EUC-KR variants).

| Metric | Target | Result |
| --- | --- | --- |
| `GET /text` latency | TBC | TBM |
| Reader time-to-first-paint after fetch | TBC | TBM |
| Browser peak JS heap | TBC | TBM |
| Over-limit TXT returns 413 fast | yes | TBM |

### S3 — Mobile memory

Data: S1/S2 data on a constrained profile (e.g. throttled CPU 4×, mobile UA, low
device-memory emulation).

| Metric | Target | Result |
| --- | --- | --- |
| Peak JS heap (continuous scroll, 50 pages) | TBC | TBM |
| No unbounded growth navigating 100 pages | yes | TBM |
| Text scroll stays responsive (no long tasks > 50 ms sustained) | TBC | TBM |

### S4 — Page request cancellation / prefetch

Procedure: open a fixed-page book, rapidly press Next several times, observe the
Network panel.

| Check | Expected | Result |
| --- | --- | --- |
| Prefetch issued for next page only | yes | TBM |
| Rapid navigation cancels stale prefetch (canceled requests visible) | yes | TBM |
| Closing the reader cancels in-flight image load | yes | TBM |
| No duplicate full-resolution loads of the same page | yes | TBM |

## Procedure

1. Build and run the app (`./gradlew :komga:bootRun` or the SV-701 image) with a
   prepared library containing the S1/S2 data sets.
2. Warm up once, then take 3 measurements per metric and record the median.
3. Fill the `TBM` cells; set/confirm `TBC` targets with the owner.
4. If a metric misses its target, capture a profile (DevTools trace / heap
   snapshot / GC log) and open a follow-up ticket — do not weaken the caps.

## Status

Methodology and existing-control inventory complete. Measurements (`TBM`) and
owner targets (`TBC`) are pending a real run on a representative host; this ticket
stays open until the results tables are filled.
