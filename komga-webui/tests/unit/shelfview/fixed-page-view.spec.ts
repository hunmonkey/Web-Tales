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
