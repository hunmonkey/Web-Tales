export type ShelfViewFixedPageLayout =
  | 'SINGLE_PAGE'
  | 'DOUBLE_PAGE'
  | 'CONTINUOUS_SCROLL'

export type ShelfViewFixedPageReadingDirection =
  | 'LTR'
  | 'RTL'

export type ShelfViewFixedPageFit =
  | 'SCREEN'
  | 'WIDTH'
  | 'HEIGHT'
  | 'ORIGINAL'

export type ShelfViewFixedPageRotation =
  | 0
  | 90
  | 180
  | 270

export type ShelfViewFixedPageBackground =
  | 'BLACK'
  | 'DARK'
  | 'GRAY'
  | 'PAPER'
  | 'WHITE'

export interface ShelfViewFixedPageBackgroundOption {
  id: ShelfViewFixedPageBackground,
  cssValue: string,
}

export const SHELFVIEW_FIXED_PAGE_LAYOUTS: ShelfViewFixedPageLayout[] = [
  'SINGLE_PAGE',
  'DOUBLE_PAGE',
  'CONTINUOUS_SCROLL',
]

export const SHELFVIEW_FIXED_PAGE_FITS: ShelfViewFixedPageFit[] = [
  'SCREEN',
  'WIDTH',
  'HEIGHT',
  'ORIGINAL',
]

export const SHELFVIEW_FIXED_PAGE_ROTATIONS: ShelfViewFixedPageRotation[] = [
  0,
  90,
  180,
  270,
]

export const SHELFVIEW_FIXED_PAGE_BACKGROUNDS: ShelfViewFixedPageBackgroundOption[] = [
  {id: 'BLACK', cssValue: '#000000'},
  {id: 'DARK', cssValue: '#111111'},
  {id: 'GRAY', cssValue: '#2b2f36'},
  {id: 'PAPER', cssValue: '#f4ecd9'},
  {id: 'WHITE', cssValue: '#ffffff'},
]

export function shelfViewFixedPageBackground(
  background: ShelfViewFixedPageBackground,
): ShelfViewFixedPageBackgroundOption {
  return SHELFVIEW_FIXED_PAGE_BACKGROUNDS.find(option => option.id === background) || SHELFVIEW_FIXED_PAGE_BACKGROUNDS[1]
}

export function shelfViewNextFixedPageRotation(
  rotation: number,
): ShelfViewFixedPageRotation {
  const normalizedRotation = normalizeFixedPageRotation(rotation)
  const currentIndex = SHELFVIEW_FIXED_PAGE_ROTATIONS.indexOf(normalizedRotation)
  return SHELFVIEW_FIXED_PAGE_ROTATIONS[(currentIndex + 1) % SHELFVIEW_FIXED_PAGE_ROTATIONS.length]
}

export function normalizeFixedPageRotation(
  rotation: number,
): ShelfViewFixedPageRotation {
  return SHELFVIEW_FIXED_PAGE_ROTATIONS.includes(rotation as ShelfViewFixedPageRotation)
    ? rotation as ShelfViewFixedPageRotation
    : 0
}

/**
 * Comics carry the occasional double-spread illustration saved as one wide
 * image. Pairing it with a neighbour squeezes both halves and flips the left and
 * right sides of every spread after it, so a wide page is shown on its own and
 * never taken as the second half of a spread. Wide pages are discovered as the
 * images load, because the manifest does not carry page dimensions.
 */
export function shelfViewVisiblePageIndexes(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
  readingDirection: ShelfViewFixedPageReadingDirection,
  coverSingle = false,
  widePageIndexes: ReadonlySet<number> = new Set(),
): number[] {
  const normalizedPageCount = normalizePageCount(pageCount)
  if (normalizedPageCount < 1) return []

  if (layout === 'CONTINUOUS_SCROLL') {
    return Array.from({length: normalizedPageCount}, (_, index) => index)
  }

  const current = clampPageIndex(currentPageIndex, normalizedPageCount)
  if (layout === 'SINGLE_PAGE') return [current]
  if (layout === 'DOUBLE_PAGE' && coverSingle && current === 0) return [0]

  const candidate = shelfViewSpreadStartPageIndex(current, normalizedPageCount, layout, coverSingle)
  const spreadStart = widePageIndexes.has(current) || (candidate !== current && widePageIndexes.has(candidate))
    ? current
    : candidate
  if (widePageIndexes.has(spreadStart)) return [spreadStart]

  const spread = [spreadStart]
  if (spreadStart + 1 < normalizedPageCount && !widePageIndexes.has(spreadStart + 1)) spread.push(spreadStart + 1)

  return readingDirection === 'RTL' ? spread.reverse() : spread
}

export function shelfViewSpreadStartPageIndex(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
  coverSingle = false,
): number {
  const normalizedPageCount = normalizePageCount(pageCount)
  if (normalizedPageCount < 1) return 0

  const current = clampPageIndex(currentPageIndex, normalizedPageCount)
  if (layout !== 'DOUBLE_PAGE') return current
  if (coverSingle && current === 0) return 0
  if (coverSingle) return current - ((current - 1) % 2)

  return current - (current % 2)
}

export function shelfViewNextPageIndex(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
  coverSingle = false,
): number {
  const normalizedPageCount = normalizePageCount(pageCount)
  if (normalizedPageCount < 1) return 0

  const current = clampPageIndex(currentPageIndex, normalizedPageCount)
  const step = layout === 'DOUBLE_PAGE' ? 2 : 1
  const spreadStart = shelfViewSpreadStartPageIndex(current, normalizedPageCount, layout, coverSingle)
  const target = layout === 'DOUBLE_PAGE' && coverSingle && spreadStart === 0 ? 1 : spreadStart + step
  return target < normalizedPageCount ? target : spreadStart
}

export function shelfViewPreviousPageIndex(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
  coverSingle = false,
): number {
  const normalizedPageCount = normalizePageCount(pageCount)
  if (normalizedPageCount < 1) return 0

  const current = clampPageIndex(currentPageIndex, normalizedPageCount)
  const step = layout === 'DOUBLE_PAGE' ? 2 : 1
  const spreadStart = shelfViewSpreadStartPageIndex(current, normalizedPageCount, layout, coverSingle)
  const target = layout === 'DOUBLE_PAGE' && coverSingle && spreadStart <= 1 ? 0 : spreadStart - step
  return target >= 0 ? target : spreadStart
}

export function shelfViewCanGoNext(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
  coverSingle = false,
): boolean {
  return shelfViewNextPageIndex(currentPageIndex, pageCount, layout, coverSingle) > shelfViewSpreadStartPageIndex(currentPageIndex, pageCount, layout, coverSingle)
}

export function shelfViewCanGoPrevious(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
  coverSingle = false,
): boolean {
  return shelfViewPreviousPageIndex(currentPageIndex, pageCount, layout, coverSingle) < shelfViewSpreadStartPageIndex(currentPageIndex, pageCount, layout, coverSingle)
}

function normalizePageCount(pageCount: number): number {
  if (!Number.isFinite(pageCount)) return 0
  return Math.max(0, Math.floor(pageCount))
}

function clampPageIndex(pageIndex: number, pageCount: number): number {
  if (!Number.isFinite(pageIndex)) return 0
  return Math.max(0, Math.min(Math.floor(pageIndex), pageCount - 1))
}
