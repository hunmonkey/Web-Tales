# ADR-0018: Reader sweep deferred decisions

- Status: Proposed
- Date: 2026-08-04
- Ticket: SV-1110 (umbrella for the six items below)
- Related: `docs/BACKLOG.md`, `docs/ADR/ADR-0004-epub-bridge.md`,
  `docs/ADR/ADR-0009-single-page-text-pagination.md`,
  `docs/ADR/ADR-0016-browser-local-file-opening.md`,
  `docs/SECURITY_AND_LICENSE.md`, `Rule_UIUX.txt`

## Context

The 2026-08-04 reader analysis produced 60 findings across five dimensions.
Thirty-one of them shipped as SV-1080..SV-1109. Six did not, because each one
either changes a decision the owner already made, changes a contract a previous
ADR reviewed, or cannot be settled without a policy choice. This ADR records
them together so none of them is lost, and so the owner decides them
deliberately rather than as a side effect of a sweep.

Nothing in this ADR is implemented. Each item needs its own SV ticket once the
owner picks an option.

## Decision 1: Two-device position conflict

**Problem.** `ShelfViewReaderStateDao.savePosition` writes with an unconditional
`onDuplicateKeyUpdate`, so the last write wins with no comparison. Reading is
already careful — `ShelfViewPositionController.getPosition` compares
`talesPosition.updatedAt` against `savedPosition.updatedAt` and picks the newer
one — so the merge concept exists on the read side and is missing on the write
side. A phone that read to page 300 loses to a desktop tab still sitting on
page 5 when that tab is closed.

**Why it is not a sweep item.** Choosing a conflict policy changes what the
server accepts, needs a DTO field, and can reject writes that succeed today.

**Options.**

1. *Newest timestamp wins, checked on the server.* Add an optional
   `baseUpdatedAt` to `ShelfViewPositionUpdateDto`, add
   `.where(POSITION.UPDATED_AT.le(baseUpdatedAt))` to the DAO, return `409` on a
   zero-row update, and have the reader re-read and tell the reader that another
   device is further along. Optional field keeps old clients working.
2. *Furthest progression wins.* Simpler to reason about but wrong for a reader
   who deliberately goes back, and wrong for a re-read.
3. *Keep last-write-wins.* Free, and the failure is rare for a single-device
   reader.

**Recommendation.** Option 1. It is the only one that never silently discards
reading a person actually did.

## Decision 2: EPUB inside the ShelfView reader

**Problem.** EPUB shows a bridge screen and hands off to `views/EpubReader.vue`
(D2Reader). That reader has its own typography, and shares none of ShelfView's
fonts, themes, dictionary, highlights, bookmarks or read-aloud. Half of the
long-form reading in the product is therefore a different product.
`contentsAvailable` also returns `false` on the bridge, so the EPUB contents the
backend already produces (`ShelfViewEpubContentsProvider.listContents`) are never
shown.

**Why it is not a sweep item.** Rendering EPUB inside the shell means taking on
a reflow engine. `CLAUDE.md` requires the fallback reader to stay until the new
reader is proven, and ADR-0004 set the bridge deliberately.

**Options.**

1. *Two small slices now, full integration later.* Show the EPUB contents on the
   bridge screen and show the saved position and progress there, so the handoff
   is informed. Leaves the reader split.
2. *Render EPUB spine documents in the shell* using the existing Readium CSS,
   reusing the text adapter's locator model. Large, and the locator schema for
   EPUB has to be designed before any position is stored.
3. *Keep the bridge as is.*

**Recommendation.** Option 1 now as its own ticket; option 2 only as a separate
ADR with a locator design attached.

## Decision 3: Offline reading

**Problem.** `docs/BACKLOG.md` records an offline PWA as done, but
`public/shelfview-service-worker.js` skips every `/api/` request, so only the
app shell is cached. Worse, `flushPositionSave` returns early when offline
without queueing, so a session read on a train is lost if the tab closes before
the connection returns.

**Why it is not a sweep item.** Real offline reading means deciding what gets
downloaded, how much storage a user may consume, and how that interacts with the
source-media read-only rule and library permissions.

**Options.**

1. *Protect the position only.* Write `{bookId, locator, progression, savedAt}`
   to `localStorage` when offline, and on open prefer it over the server value
   when it is newer, then resend. No backend change, no storage policy needed.
   Fixes the data-loss half without promising offline reading.
2. *Cache pages for the open book* with a bounded quota.
3. *Explicit per-book download.* The most useful and the largest.

**Recommendation.** Option 1 as its own ticket, and correct the BACKLOG entry to
say "app shell only" either way, because the current wording overpromises.

## Decision 4: Automatic margin trimming

**Problem.** Scanned comics carry black borders and white margins, so the
picture occupies roughly 70% of the screen. Neither auto-crop nor split-wide-page
exists anywhere in the codebase.

**Why it is not a sweep item.** `docs/SECURITY_AND_LICENSE.md` lists
"client-side WebGL/Canvas image-processing pipeline" under *stop and escalate*.
Adding a crop stage is exactly that.

**Options.**

1. *Client-side crop in `ShelfViewCanvasImagePage`.* Add a pure
   `shelfViewDetectContentBounds(imageData, tolerance)` and draw with a source
   rectangle. Cheap to test, but the CPU budget added in SV-1096 already shows
   this path is main-thread bound.
2. *Server-side crop* on the page endpoint. Keeps the client cheap and works for
   the thumbnail strip too, but changes a cached image endpoint and needs
   cache-key handling.
3. *Do not add it.*

**Recommendation.** Option 1 behind the existing filter-preset list, with the
detection written as a pure function so the evidence pass from SV-993 can be
repeated against it.

## Decision 5: Local CBZ lazy extraction

**Problem.** `openShelfViewLocalArchiveFile` extracts every image entry up front,
on the main thread, before the first page is shown. The limits allow 1000 images
and 512 MiB, so a large local CBZ freezes the tab and can kill it on a phone.

**Why it is not a sweep item.** The obvious fix — turning zip.js web workers back
on — is blocked: `useWebWorkers: false` is a recorded dependency-review decision
in ADR-0016 and `docs/SECURITY_AND_LICENSE.md`. The other fix, lazy extraction,
changes the object-URL lifecycle that ADR-0016 lists under its tested coverage.

**Options.**

1. *Lazy extraction.* Return pages exposing `getUrl()`, keep the zip reader open
   until `close()`, and hold object URLs only for the current page and its
   neighbours. No dependency-behaviour change, so ADR-0016's review stands; the
   ADR's cleanup section needs updating.
2. *Re-review web workers* and enable them if the review passes.
3. *Lower the limits* so the freeze is shorter.

**Recommendation.** Option 1, with an ADR-0016 amendment rather than a new
dependency review.

## Decision 6: Touch zoom versus the centre tap

**Problem.** There is no pinch zoom and no double-tap zoom; the only way to
enlarge a panel on a phone is a slider three panels deep. SV-1101 added keyboard
zoom, which does nothing for touch.

**Why it is not a sweep item.** Double-tap zoom collides with the centre tap that
toggles the chrome — an interaction the owner confirmed twice, in SV-1066 and
SV-1072. Implementing it during the sweep silently changed that behaviour and a
regression test caught it, so it was reverted.

**Options.**

1. *Double-tap zoom outside the centre zone only.* Keeps the centre tap exact,
   but the side zones page, so a double tap there turns two pages first.
2. *Delay the single tap by ~300ms* to tell the two apart. Correct, and adds
   perceptible latency to the chrome toggle and to paging.
3. *Pinch zoom only.* No conflict with any tap zone, because it needs two
   fingers. Needs `touches[1]` handling and a scale gesture in `reader-input.ts`;
   `SV-1080` already ignores multi-touch, so the hook is in place.
4. *Leave it.*

**Recommendation.** Option 3. It is the gesture readers actually reach for, and
it is the only one that does not trade away an interaction already decided.

## Consequences

- Six known reader gaps stay open and are attributable, rather than being closed
  quietly or fixed in a way that reverses an earlier owner decision.
- `docs/BACKLOG.md` gains the six items with the option chosen, once the owner
  picks.
- ADR-0016 needs an amendment if Decision 5 option 1 is taken.
