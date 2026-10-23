import {
  FixedPageAdapter,
  ShelfViewPagePreloader,
  ShelfViewPageUrlResolver,
  defaultShelfViewPagePreloader,
} from '@/shelfview/adapters/fixed-page-adapter'
import {ReaderAdapter} from '@/shelfview/adapters/reader-adapter'
import {ShelfViewTextContentLoader, TextReflowAdapter} from '@/shelfview/adapters/text-reflow-adapter'
import {shelfViewPageUrl} from '@/shelfview/functions/shelfview-urls'
import {ShelfViewFixedPageLayout} from '@/shelfview/functions/fixed-page-view'
import {ShelfViewManifest, ShelfViewManifestItem} from '@/shelfview/models/shelfview-manifest'
import {
  SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  ShelfViewLocator,
  ShelfViewMixedLocator,
  ShelfViewPageLocator,
  ShelfViewTextLocator,
} from '@/shelfview/models/shelfview-locator'
import {ShelfViewTextParagraph} from '@/shelfview/models/shelfview-text'

type MixedItemAdapter = FixedPageAdapter | TextReflowAdapter

export class MixedContentAdapter implements ReaderAdapter {
  private manifest: ShelfViewManifest | undefined
  private items: ShelfViewManifestItem[] = []
  private currentItemIndex = 0
  private currentAdapter: MixedItemAdapter | undefined
  private charsPerPage = 0
  private openRequestId = 0

  constructor(
    private readonly loadTextContent: ShelfViewTextContentLoader,
    private readonly resolvePageUrl: ShelfViewPageUrlResolver = shelfViewPageUrl,
    private readonly preloadPage: ShelfViewPagePreloader = defaultShelfViewPagePreloader,
  ) {
  }

  async open(manifest: ShelfViewManifest): Promise<void> {
    if (manifest.kind !== 'MIXED_CONTENT_SEQUENCE') {
      throw new Error('MixedContentAdapter can only open mixed manifests.')
    }

    const items = [...manifest.items ?? []].sort((a, b) => a.index - b.index)
    if (items.length === 0) throw new Error('MixedContentAdapter requires at least one item.')
    if (items.some(item => !this.isSupportedItem(item))) {
      throw new Error('MixedContentAdapter only accepts fixed-page or text items.')
    }

    this.destroy()
    this.manifest = manifest
    this.items = items
    await this.openItem(0)
  }

  /**
   * Hands the page budget to the text items so a TXT inside an archive is read
   * in screenfuls like a standalone TXT. A budget of zero keeps the original
   * one-paragraph-per-page behaviour, which is what the other layouts want.
   */
  setPagination(charsPerPage: number): void {
    this.charsPerPage = Number.isFinite(charsPerPage) && charsPerPage > 0 ? Math.floor(charsPerPage) : 0

    const adapter = this.currentAdapter
    if (adapter instanceof TextReflowAdapter) adapter.setPagination(this.charsPerPage)
  }

  async next(): Promise<void> {
    const adapter = this.requireCurrentAdapter()
    const paginated = this.currentPaginatedText(adapter)
    if (paginated) {
      if (paginated.canGoNextPage()) {
        await paginated.nextPage()
        return
      }
    } else if (adapter.canGoNext()) {
      await adapter.next()
      return
    }
    if (this.currentItemIndex < this.items.length - 1) await this.openItem(this.currentItemIndex + 1)
  }

  async previous(): Promise<void> {
    const adapter = this.requireCurrentAdapter()
    const paginated = this.currentPaginatedText(adapter)
    if (paginated) {
      if (paginated.canGoPreviousPage()) {
        await paginated.previousPage()
        return
      }
    } else if (adapter.canGoPrevious()) {
      await adapter.previous()
      return
    }
    if (this.currentItemIndex > 0 && await this.openItem(this.currentItemIndex - 1)) {
      await this.goToLastLocationInCurrentItem()
    }
  }

  async goTo(locator: ShelfViewLocator): Promise<void> {
    const manifest = this.requireManifest()

    if (locator.schemaVersion !== SHELFVIEW_LOCATOR_SCHEMA_VERSION || locator.kind !== 'MIXED') {
      throw new Error('MixedContentAdapter only accepts mixed locators.')
    }
    if (locator.contentFingerprint !== manifest.contentFingerprint) {
      throw new Error('The mixed locator belongs to a different book version.')
    }

    const item = this.items[locator.itemIndex]
    if (!item || item.itemId !== locator.itemId) {
      throw new Error('The mixed locator item is unavailable.')
    }
    if (!this.itemMatchesLocator(item, locator.itemLocator)) {
      throw new Error('The mixed locator item type is invalid.')
    }

    if (await this.openItem(locator.itemIndex)) await this.requireCurrentAdapter().goTo(locator.itemLocator)
  }

  async goToCurrentItemContentIndex(contentIndex: number): Promise<void> {
    const adapter = this.requireCurrentAdapter()
    const paginated = this.currentPaginatedText(adapter)
    if (paginated) {
      await paginated.goToPage(contentIndex)
      return
    }

    const locator = adapter.getCurrentLocator()

    if (locator.kind === 'PAGE') {
      await adapter.goTo({
        ...locator,
        page: contentIndex,
      })
      return
    }

    await adapter.goTo({
      ...locator,
      paragraph: contentIndex,
    })
  }

  async goToCurrentTextLocator(locator: ShelfViewTextLocator): Promise<void> {
    const adapter = this.requireCurrentAdapter()
    if (!(adapter instanceof TextReflowAdapter)) throw new Error('Current mixed item is not text.')
    await adapter.goTo(locator)
  }

  getCurrentLocator(): ShelfViewMixedLocator {
    const manifest = this.requireManifest()
    const item = this.requireCurrentItem()

    return {
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'MIXED',
      itemId: item.itemId,
      itemIndex: this.currentItemIndex,
      itemLocator: this.requireCurrentAdapter().getCurrentLocator() as ShelfViewPageLocator | ShelfViewTextLocator,
      contentFingerprint: manifest.contentFingerprint,
    }
  }

  getProgression(): number {
    if (this.items.length === 0) return 0
    return (this.currentItemIndex + this.requireCurrentAdapter().getProgression()) / this.items.length
  }

  getCurrentContentIndex(): number {
    const adapter = this.requireCurrentAdapter()
    const paginated = this.currentPaginatedText(adapter)
    if (paginated) return paginated.getCurrentPageIndex()

    const locator = adapter.getCurrentLocator()
    return locator.kind === 'PAGE' ? locator.page : locator.paragraph
  }

  getCurrentContentCount(): number {
    const adapter = this.requireCurrentAdapter()
    if (adapter instanceof FixedPageAdapter) return adapter.getPagesCount()
    return this.currentPaginatedText(adapter)?.getPageCount() ?? adapter.getParagraphsCount()
  }

  getCurrentTextLocator(): ShelfViewTextLocator | undefined {
    const adapter = this.currentAdapter
    return adapter instanceof TextReflowAdapter ? adapter.getCurrentLocator() : undefined
  }

  getParagraphLength(paragraphIndex: number): number {
    const adapter = this.currentAdapter
    return adapter instanceof TextReflowAdapter ? adapter.getParagraphLength(paragraphIndex) : 0
  }

  getVisibleParagraphs(layout: ShelfViewFixedPageLayout): ShelfViewTextParagraph[] {
    const adapter = this.currentAdapter
    return adapter instanceof TextReflowAdapter ? adapter.getVisibleParagraphs(layout) : []
  }

  getPageUrl(pageIndex: number): string | undefined {
    const adapter = this.currentAdapter
    return adapter instanceof FixedPageAdapter ? adapter.getPageUrl(pageIndex) : undefined
  }

  isCurrentFixedPage(): boolean {
    return this.currentAdapter instanceof FixedPageAdapter
  }

  isCurrentText(): boolean {
    return this.currentAdapter instanceof TextReflowAdapter
  }

  canGoPrevious(): boolean {
    const adapter = this.requireCurrentAdapter()
    const paginated = this.currentPaginatedText(adapter)
    const withinItem = paginated ? paginated.canGoPreviousPage() : adapter.canGoPrevious()
    return withinItem || this.currentItemIndex > 0
  }

  canGoNext(): boolean {
    const adapter = this.requireCurrentAdapter()
    const paginated = this.currentPaginatedText(adapter)
    const withinItem = paginated ? paginated.canGoNextPage() : adapter.canGoNext()
    return withinItem || this.currentItemIndex < this.items.length - 1
  }

  destroy(): void {
    this.openRequestId += 1
    this.currentAdapter?.destroy()
    this.currentAdapter = undefined
    this.manifest = undefined
    this.items = []
    this.currentItemIndex = 0
  }

  /**
   * The current item stays open until the next one has loaded, so a page turn
   * made while a text item is still downloading moves from the item on screen,
   * and a failed load leaves that item readable. Resolves false when a newer
   * open or destroy() superseded this one, and the item never became current.
   */
  private async openItem(itemIndex: number): Promise<boolean> {
    const manifest = this.requireManifest()
    const item = this.items[itemIndex]
    if (!item) throw new Error('The mixed manifest item is unavailable.')

    const requestId = ++this.openRequestId
    const adapter = item.kind === 'FIXED_PAGE_SEQUENCE'
      ? new FixedPageAdapter(this.resolvePageUrl, this.preloadPage)
      : new TextReflowAdapter(this.loadTextContent)
    try {
      await adapter.open(toItemManifest(manifest, item))
    } catch (e) {
      adapter.destroy()
      throw e
    }
    if (requestId !== this.openRequestId) {
      adapter.destroy()
      return false
    }

    if (adapter instanceof TextReflowAdapter) adapter.setPagination(this.charsPerPage)
    this.currentAdapter?.destroy()
    this.currentAdapter = adapter
    this.currentItemIndex = itemIndex
    return true
  }

  /**
   * The current item as a paginated text adapter, or undefined when it is a
   * fixed-page item or text without a page budget. Returning the adapter rather
   * than a boolean keeps the union intact for the callers that fall through.
   */
  private currentPaginatedText(adapter: MixedItemAdapter): TextReflowAdapter | undefined {
    return adapter instanceof TextReflowAdapter && this.charsPerPage > 0 ? adapter : undefined
  }

  private async goToLastLocationInCurrentItem(): Promise<void> {
    const adapter = this.requireCurrentAdapter()
    if (adapter instanceof FixedPageAdapter) {
      await this.goToCurrentItemContentIndex(adapter.getPagesCount() - 1)
      return
    }

    const paginated = this.currentPaginatedText(adapter)
    await this.goToCurrentItemContentIndex(
      (paginated?.getPageCount() ?? adapter.getParagraphsCount()) - 1,
    )
  }

  private itemMatchesLocator(
    item: ShelfViewManifestItem,
    locator: ShelfViewPageLocator | ShelfViewTextLocator,
  ): boolean {
    if (item.kind === 'FIXED_PAGE_SEQUENCE') return locator.kind === 'PAGE'
    if (item.kind === 'TEXT_REFLOW') return locator.kind === 'TEXT'
    return false
  }

  private isSupportedItem(item: ShelfViewManifestItem): boolean {
    return item.kind === 'FIXED_PAGE_SEQUENCE' || item.kind === 'TEXT_REFLOW'
  }

  private requireManifest(): ShelfViewManifest {
    if (!this.manifest) throw new Error('MixedContentAdapter is not open.')
    return this.manifest
  }

  private requireCurrentAdapter(): MixedItemAdapter {
    if (!this.currentAdapter) throw new Error('MixedContentAdapter has no current item.')
    return this.currentAdapter
  }

  private requireCurrentItem(): ShelfViewManifestItem {
    const item = this.items[this.currentItemIndex]
    if (!item) throw new Error('MixedContentAdapter has no current item.')
    return item
  }
}

function toItemManifest(
  manifest: ShelfViewManifest,
  item: ShelfViewManifestItem,
): ShelfViewManifest {
  return {
    schemaVersion: manifest.schemaVersion,
    bookId: manifest.bookId,
    title: item.label,
    kind: item.kind,
    sourceFormat: item.sourceFormat ?? manifest.sourceFormat,
    contentFingerprint: item.contentFingerprint,
    pageCount: item.pageCount,
    text: item.text,
    pages: item.pages,
    capabilities: {
      ...manifest.capabilities,
      textSelection: item.kind === 'TEXT_REFLOW',
      doublePage: item.kind === 'FIXED_PAGE_SEQUENCE' && manifest.capabilities.doublePage,
      imageFilters: item.kind === 'FIXED_PAGE_SEQUENCE' && manifest.capabilities.imageFilters,
    },
  }
}
