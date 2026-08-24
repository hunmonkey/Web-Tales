import {ReaderAdapter} from '@/shelfview/adapters/reader-adapter'
import {shelfViewPageImageCrossOrigin, shelfViewPageUrl} from '@/shelfview/functions/shelfview-urls'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {
  SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  ShelfViewLocator,
  ShelfViewPageLocator,
} from '@/shelfview/models/shelfview-locator'

export interface ShelfViewPagePreload {
  cancel(): void
}

export type ShelfViewPageUrlResolver = (manifest: ShelfViewManifest, pageIndex: number) => string | undefined
export type ShelfViewPagePreloader = (url: string) => ShelfViewPagePreload

// Double page shows two pages per turn, so prefetching only one page leaves the
// second half of the next spread unfetched. Four covers the next spread and the
// one after it, which keeps a reader who turns quickly ahead of the network.
export const SHELFVIEW_PREFETCH_PAGE_COUNT = 4

export class FixedPageAdapter implements ReaderAdapter {
  private manifest: ShelfViewManifest | undefined
  private currentPageIndex = 0
  private prefetchHandles = new Map<string, ShelfViewPagePreload>()

  constructor(
    private readonly resolvePageUrl: ShelfViewPageUrlResolver = shelfViewPageUrl,
    private readonly preloadPage: ShelfViewPagePreloader = defaultShelfViewPagePreloader,
  ) {
  }

  async open(manifest: ShelfViewManifest): Promise<void> {
    if (manifest.kind !== 'FIXED_PAGE_SEQUENCE') {
      throw new Error('FixedPageAdapter can only open fixed-page manifests.')
    }

    this.cancelPrefetch()
    this.manifest = manifest
    this.currentPageIndex = 0
    this.prefetchNextPage()
  }

  async next(): Promise<void> {
    this.moveToPage(this.currentPageIndex + 1)
  }

  async previous(): Promise<void> {
    this.moveToPage(this.currentPageIndex - 1)
  }

  async goTo(locator: ShelfViewLocator): Promise<void> {
    const manifest = this.requireManifest()

    if (locator.schemaVersion !== SHELFVIEW_LOCATOR_SCHEMA_VERSION || locator.kind !== 'PAGE') {
      throw new Error('FixedPageAdapter only accepts page locators.')
    }

    if (locator.contentFingerprint !== manifest.contentFingerprint) {
      throw new Error('The page locator belongs to a different book version.')
    }

    this.moveToPage(locator.page)
  }

  getCurrentLocator(): ShelfViewPageLocator {
    const manifest = this.requireManifest()

    return {
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'PAGE',
      page: this.currentPageIndex,
      offset: 0,
      contentFingerprint: manifest.contentFingerprint,
    }
  }

  getProgression(): number {
    const pageCount = this.getPagesCount()
    if (pageCount < 1) return 0
    return (this.currentPageIndex + 1) / pageCount
  }

  getCurrentPageUrl(): string | undefined {
    return this.getPageUrl(this.currentPageIndex)
  }

  getPageUrl(pageIndex: number): string | undefined {
    if (!this.manifest || this.getPagesCount() < 1) return undefined
    if (!Number.isFinite(pageIndex) || pageIndex < 0 || pageIndex >= this.getPagesCount()) return undefined
    return this.resolvePageUrl(this.manifest, Math.floor(pageIndex))
  }

  getCurrentPageIndex(): number {
    return this.currentPageIndex
  }

  getPagesCount(): number {
    return this.normalizedPageCount(this.manifest)
  }

  canGoPrevious(): boolean {
    return this.currentPageIndex > 0
  }

  canGoNext(): boolean {
    return this.currentPageIndex < this.getPagesCount() - 1
  }

  destroy(): void {
    this.cancelPrefetch()
    this.manifest = undefined
    this.currentPageIndex = 0
  }

  private moveToPage(pageIndex: number): void {
    this.requireManifest()

    const pageCount = this.getPagesCount()
    if (pageCount < 1) {
      this.currentPageIndex = 0
      this.cancelPrefetch()
      return
    }

    const nextPageIndex = Math.max(0, Math.min(pageIndex, pageCount - 1))
    if (nextPageIndex === this.currentPageIndex) return

    this.currentPageIndex = nextPageIndex
    this.prefetchNextPage()
  }

  /**
   * Keeps a window of pages loading around the current one. Turning a page used
   * to cancel every handle and reissue the overlapping ones, which threw away
   * downloads that were already in flight; the ones that stay inside the new
   * window are reused instead, and only the pages that fell out are cancelled.
   * The window reaches one page backwards so returning to check something does
   * not start from a blank screen.
   *
   * The current page is inside the window rather than skipped. Skipping it meant
   * that arriving at a page cancelled that page's own prefetch, and cancelling
   * aborts a download still in flight - so turning pages faster than they load
   * threw away the page being turned to and made the reader fetch it again from
   * nothing. It is requested first so the browser starts the page on screen
   * ahead of its neighbours, then the pages ahead in reading order, then the
   * page behind.
   */
  private prefetchNextPage(): void {
    const manifest = this.manifest
    if (!manifest) {
      this.cancelPrefetch()
      return
    }

    const pageCount = this.getPagesCount()
    const offsets = [0]
    for (let offset = 1; offset <= SHELFVIEW_PREFETCH_PAGE_COUNT; offset++) offsets.push(offset)
    offsets.push(-1)

    const wanted = new Map<string, number>()
    offsets.forEach(offset => {
      const pageIndex = this.currentPageIndex + offset
      if (pageIndex < 0 || pageIndex >= pageCount) return

      const url = this.resolvePageUrl(manifest, pageIndex)
      if (url) wanted.set(url, pageIndex)
    })

    const kept = new Map<string, ShelfViewPagePreload>()
    this.prefetchHandles.forEach((handle, url) => {
      if (wanted.has(url)) kept.set(url, handle)
      else handle.cancel()
    })

    wanted.forEach((_pageIndex, url) => {
      if (!kept.has(url)) kept.set(url, this.preloadPage(url))
    })

    this.prefetchHandles = kept
  }

  private cancelPrefetch(): void {
    this.prefetchHandles.forEach(handle => handle.cancel())
    this.prefetchHandles = new Map()
  }

  private requireManifest(): ShelfViewManifest {
    if (!this.manifest) throw new Error('FixedPageAdapter is not open.')
    return this.manifest
  }

  private normalizedPageCount(manifest: ShelfViewManifest | undefined): number {
    const pageCount = manifest?.pageCount ?? 0
    if (!Number.isFinite(pageCount)) return 0
    return Math.max(0, Math.floor(pageCount))
  }
}

export function defaultShelfViewPagePreloader(url: string): ShelfViewPagePreload {
  const image = new Image()
  // Match the fetch mode the display path uses, so a prefetched page is reused
  // there instead of being requested a second time. Same-origin pages, which is
  // every deployed one, are fetched without a CORS mode by both the plain `<img>`
  // and the canvas.
  const crossOrigin = shelfViewPageImageCrossOrigin(url)
  if (crossOrigin) image.crossOrigin = crossOrigin
  image.src = url

  return {
    cancel() {
      image.onload = null
      image.onerror = null
      // Not `src = ''`: an empty string resolves to the document URL, so the
      // browser would fetch the page itself as an image instead of stopping.
      image.removeAttribute('src')
    },
  }
}
