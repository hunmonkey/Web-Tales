import {ReaderAdapter} from '@/shelfview/adapters/reader-adapter'
import {ShelfViewFixedPageLayout} from '@/shelfview/functions/fixed-page-view'
import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {
  SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  ShelfViewLocator,
  ShelfViewTextLocator,
} from '@/shelfview/models/shelfview-locator'
import {ShelfViewTextContent, ShelfViewTextParagraph} from '@/shelfview/models/shelfview-text'

export type ShelfViewTextContentLoader = (manifest: ShelfViewManifest) => Promise<ShelfViewTextContent>

export class TextReflowAdapter implements ReaderAdapter {
  private manifest: ShelfViewManifest | undefined
  private content: ShelfViewTextContent | undefined
  private currentParagraphIndex = 0
  private currentCharacterOffset = 0

  constructor(
    private readonly loadTextContent: ShelfViewTextContentLoader,
  ) {
  }

  async open(manifest: ShelfViewManifest): Promise<void> {
    if (manifest.kind !== 'TEXT_REFLOW') {
      throw new Error('TextReflowAdapter can only open text manifests.')
    }

    const content = await this.loadTextContent(manifest)
    if (content.contentFingerprint !== manifest.contentFingerprint) {
      throw new Error('ShelfView text content belongs to a different book version.')
    }
    if (content.normalizationVersion !== manifest.text?.normalizationVersion) {
      throw new Error('ShelfView text content uses an unsupported normalization version.')
    }

    this.manifest = manifest
    this.content = content
    this.currentParagraphIndex = 0
  }

  async next(): Promise<void> {
    this.moveToParagraph(this.currentParagraphIndex + 1, 0)
  }

  async previous(): Promise<void> {
    this.moveToParagraph(this.currentParagraphIndex - 1, 0)
  }

  async goTo(locator: ShelfViewLocator): Promise<void> {
    const manifest = this.requireManifest()

    if (locator.schemaVersion !== SHELFVIEW_LOCATOR_SCHEMA_VERSION || locator.kind !== 'TEXT') {
      throw new Error('TextReflowAdapter only accepts text locators.')
    }
    if (locator.contentFingerprint !== manifest.contentFingerprint) {
      throw new Error('The text locator belongs to a different book version.')
    }
    if (locator.normalizationVersion !== manifest.text?.normalizationVersion) {
      throw new Error('The text locator uses an unsupported normalization version.')
    }
    if (!Number.isInteger(locator.paragraph) || locator.paragraph < 0) {
      throw new Error('The text locator paragraph is invalid.')
    }
    if (!Number.isInteger(locator.characterOffset) || locator.characterOffset < 0) {
      throw new Error('The text locator character offset is invalid.')
    }
    if (!Number.isFinite(locator.progression) || locator.progression < 0 || locator.progression > 1) {
      throw new Error('The text locator progression is invalid.')
    }

    this.moveToParagraph(locator.paragraph, locator.characterOffset)
  }

  getCurrentLocator(): ShelfViewTextLocator {
    const manifest = this.requireManifest()

    return {
      schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
      kind: 'TEXT',
      paragraph: this.currentParagraphIndex,
      characterOffset: this.currentCharacterOffset,
      progression: this.getProgression(),
      normalizationVersion: manifest.text?.normalizationVersion ?? 1,
      contentFingerprint: manifest.contentFingerprint,
    }
  }

  getProgression(): number {
    const paragraphCount = this.getParagraphsCount()
    if (paragraphCount < 1) return 0

    const paragraphLength = this.getParagraphLength(this.currentParagraphIndex)
    const paragraphProgression = paragraphLength > 0
      ? this.currentCharacterOffset / paragraphLength
      : 0

    return clamp((this.currentParagraphIndex + paragraphProgression) / paragraphCount, 0, 1)
  }

  getCurrentParagraphIndex(): number {
    return this.currentParagraphIndex
  }

  getCurrentCharacterOffset(): number {
    return this.currentCharacterOffset
  }

  getParagraphsCount(): number {
    return this.content?.paragraphs.length ?? 0
  }

  getParagraphLength(paragraphIndex: number): number {
    return this.content?.paragraphs[paragraphIndex]?.length ?? 0
  }

  getVisibleParagraphs(layout: ShelfViewFixedPageLayout): ShelfViewTextParagraph[] {
    const paragraphs = this.content?.paragraphs ?? []
    if (layout === 'CONTINUOUS_SCROLL') return toTextParagraphs(paragraphs)

    const text = paragraphs[this.currentParagraphIndex]
    return text === undefined
      ? []
      : [{paragraphIndex: this.currentParagraphIndex, text}]
  }

  canGoPrevious(): boolean {
    return this.currentParagraphIndex > 0
  }

  canGoNext(): boolean {
    return this.currentParagraphIndex < this.getParagraphsCount() - 1
  }

  destroy(): void {
    this.manifest = undefined
    this.content = undefined
    this.currentParagraphIndex = 0
    this.currentCharacterOffset = 0
  }

  private moveToParagraph(
    paragraphIndex: number,
    characterOffset: number,
  ): void {
    this.requireManifest()

    const paragraphCount = this.getParagraphsCount()
    if (paragraphCount < 1) {
      this.currentParagraphIndex = 0
      this.currentCharacterOffset = 0
      return
    }

    this.currentParagraphIndex = Math.max(0, Math.min(Math.floor(paragraphIndex), paragraphCount - 1))
    this.currentCharacterOffset = Math.max(0, Math.min(Math.floor(characterOffset), this.getParagraphLength(this.currentParagraphIndex)))
  }

  private requireManifest(): ShelfViewManifest {
    if (!this.manifest) throw new Error('TextReflowAdapter is not open.')
    return this.manifest
  }
}

function clamp(
  value: number,
  min: number,
  max: number,
): number {
  return Math.max(min, Math.min(value, max))
}

function toTextParagraphs(paragraphs: string[]): ShelfViewTextParagraph[] {
  return paragraphs.map((text, paragraphIndex) => ({
    paragraphIndex,
    text,
  }))
}
