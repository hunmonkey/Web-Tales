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

export class FixedPageAdapter implements ReaderAdapter {
  private manifest: ShelfViewManifest | undefined
  private currentPageIndex = 0
  private prefetchHandle: ShelfViewPagePreload | undefined

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
    if (!this.manifest || this.getPagesCount() < 1) return undefined
    return this.resolvePageUrl(this.manifest, this.currentPageIndex)
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

  private prefetchNextPage(): void {
    this.cancelPrefetch()

    if (!this.manifest) return

    const nextPageIndex = this.currentPageIndex + 1
    if (nextPageIndex >= this.getPagesCount()) return

    const nextPageUrl = this.resolvePageUrl(this.manifest, nextPageIndex)
    if (!nextPageUrl) return

    this.prefetchHandle = this.preloadPage(nextPageUrl)
  }

  private cancelPrefetch(): void {
    this.prefetchHandle?.cancel()
    this.prefetchHandle = undefined
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
  image.src = url

  return {
    cancel() {
      image.onload = null
      image.onerror = null
      image.src = ''
    },
  }
}
