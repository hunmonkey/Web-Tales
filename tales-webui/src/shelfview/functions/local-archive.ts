export const SHELFVIEW_LOCAL_ARCHIVE_MAX_ENTRIES = 2000
export const SHELFVIEW_LOCAL_ARCHIVE_MAX_IMAGES = 1000
export const SHELFVIEW_LOCAL_ARCHIVE_MAX_IMAGE_BYTES = 80 * 1024 * 1024
export const SHELFVIEW_LOCAL_ARCHIVE_MAX_TOTAL_IMAGE_BYTES = 512 * 1024 * 1024

export interface ShelfViewLocalArchivePage {
  index: number,
  name: string,
  url: string,
  size: number,
  mimeType: string,
}

export interface ShelfViewLocalArchiveBook {
  title: string,
  pages: ShelfViewLocalArchivePage[],
  close(): void,
}

export interface ShelfViewLocalArchiveEntry {
  filename: string,
  directory: boolean,
  uncompressedSize: number,
  encrypted?: boolean,
  getBlob(mimeType: string): Promise<Blob>,
}

export interface ShelfViewLocalArchiveSource {
  entries: ShelfViewLocalArchiveEntry[],
  close(): Promise<void>,
}

export interface ShelfViewLocalArchiveOpenOptions {
  openSource?: (file: File) => Promise<ShelfViewLocalArchiveSource>,
  createObjectUrl?: (blob: Blob) => string,
  revokeObjectUrl?: (url: string) => void,
}

interface ShelfViewLocalArchiveCandidate {
  entry: ShelfViewLocalArchiveEntry,
  path: string,
  mimeType: string,
}

const IMAGE_MIME_BY_EXTENSION: Record<string, string> = {
  gif: 'image/gif',
  jpeg: 'image/jpeg',
  jpg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp',
}

export async function openShelfViewLocalArchiveFile(
  file: File,
  options: ShelfViewLocalArchiveOpenOptions = {},
): Promise<ShelfViewLocalArchiveBook> {
  const openSource = options.openSource ?? openZipJsArchiveSource
  const createObjectUrl = options.createObjectUrl ?? createDefaultObjectUrl
  const revokeObjectUrl = options.revokeObjectUrl ?? revokeDefaultObjectUrl
  const source = await openSource(file)
  const createdUrls: string[] = []

  try {
    if (source.entries.length > SHELFVIEW_LOCAL_ARCHIVE_MAX_ENTRIES) {
      throw new Error('The archive has too many entries.')
    }

    const candidates = selectImageEntries(source.entries)
    if (candidates.length === 0) throw new Error('The archive does not contain supported image pages.')
    if (candidates.length > SHELFVIEW_LOCAL_ARCHIVE_MAX_IMAGES) {
      throw new Error('The archive has too many image pages.')
    }

    const pages: ShelfViewLocalArchivePage[] = []
    for (const candidate of candidates) {
      const blob = await candidate.entry.getBlob(candidate.mimeType)
      if (blob.size > SHELFVIEW_LOCAL_ARCHIVE_MAX_IMAGE_BYTES) {
        throw new Error('An archive image is over the local image size limit.')
      }

      const url = createObjectUrl(blob)
      createdUrls.push(url)
      pages.push({
        index: pages.length,
        name: candidate.path,
        url,
        size: blob.size,
        mimeType: candidate.mimeType,
      })
    }

    return {
      title: localArchiveTitle(file.name),
      pages,
      close() {
        createdUrls.splice(0).forEach(url => revokeObjectUrl(url))
      },
    }
  } catch (error) {
    createdUrls.splice(0).forEach(url => revokeObjectUrl(url))
    throw error
  } finally {
    await source.close()
  }
}

export function normalizeShelfViewLocalArchivePath(filename: string): string | undefined {
  const withoutDriveSeparator = filename.replace(/\\/g, '/')
  if (/^[a-zA-Z]:/.test(withoutDriveSeparator)) return undefined
  if (withoutDriveSeparator.startsWith('/')) return undefined

  const segments = withoutDriveSeparator
    .split('/')
    .filter(segment => segment.length > 0)

  if (segments.length === 0) return undefined
  if (segments.some(segment => segment === '.' || segment === '..')) return undefined

  return segments.join('/')
}

export function shelfViewLocalArchiveImageMimeType(filename: string): string | undefined {
  const extension = filename.split('.').pop()?.toLowerCase()
  if (!extension) return undefined
  return IMAGE_MIME_BY_EXTENSION[extension]
}

function selectImageEntries(entries: ShelfViewLocalArchiveEntry[]): ShelfViewLocalArchiveCandidate[] {
  let totalImageBytes = 0
  const candidates: ShelfViewLocalArchiveCandidate[] = []

  for (const entry of entries) {
    if (entry.directory || entry.encrypted) continue

    const path = normalizeShelfViewLocalArchivePath(entry.filename)
    if (!path) continue

    const mimeType = shelfViewLocalArchiveImageMimeType(path)
    if (!mimeType) continue

    if (!Number.isFinite(entry.uncompressedSize) || entry.uncompressedSize < 0) {
      throw new Error('An archive image has an invalid size.')
    }
    if (entry.uncompressedSize > SHELFVIEW_LOCAL_ARCHIVE_MAX_IMAGE_BYTES) {
      throw new Error('An archive image is over the local image size limit.')
    }

    totalImageBytes += entry.uncompressedSize
    if (totalImageBytes > SHELFVIEW_LOCAL_ARCHIVE_MAX_TOTAL_IMAGE_BYTES) {
      throw new Error('The archive images are over the local total size limit.')
    }

    candidates.push({entry, path, mimeType})
  }

  return candidates.sort((a, b) => a.path.localeCompare(b.path, undefined, {
    numeric: true,
    sensitivity: 'base',
  }))
}

function localArchiveTitle(filename: string): string {
  return filename.replace(/\.[^.]+$/, '') || filename
}

function createDefaultObjectUrl(blob: Blob): string {
  if (typeof URL.createObjectURL !== 'function') {
    throw new Error('Object URL support is unavailable.')
  }
  return URL.createObjectURL(blob)
}

function revokeDefaultObjectUrl(url: string): void {
  if (typeof URL.revokeObjectURL === 'function') URL.revokeObjectURL(url)
}

async function openZipJsArchiveSource(file: File): Promise<ShelfViewLocalArchiveSource> {
  const zip = await import('@zip.js/zip.js')
  const reader = new zip.ZipReader(new zip.BlobReader(file), {useWebWorkers: false})
  const entries = await reader.getEntries()

  return {
    entries: entries.map(entry => ({
      filename: entry.filename,
      directory: entry.directory,
      uncompressedSize: entry.uncompressedSize,
      encrypted: entry.encrypted,
      getBlob(mimeType: string) {
        if (entry.directory) throw new Error('Directory entries cannot be opened as images.')
        return entry.getData(new zip.BlobWriter(mimeType), {useWebWorkers: false})
      },
    })),
    close() {
      return reader.close()
    },
  }
}
