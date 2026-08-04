import {ReaderAdapter} from '@/shelfview/adapters/reader-adapter'
import {shelfViewPageUrl} from '@/shelfview/functions/shelfview-urls'
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
   */
  private prefetchNextPage(): void {
    if (!this.manifest) {
      this.cancelPrefetch()
      return
    }

    const pageCount = this.getPagesCount()
    const wanted = new Map<string, number>()
    for (let offset = -1; offset <= SHELFVIEW_PREFETCH_PAGE_COUNT; offset++) {
      if (offset === 0) continue

      const pageIndex = this.currentPageIndex + offset
      if (pageIndex < 0 || pageIndex >= pageCount) continue

      const url = this.resolvePageUrl(this.manifest, pageIndex)
      if (url) wanted.set(url, pageIndex)
    }

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
  // Match the fetch mode the canvas display path uses, so a prefetched page is
  // reused there instead of being requested a second time.
  image.crossOrigin = 'use-credentials'
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
