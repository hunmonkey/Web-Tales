import {ShelfViewFixedPageReadingDirection} from '@/shelfview/functions/fixed-page-view'

export type ShelfViewReaderInputAction =
  | 'NEXT'
  | 'PREVIOUS'
  | 'FIRST'
  | 'LAST'
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
