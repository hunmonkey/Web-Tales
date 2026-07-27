export interface ShelfViewViewportScroll {
  scrollLeft: number,
  scrollTop: number,
}

export interface ShelfViewAnchoredZoomInput extends ShelfViewViewportScroll {
  previousScale: number,
  nextScale: number,
  stageX: number,
  stageY: number,
}

export interface ShelfViewPanStart extends ShelfViewViewportScroll {
  pointerId: number,
  x: number,
  y: number,
}

export function shelfViewAnchoredZoomScroll({
  previousScale,
  nextScale,
  scrollLeft,
  scrollTop,
  stageX,
  stageY,
}: ShelfViewAnchoredZoomInput): ShelfViewViewportScroll {
  const scaleRatio = previousScale > 0 ? nextScale / previousScale : 1
  const contentX = scrollLeft + stageX
  const contentY = scrollTop + stageY

  return {
    scrollLeft: Math.max(0, (contentX * scaleRatio) - stageX),
    scrollTop: Math.max(0, (contentY * scaleRatio) - stageY),
  }
}

// A mouse pointer drifts by a few pixels during an ordinary click, so the
// threshold has to clear the tap threshold in reader-input; below it the drag
// is still a click and must keep working as one.
export function shelfViewShouldStartPanning(
  start: ShelfViewPanStart,
  x: number,
  y: number,
  threshold = 10,
): boolean {
  const delta = shelfViewPanDelta(start, x, y)
  return Math.abs(delta.x) + Math.abs(delta.y) > threshold
}

export function shelfViewPanScroll(
  start: ShelfViewPanStart,
  x: number,
  y: number,
): ShelfViewViewportScroll {
  const delta = shelfViewPanDelta(start, x, y)

  return {
    scrollLeft: Math.max(0, start.scrollLeft - delta.x),
    scrollTop: Math.max(0, start.scrollTop - delta.y),
  }
}

export function shelfViewTextCharacterOffsetForScroll({
  stageTop,
  paragraphTop,
  paragraphHeight,
  textLength,
}: {
  stageTop: number,
  paragraphTop: number,
  paragraphHeight: number,
  textLength: number,
}): number {
  const offsetRatio = paragraphHeight > 0
    ? clampNumber((stageTop - paragraphTop) / paragraphHeight, 0, 1)
    : 0

  return Math.floor(Math.max(0, textLength) * offsetRatio)
}

export function shelfViewTextScrollTopForOffset({
  paragraphOffsetTop,
  paragraphHeight,
  characterOffset,
  textLength,
}: {
  paragraphOffsetTop: number,
  paragraphHeight: number,
  characterOffset: number,
  textLength: number,
}): number {
  const offsetRatio = textLength > 0
    ? clampNumber(characterOffset / textLength, 0, 1)
    : 0

  return Math.max(0, paragraphOffsetTop + (paragraphHeight * offsetRatio))
}

function shelfViewPanDelta(
  start: ShelfViewPanStart,
  x: number,
  y: number,
): { x: number, y: number } {
  return {
    x: x - start.x,
    y: y - start.y,
  }
}

function clampNumber(
  value: number,
  min: number,
  max: number,
): number {
  if (!Number.isFinite(value)) return min
  return Math.min(Math.max(value, min), max)
}
