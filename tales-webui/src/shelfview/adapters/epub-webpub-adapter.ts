import {ShelfViewEpub, ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'

export class EpubWebPubAdapter {
  private epub: ShelfViewEpub | undefined

  async open(manifest: ShelfViewManifest): Promise<void> {
    if (manifest.kind !== 'EPUB_WEBPUB') {
      throw new Error('EpubWebPubAdapter can only open EPUB WebPub manifests.')
    }

    const webpubManifestEndpoint = manifest.epub?.webpubManifestEndpoint?.trim()
    const positionsEndpoint = manifest.epub?.positionsEndpoint?.trim()
    if (!webpubManifestEndpoint || !positionsEndpoint) {
      throw new Error('EPUB WebPub manifest endpoints are missing.')
    }

    this.epub = {
      webpubManifestEndpoint,
      positionsEndpoint,
    }
  }

  getWebPubManifestEndpoint(): string {
    return this.requireEpub().webpubManifestEndpoint
  }

  getPositionsEndpoint(): string {
    return this.requireEpub().positionsEndpoint
  }

  destroy(): void {
    this.epub = undefined
  }

  private requireEpub(): ShelfViewEpub {
    if (!this.epub) throw new Error('EpubWebPubAdapter is not open.')
    return this.epub
  }
}
