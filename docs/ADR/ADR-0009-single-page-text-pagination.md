# ADR-0009: Single-page TXT pagination by paragraph grouping

- Status: Accepted
- Date: 2026-06-24
- Ticket: none (owner request, no ticket)
- Supersedes: none
- Related: `docs/ARCHITECTURE.md`, ADR-0002, `tales-webui/src/shelfview/adapters/text-reflow-adapter.ts`

## Context

The ShelfView text (TXT) reader treats one paragraph as one page. In
`TextReflowAdapter`, `getVisibleParagraphs('SINGLE_PAGE')` returns only the
current paragraph and `next()` / `previous()` move one paragraph at a time, so
`getParagraphsCount()` is both the paragraph count and the page count.

For a real novel this means single-page mode shows one paragraph per screen.
Short paragraphs (a single line of dialogue) leave most of the screen empty, and
the page counter reads e.g. `4 / 16636`. The owner reported this as the reader
"breaking off at every paragraph" in single-page mode. Continuous-scroll mode is
unaffected because it already renders all paragraphs.

## Decision

In single-page mode, define a page as a run of consecutive whole paragraphs
whose combined length fits an estimated per-page character budget derived from
the current viewport size and text settings (font size, line height, column
width, margins).

- Paragraph boundaries are preserved: a paragraph is never split across pages.
- A paragraph longer than the budget becomes its own page (it is never cut).
- The position model stays paragraph-based. A page is identified by its first
  paragraph index, so the saved `ShelfViewTextLocator` (paragraph + character
  offset), bookmarks, highlights and progression are unchanged and remain
  backward compatible.
- Continuous-scroll mode is untouched and still renders every paragraph.
- Pagination is opt-in inside the adapter. With no budget set (or a
  non-positive budget) each paragraph is its own page, preserving the previous
  behaviour and existing adapter tests. The shell calls `setPagination()` with
  the estimated budget once content and viewport are known, and re-estimates on
  font/line-height changes and viewport resize.

The character budget is a heuristic (estimated lines-per-page times
characters-per-line). It approximates a full screen rather than measuring the
DOM, which keeps the page model a pure, testable function and avoids reflow
measurement on every setting or resize change.

## Consequences

Positive:

- Single-page mode fills the screen with multiple paragraphs and the page count
  drops from the paragraph count to a realistic number, so page turning feels
  like a book.
- No backend, API, migration, dependency or media-write change. Saved positions,
  bookmarks and highlights keep working because the locator stays paragraph-based.
- Pagination is a pure function over paragraph lengths plus a budget, so it is
  unit-testable without a DOM.

Negative:

- Pages are approximate, not pixel-perfect: a page can end slightly short or run
  a little long, especially with many short paragraphs or one very long
  paragraph.
- The per-page budget must be re-estimated when the font, line height, column
  width or viewport changes, which adds a small amount of shell wiring.

## Rejected alternatives

- **DOM-measured pagination (exact screen fill).** Rejected for this slice
  because it requires measuring rendered heights and re-paginating on every
  font/size/resize change, is hard to unit-test, and can shift saved positions.
  The owner chose paragraph-grouping over exact fill.
- **CSS multi-column / scroll-snap paging.** Rejected because mapping a column
  offset back to a paragraph locator (for bookmarks, progress and restore) is
  fragile, and column counts vary with font and viewport.
- **Fixed paragraphs-per-page.** Rejected because paragraph lengths vary wildly,
  so pages would be very uneven.

## Revisit triggers

- Owner wants exact screen-height fill or paper-book-style mid-paragraph breaks.
- Mixed-content (ZIP) text items need the same paragraph grouping; the pure
  pagination functions are written to be reusable there.
- A future EPUB/Readium reflow path replaces this text renderer.
