import {
  normalizeFixedPageRotation,
  shelfViewCanGoNext,
  shelfViewCanGoPrevious,
  shelfViewNextFixedPageRotation,
  shelfViewNextPageIndex,
  shelfViewPreviousPageIndex,
  shelfViewSpreadStartPageIndex,
  shelfViewVisiblePageIndexes,
} from '@/shelfview/functions/fixed-page-view'

describe('ShelfView wide page spreads', () => {
  it('shows a wide page alone and never pairs it as the second half', () => {
    const wide = new Set([2])

    // The wide page itself takes the whole stage.
    expect(shelfViewVisiblePageIndexes(2, 6, 'DOUBLE_PAGE', 'LTR', false, wide)).toEqual([2])
    // The spread before it is untouched and stops short of the wide page.
    expect(shelfViewVisiblePageIndexes(1, 6, 'DOUBLE_PAGE', 'LTR', false, wide)).toEqual([0, 1])
    // A wide page sitting where a right-hand half would go is left out.
    expect(shelfViewVisiblePageIndexes(1, 6, 'DOUBLE_PAGE', 'LTR', false, new Set([1]))).toEqual([1])
    // The page after it starts a fresh spread instead of inheriting the offset.
    expect(shelfViewVisiblePageIndexes(3, 6, 'DOUBLE_PAGE', 'LTR', false, wide)).toEqual([3, 4])
    // Without the hint the pairing is unchanged.
    expect(shelfViewVisiblePageIndexes(2, 6, 'DOUBLE_PAGE', 'LTR')).toEqual([2, 3])
  })

  it.each([
    {wide: [2], pageCount: 6, coverSingle: false, spreads: [[0, 1], [2], [3, 4], [5]]},
    {wide: [3], pageCount: 6, coverSingle: false, spreads: [[0, 1], [2], [3], [4, 5]]},
    {wide: [4], pageCount: 7, coverSingle: true, spreads: [[0], [1, 2], [3], [4], [5, 6]]},
  ])('turns through every page exactly once across wide pages $wide', ({wide, pageCount, coverSingle, spreads}) => {
    const wideSet = new Set(wide)
    const walk = (start: number, turn: typeof shelfViewNextPageIndex, canTurn: typeof shelfViewCanGoNext) => {
      const seen = [shelfViewVisiblePageIndexes(start, pageCount, 'DOUBLE_PAGE', 'LTR', coverSingle, wideSet)]
      let current = start
      while (canTurn(current, pageCount, 'DOUBLE_PAGE', coverSingle, wideSet) && seen.length <= pageCount) {
        current = turn(current, pageCount, 'DOUBLE_PAGE', coverSingle, wideSet)
        seen.push(shelfViewVisiblePageIndexes(current, pageCount, 'DOUBLE_PAGE', 'LTR', coverSingle, wideSet))
      }
      return seen
    }

    expect(walk(0, shelfViewNextPageIndex, shelfViewCanGoNext)).toEqual(spreads)
    expect(walk(pageCount - 1, shelfViewPreviousPageIndex, shelfViewCanGoPrevious)).toEqual([...spreads].reverse())
  })
})

describe('ShelfView fixed page view helpers', () => {
  it('keeps single page mode on the current page', () => {
    expect(shelfViewVisiblePageIndexes(2, 5, 'SINGLE_PAGE', 'LTR')).toEqual([2])
    expect(shelfViewNextPageIndex(2, 5, 'SINGLE_PAGE')).toBe(3)
    expect(shelfViewPreviousPageIndex(2, 5, 'SINGLE_PAGE')).toBe(1)
  })

  it('groups double page mode by zero-based spreads', () => {
    expect(shelfViewSpreadStartPageIndex(1, 5, 'DOUBLE_PAGE')).toBe(0)
    expect(shelfViewVisiblePageIndexes(1, 5, 'DOUBLE_PAGE', 'LTR')).toEqual([0, 1])
    expect(shelfViewVisiblePageIndexes(4, 5, 'DOUBLE_PAGE', 'LTR')).toEqual([4])
  })

  it('reverses double page display order for RTL without changing page numbers', () => {
    expect(shelfViewVisiblePageIndexes(2, 5, 'DOUBLE_PAGE', 'RTL')).toEqual([3, 2])
  })

  it('moves by whole spreads in double page mode', () => {
    expect(shelfViewCanGoPrevious(1, 5, 'DOUBLE_PAGE')).toBe(false)
    expect(shelfViewCanGoNext(1, 5, 'DOUBLE_PAGE')).toBe(true)
    expect(shelfViewNextPageIndex(1, 5, 'DOUBLE_PAGE')).toBe(2)
    expect(shelfViewPreviousPageIndex(2, 5, 'DOUBLE_PAGE')).toBe(0)
    expect(shelfViewCanGoNext(3, 4, 'DOUBLE_PAGE')).toBe(false)
    expect(shelfViewNextPageIndex(3, 4, 'DOUBLE_PAGE')).toBe(2)
  })

  it('keeps the cover alone when double page cover-single mode is enabled', () => {
    expect(shelfViewVisiblePageIndexes(0, 5, 'DOUBLE_PAGE', 'LTR', true)).toEqual([0])
    expect(shelfViewVisiblePageIndexes(1, 5, 'DOUBLE_PAGE', 'LTR', true)).toEqual([1, 2])
    expect(shelfViewVisiblePageIndexes(2, 5, 'DOUBLE_PAGE', 'LTR', true)).toEqual([1, 2])
    expect(shelfViewNextPageIndex(0, 5, 'DOUBLE_PAGE', true)).toBe(1)
    expect(shelfViewPreviousPageIndex(1, 5, 'DOUBLE_PAGE', true)).toBe(0)
  })

  it('returns all pages for continuous scroll mode', () => {
    expect(shelfViewVisiblePageIndexes(2, 4, 'CONTINUOUS_SCROLL', 'RTL')).toEqual([0, 1, 2, 3])
  })

  it('cycles fixed page rotation by right angles', () => {
    expect(shelfViewNextFixedPageRotation(0)).toBe(90)
    expect(shelfViewNextFixedPageRotation(90)).toBe(180)
    expect(shelfViewNextFixedPageRotation(180)).toBe(270)
    expect(shelfViewNextFixedPageRotation(270)).toBe(0)
    expect(normalizeFixedPageRotation(45)).toBe(0)
  })

  it('handles empty and out-of-range values safely', () => {
    expect(shelfViewVisiblePageIndexes(20, 3, 'SINGLE_PAGE', 'LTR')).toEqual([2])
    expect(shelfViewVisiblePageIndexes(0, 0, 'SINGLE_PAGE', 'LTR')).toEqual([])
    expect(shelfViewCanGoNext(0, 0, 'SINGLE_PAGE')).toBe(false)
    expect(shelfViewCanGoPrevious(0, 0, 'SINGLE_PAGE')).toBe(false)
  })
})
