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

export function shelfViewVisiblePageIndexes(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
  readingDirection: ShelfViewFixedPageReadingDirection,
): number[] {
  const normalizedPageCount = normalizePageCount(pageCount)
  if (normalizedPageCount < 1) return []

  if (layout === 'CONTINUOUS_SCROLL') {
    return Array.from({length: normalizedPageCount}, (_, index) => index)
  }

  const current = clampPageIndex(currentPageIndex, normalizedPageCount)
  if (layout === 'SINGLE_PAGE') return [current]

  const spreadStart = shelfViewSpreadStartPageIndex(current, normalizedPageCount, layout)
  const spread = [spreadStart]
  if (spreadStart + 1 < normalizedPageCount) spread.push(spreadStart + 1)

  return readingDirection === 'RTL' ? spread.reverse() : spread
}

export function shelfViewSpreadStartPageIndex(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
): number {
  const normalizedPageCount = normalizePageCount(pageCount)
  if (normalizedPageCount < 1) return 0

  const current = clampPageIndex(currentPageIndex, normalizedPageCount)
  if (layout !== 'DOUBLE_PAGE') return current

  return current - (current % 2)
}

export function shelfViewNextPageIndex(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
): number {
  const normalizedPageCount = normalizePageCount(pageCount)
  if (normalizedPageCount < 1) return 0

  const current = clampPageIndex(currentPageIndex, normalizedPageCount)
  const step = layout === 'DOUBLE_PAGE' ? 2 : 1
  const spreadStart = shelfViewSpreadStartPageIndex(current, normalizedPageCount, layout)
  const target = spreadStart + step
  return target < normalizedPageCount ? target : spreadStart
}

export function shelfViewPreviousPageIndex(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
): number {
  const normalizedPageCount = normalizePageCount(pageCount)
  if (normalizedPageCount < 1) return 0

  const current = clampPageIndex(currentPageIndex, normalizedPageCount)
  const step = layout === 'DOUBLE_PAGE' ? 2 : 1
  const spreadStart = shelfViewSpreadStartPageIndex(current, normalizedPageCount, layout)
  const target = spreadStart - step
  return target >= 0 ? target : spreadStart
}

export function shelfViewCanGoNext(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
): boolean {
  return shelfViewNextPageIndex(currentPageIndex, pageCount, layout) > shelfViewSpreadStartPageIndex(currentPageIndex, pageCount, layout)
}

export function shelfViewCanGoPrevious(
  currentPageIndex: number,
  pageCount: number,
  layout: ShelfViewFixedPageLayout,
): boolean {
  return shelfViewPreviousPageIndex(currentPageIndex, pageCount, layout) < shelfViewSpreadStartPageIndex(currentPageIndex, pageCount, layout)
}

function normalizePageCount(pageCount: number): number {
  if (!Number.isFinite(pageCount)) return 0
  return Math.max(0, Math.floor(pageCount))
}

function clampPageIndex(pageIndex: number, pageCount: number): number {
  if (!Number.isFinite(pageIndex)) return 0
  return Math.max(0, Math.min(Math.floor(pageIndex), pageCount - 1))
}
