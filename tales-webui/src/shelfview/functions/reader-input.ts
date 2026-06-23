import {ShelfViewFixedPageReadingDirection} from '@/shelfview/functions/fixed-page-view'

export type ShelfViewReaderInputAction =
  | 'NEXT'
  | 'PREVIOUS'
  | 'FIRST'
  | 'LAST'
  | 'ROTATE'
  | 'TOGGLE_FULLSCREEN'
  | 'TOGGLE_CHROME'
  | 'SHOW_SHORTCUTS'

export interface ShelfViewTouchPoint {
  x: number,
  y: number,
}

export function shelfViewReaderKeyboardAction(
  key: string,
  readingDirection: ShelfViewFixedPageReadingDirection,
): ShelfViewReaderInputAction | undefined {
  switch (key) {
    case 'ArrowRight':
      return readingDirection === 'RTL' ? 'PREVIOUS' : 'NEXT'
    case 'ArrowLeft':
      return readingDirection === 'RTL' ? 'NEXT' : 'PREVIOUS'
    case 'ArrowDown':
    case 'PageDown':
    case ' ':
      return 'NEXT'
    case 'ArrowUp':
    case 'PageUp':
      return 'PREVIOUS'
    case 'Home':
      return 'FIRST'
    case 'End':
      return 'LAST'
    case 'r':
    case 'R':
      return 'ROTATE'
    case 'f':
    case 'F':
      return 'TOGGLE_FULLSCREEN'
    case '?':
    case 'F1':
      return 'SHOW_SHORTCUTS'
    default:
      return undefined
  }
}

export function shelfViewTouchAction(
  start: ShelfViewTouchPoint,
  end: ShelfViewTouchPoint,
  readingDirection: ShelfViewFixedPageReadingDirection,
): ShelfViewReaderInputAction | undefined {
  const deltaX = end.x - start.x
  const deltaY = end.y - start.y
  const absX = Math.abs(deltaX)
  const absY = Math.abs(deltaY)

  if (absX < 48 || absX < absY * 1.2) return undefined

  const swipedLeft = deltaX < 0
  if (readingDirection === 'RTL') return swipedLeft ? 'PREVIOUS' : 'NEXT'
  return swipedLeft ? 'NEXT' : 'PREVIOUS'
}

export function shelfViewCenterTapAction(
  start: ShelfViewTouchPoint,
  end: ShelfViewTouchPoint,
  stageWidth: number,
): ShelfViewReaderInputAction | undefined {
  return shelfViewTapAction(start, end, stageWidth, 'LTR', false)
}

export function shelfViewTapAction(
  start: ShelfViewTouchPoint,
  end: ShelfViewTouchPoint,
  stageWidth: number,
  readingDirection: ShelfViewFixedPageReadingDirection,
  pagingEnabled: boolean,
): ShelfViewReaderInputAction | undefined {
  if (!Number.isFinite(stageWidth) || stageWidth <= 0) return undefined

  const deltaX = end.x - start.x
  const deltaY = end.y - start.y
  if (Math.abs(deltaX) >= 10 || Math.abs(deltaY) >= 10) return undefined

  const tapX = (start.x + end.x) / 2
  const leftBoundary = stageWidth * 0.35
  const rightBoundary = stageWidth * 0.65

  if (tapX >= leftBoundary && tapX <= rightBoundary) return 'TOGGLE_CHROME'
  if (!pagingEnabled) return undefined

  const leftTap = tapX < leftBoundary
  if (readingDirection === 'RTL') return leftTap ? 'NEXT' : 'PREVIOUS'
  return leftTap ? 'PREVIOUS' : 'NEXT'
}

export function shelfViewShouldIgnoreShortcutTarget(target: EventTarget | null): boolean {
  if (!target || !(target instanceof HTMLElement)) return false

  const tagName = target.tagName.toLowerCase()
  return target.isContentEditable ||
    target.contentEditable === 'true' ||
    target.getAttribute('contenteditable') === 'true' ||
    tagName === 'input' ||
    tagName === 'textarea' ||
    tagName === 'select'
}
