import {
  ShelfViewPanStart,
  shelfViewAnchoredZoomScroll,
  shelfViewPanScroll,
  shelfViewShouldStartPanning,
  shelfViewTextCharacterOffsetForScroll,
  shelfViewTextScrollTopForOffset,
} from '@/shelfview/functions/reader-viewport'

describe('ShelfView reader viewport helpers', () => {
  it('keeps the pointer area anchored while zooming', () => {
    expect(shelfViewAnchoredZoomScroll({
      previousScale: 100,
      nextScale: 110,
      scrollLeft: 40,
      scrollTop: 60,
      stageX: 100,
      stageY: 80,
    })).toEqual({
      scrollLeft: 54,
      scrollTop: 74,
    })
  })

  it('clamps zoom scroll positions at the viewport edge', () => {
    expect(shelfViewAnchoredZoomScroll({
      previousScale: 100,
      nextScale: 50,
      scrollLeft: 0,
      scrollTop: 0,
      stageX: 100,
      stageY: 80,
    })).toEqual({
      scrollLeft: 0,
      scrollTop: 0,
    })
  })

  it('calculates fixed-page drag panning', () => {
    const start: ShelfViewPanStart = {
      pointerId: 7,
      x: 120,
      y: 90,
      scrollLeft: 70,
      scrollTop: 25,
    }

    expect(shelfViewShouldStartPanning(start, 118, 89)).toBe(false)
    expect(shelfViewShouldStartPanning(start, 90, 70)).toBe(true)
    expect(shelfViewPanScroll(start, 90, 70)).toEqual({
      scrollLeft: 100,
      scrollTop: 45,
    })
  })

  it('maps text scroll position to a character offset', () => {
    expect(shelfViewTextCharacterOffsetForScroll({
      stageTop: 150,
      paragraphTop: 100,
      paragraphHeight: 200,
      textLength: 80,
    })).toBe(20)
  })

  it('maps a text character offset back to scroll top', () => {
    expect(shelfViewTextScrollTopForOffset({
      paragraphOffsetTop: 120,
      paragraphHeight: 80,
      characterOffset: 6,
      textLength: 24,
    })).toBe(140)
  })
})
