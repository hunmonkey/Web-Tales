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
  | 'TOGGLE_CONTENTS'
  | 'TOGGLE_BOOKMARKS'
  | 'TOGGLE_SETTINGS'
  | 'FOCUS_PAGE_JUMP'
  | 'ZOOM_IN'
  | 'ZOOM_OUT'
  | 'ZOOM_RESET'

export interface ShelfViewTouchPoint {
  x: number,
  y: number,
}

/**
 * What the stage was doing while a touch gesture ran. A page that is wider than
 * the stage is panned by dragging it, so the same drag must not also turn the
 * page. The mouse path already makes that distinction through the pan pointer
 * handlers; touch has no pan handler of its own because the stage scrolls
 * natively, so the distinction has to be made here instead.
 */
export interface ShelfViewTouchStageState {
  canPanHorizontally: boolean,
  scrolledHorizontally: boolean,
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
    // The letters below match the ones the fallback readers already use, so a
    // reader moving between the two readers does not have to relearn them.
    case 't':
    case 'T':
      return 'TOGGLE_CONTENTS'
    case 'b':
    case 'B':
      return 'TOGGLE_BOOKMARKS'
    case 's':
    case 'S':
      return 'TOGGLE_SETTINGS'
    case 'g':
    case 'G':
      return 'FOCUS_PAGE_JUMP'
    case '+':
    case '=':
      return 'ZOOM_IN'
    case '-':
    case '_':
      return 'ZOOM_OUT'
    case '0':
      return 'ZOOM_RESET'
    default:
      return undefined
  }
}

export function shelfViewTouchAction(
  start: ShelfViewTouchPoint,
  end: ShelfViewTouchPoint,
  readingDirection: ShelfViewFixedPageReadingDirection,
  stage?: ShelfViewTouchStageState,
): ShelfViewReaderInputAction | undefined {
  if (stage && (stage.canPanHorizontally || stage.scrolledHorizontally)) return undefined

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
