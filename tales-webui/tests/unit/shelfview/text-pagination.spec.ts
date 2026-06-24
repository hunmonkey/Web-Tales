import {
  shelfViewBuildTextPageStarts,
  shelfViewEstimateCharsPerPage,
  shelfViewPageIndexForParagraph,
} from '@/shelfview/functions/text-pagination'

describe('ShelfView text pagination', () => {
  describe('shelfViewBuildTextPageStarts', () => {
    it('returns no pages for empty content', () => {
      expect(shelfViewBuildTextPageStarts([], 100)).toEqual([])
    })

    it('keeps one paragraph per page when the budget is non-positive', () => {
      expect(shelfViewBuildTextPageStarts([10, 20, 30], 0)).toEqual([0, 1, 2])
      expect(shelfViewBuildTextPageStarts([10, 20, 30], -5)).toEqual([0, 1, 2])
    })

    it('groups consecutive paragraphs up to the budget', () => {
      // 10 + 10 = 20 <= 25, adding the third (30) exceeds 25 -> new page at 2.
      expect(shelfViewBuildTextPageStarts([10, 10, 10], 25)).toEqual([0, 2])
    })

    it('puts a paragraph longer than the budget on its own page without splitting', () => {
      // Paragraph 0 (30) alone exceeds 25 but is never split; 1 and 2 then group.
      expect(shelfViewBuildTextPageStarts([30, 5, 5], 25)).toEqual([0, 1])
      expect(shelfViewBuildTextPageStarts([100], 25)).toEqual([0])
    })
  })

  describe('shelfViewPageIndexForParagraph', () => {
    it('maps a paragraph to the page that contains it', () => {
      const pageStarts = [0, 2, 5]
      expect(shelfViewPageIndexForParagraph(pageStarts, 0)).toBe(0)
      expect(shelfViewPageIndexForParagraph(pageStarts, 1)).toBe(0)
      expect(shelfViewPageIndexForParagraph(pageStarts, 2)).toBe(1)
      expect(shelfViewPageIndexForParagraph(pageStarts, 4)).toBe(1)
      expect(shelfViewPageIndexForParagraph(pageStarts, 5)).toBe(2)
      expect(shelfViewPageIndexForParagraph(pageStarts, 99)).toBe(2)
    })

    it('clamps out-of-range and empty inputs', () => {
      expect(shelfViewPageIndexForParagraph([0, 2, 5], -3)).toBe(0)
      expect(shelfViewPageIndexForParagraph([], 4)).toBe(0)
    })
  })

  describe('shelfViewEstimateCharsPerPage', () => {
    const wide = {
      columnWidth: 1000,
      viewportHeight: 900,
      fontSize: 20,
      lineHeight: 1.75,
      marginX: 56,
      marginY: 42,
    }

    it('estimates a realistic budget for a normal viewport', () => {
      // 23 lines * 46 chars in the reference calculation.
      expect(shelfViewEstimateCharsPerPage(wide)).toBe(1058)
    })

    it('grows with the viewport and shrinks with larger fonts', () => {
      const taller = shelfViewEstimateCharsPerPage({...wide, viewportHeight: 1400})
      const bigFont = shelfViewEstimateCharsPerPage({...wide, fontSize: 30})
      expect(taller).toBeGreaterThan(shelfViewEstimateCharsPerPage(wide))
      expect(bigFont).toBeLessThan(shelfViewEstimateCharsPerPage(wide))
    })

    it('never drops below the minimum budget', () => {
      const tiny = shelfViewEstimateCharsPerPage({
        columnWidth: 320,
        viewportHeight: 400,
        fontSize: 32,
        lineHeight: 2.2,
        marginX: 16,
        marginY: 18,
      })
      expect(tiny).toBe(200)
    })

    it('falls back to the minimum for degenerate metrics', () => {
      expect(shelfViewEstimateCharsPerPage({...wide, fontSize: 0})).toBe(200)
    })
  })
})
