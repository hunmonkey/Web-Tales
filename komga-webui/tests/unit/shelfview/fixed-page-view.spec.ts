import {
  shelfViewCanGoNext,
  shelfViewCanGoPrevious,
  shelfViewNextPageIndex,
  shelfViewPreviousPageIndex,
  shelfViewSpreadStartPageIndex,
  shelfViewVisiblePageIndexes,
} from '@/shelfview/functions/fixed-page-view'

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

  it('returns all pages for continuous scroll mode', () => {
    expect(shelfViewVisiblePageIndexes(2, 4, 'CONTINUOUS_SCROLL', 'RTL')).toEqual([0, 1, 2, 3])
  })

  it('handles empty and out-of-range values safely', () => {
    expect(shelfViewVisiblePageIndexes(20, 3, 'SINGLE_PAGE', 'LTR')).toEqual([2])
    expect(shelfViewVisiblePageIndexes(0, 0, 'SINGLE_PAGE', 'LTR')).toEqual([])
    expect(shelfViewCanGoNext(0, 0, 'SINGLE_PAGE')).toBe(false)
    expect(shelfViewCanGoPrevious(0, 0, 'SINGLE_PAGE')).toBe(false)
  })
})
