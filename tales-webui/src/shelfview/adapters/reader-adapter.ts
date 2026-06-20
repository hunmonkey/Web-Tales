import {ShelfViewManifest} from '@/shelfview/models/shelfview-manifest'
import {ShelfViewLocator} from '@/shelfview/models/shelfview-locator'

export interface ReaderAdapter {
  open(manifest: ShelfViewManifest): Promise<void>
  next(): Promise<void>
  previous(): Promise<void>
  goTo(locator: ShelfViewLocator): Promise<void>
  getCurrentLocator(): ShelfViewLocator
  getProgression(): number
  destroy(): void
}
