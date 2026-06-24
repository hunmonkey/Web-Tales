// Single-page TXT pagination by paragraph grouping (ADR-0009).
//
// A page is a run of consecutive whole paragraphs whose combined length fits an
// estimated per-page character budget. Paragraphs are never split; a paragraph
// longer than the budget becomes its own page. Pages are identified by their
// first paragraph index so the existing paragraph-based locator stays valid.

export interface ShelfViewTextPageMetrics {
  columnWidth: number    // rendered text column width in px (after max-width)
  viewportHeight: number // reader stage height in px
  fontSize: number       // px
  lineHeight: number     // unitless multiplier
  marginX: number        // horizontal padding per side in px
  marginY: number        // vertical padding per side in px
}

// Average glyph advance relative to font size. ShelfView text is CJK-heavy where
// glyphs are roughly full-width; spaces, punctuation and Latin runs make the
// average a little narrower, which also helps fill lines that break early.
const GLYPH_ADVANCE_RATIO = 0.95
const MIN_CHARS_PER_PAGE = 200

// Estimate how many characters comfortably fill one screen for the current text
// settings. This is a heuristic (lines-per-page times characters-per-line), not
// a DOM measurement, so it stays a pure function.
export function shelfViewEstimateCharsPerPage(metrics: ShelfViewTextPageMetrics): number {
  const lineHeightPx = metrics.fontSize * metrics.lineHeight
  const glyphWidth = metrics.fontSize * GLYPH_ADVANCE_RATIO
  if (lineHeightPx <= 0 || glyphWidth <= 0) return MIN_CHARS_PER_PAGE

  const usableHeight = metrics.viewportHeight - metrics.marginY * 2
  const usableWidth = metrics.columnWidth - metrics.marginX * 2
  const linesPerPage = Math.floor(usableHeight / lineHeightPx)
  const charsPerLine = Math.floor(usableWidth / glyphWidth)
  const estimate = linesPerPage * charsPerLine

  return Math.max(MIN_CHARS_PER_PAGE, Number.isFinite(estimate) ? estimate : 0)
}

// Build the first-paragraph index of each page. With a non-positive budget every
// paragraph is its own page, which preserves the original one-paragraph-per-page
// behaviour.
export function shelfViewBuildTextPageStarts(
  paragraphLengths: number[],
  charsPerPage: number,
): number[] {
  const count = paragraphLengths.length
  if (count === 0) return []
  if (!(charsPerPage > 0)) return paragraphLengths.map((_, index) => index)

  const starts = [0]
  let pageChars = 0
  for (let index = 0; index < count; index++) {
    const length = Math.max(0, paragraphLengths[index])
    const isPageStart = starts[starts.length - 1] === index
    if (!isPageStart && pageChars + length > charsPerPage) {
      starts.push(index)
      pageChars = length
    } else {
      pageChars += length
    }
  }
  return starts
}

// Find the page (index into pageStarts) that contains a paragraph: the last page
// whose start is at or before the paragraph.
export function shelfViewPageIndexForParagraph(
  pageStarts: number[],
  paragraphIndex: number,
): number {
  if (pageStarts.length === 0) return 0

  let low = 0
  let high = pageStarts.length - 1
  let answer = 0
  while (low <= high) {
    const mid = (low + high) >> 1
    if (pageStarts[mid] <= paragraphIndex) {
      answer = mid
      low = mid + 1
    } else {
      high = mid - 1
    }
  }
  return answer
}
