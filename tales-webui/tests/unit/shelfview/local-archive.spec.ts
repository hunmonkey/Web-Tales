import {
  SHELFVIEW_LOCAL_ARCHIVE_MAX_ENTRIES,
  SHELFVIEW_LOCAL_ARCHIVE_MAX_IMAGE_BYTES,
  SHELFVIEW_LOCAL_ARCHIVE_MAX_TOTAL_IMAGE_BYTES,
  ShelfViewLocalArchiveEntry,
  normalizeShelfViewLocalArchivePath,
  openShelfViewLocalArchiveFile,
  shelfViewLocalArchiveImageMimeType,
} from '@/shelfview/functions/local-archive'

describe('local archive opening', () => {
  it('normalizes only safe relative archive paths', () => {
    expect(normalizeShelfViewLocalArchivePath('chapter\\page-1.jpg')).toBe('chapter/page-1.jpg')
    expect(normalizeShelfViewLocalArchivePath('/page-1.jpg')).toBeUndefined()
    expect(normalizeShelfViewLocalArchivePath('C:\\books\\page-1.jpg')).toBeUndefined()
    expect(normalizeShelfViewLocalArchivePath('../page-1.jpg')).toBeUndefined()
    expect(normalizeShelfViewLocalArchivePath('./page-1.jpg')).toBeUndefined()
  })

  it('detects supported local image mime types', () => {
    expect(shelfViewLocalArchiveImageMimeType('page.JPG')).toBe('image/jpeg')
    expect(shelfViewLocalArchiveImageMimeType('page.png')).toBe('image/png')
    expect(shelfViewLocalArchiveImageMimeType('page.svg')).toBeUndefined()
    expect(shelfViewLocalArchiveImageMimeType('page.txt')).toBeUndefined()
  })

  it('opens safe image entries in natural path order', async () => {
    const close = jest.fn().mockResolvedValue(undefined)
    const createObjectUrl = jest
      .fn()
      .mockReturnValueOnce('blob:page-1')
      .mockReturnValueOnce('blob:page-2')
    const revokeObjectUrl = jest.fn()
    const entries = [
      imageEntry('pages/page-10.jpg', 10),
      imageEntry('../unsafe.jpg', 10),
      imageEntry('notes.txt', 10),
      imageEntry('pages/page-2.jpg', 10),
    ]

    const book = await openShelfViewLocalArchiveFile(file('My Book.cbz'), {
      openSource: async () => ({entries, close}),
      createObjectUrl,
      revokeObjectUrl,
    })

    expect(book.title).toBe('My Book')
    expect(book.pages.map(page => page.name)).toEqual(['pages/page-2.jpg', 'pages/page-10.jpg'])
    expect(book.pages.map(page => page.url)).toEqual(['blob:page-1', 'blob:page-2'])
    expect(createObjectUrl).toHaveBeenCalledTimes(2)
    expect(close).toHaveBeenCalledTimes(1)

    book.close()
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:page-1')
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:page-2')
  })

  it('rejects archives with too many entries before extracting images', async () => {
    const entries = Array.from(
      {length: SHELFVIEW_LOCAL_ARCHIVE_MAX_ENTRIES + 1},
      (_, index) => imageEntry(`page-${index}.jpg`, 1),
    )

    await expect(openShelfViewLocalArchiveFile(file('big.cbz'), {
      openSource: async () => ({entries, close: jest.fn().mockResolvedValue(undefined)}),
    })).rejects.toThrow('too many entries')
  })

  it('rejects image entries over the per-image limit', async () => {
    await expect(openShelfViewLocalArchiveFile(file('large.cbz'), {
      openSource: async () => ({
        entries: [imageEntry('page-1.jpg', SHELFVIEW_LOCAL_ARCHIVE_MAX_IMAGE_BYTES + 1)],
        close: jest.fn().mockResolvedValue(undefined),
      }),
    })).rejects.toThrow('image size limit')
  })

  it('rejects image totals over the archive limit', async () => {
    await expect(openShelfViewLocalArchiveFile(file('large.cbz'), {
      openSource: async () => ({
        entries: Array.from(
          {length: Math.ceil(SHELFVIEW_LOCAL_ARCHIVE_MAX_TOTAL_IMAGE_BYTES / SHELFVIEW_LOCAL_ARCHIVE_MAX_IMAGE_BYTES) + 1},
          (_, index) => imageEntry(`page-${index}.jpg`, SHELFVIEW_LOCAL_ARCHIVE_MAX_IMAGE_BYTES),
        ),
        close: jest.fn().mockResolvedValue(undefined),
      }),
    })).rejects.toThrow('total size limit')
  })

  it('revokes already-created object URLs when extraction fails', async () => {
    const createObjectUrl = jest.fn().mockReturnValue('blob:page-1')
    const revokeObjectUrl = jest.fn()

    await expect(openShelfViewLocalArchiveFile(file('broken.cbz'), {
      openSource: async () => ({
        entries: [
          imageEntry('page-1.jpg', 1),
          imageEntry('page-2.jpg', 1, new Error('broken entry')),
        ],
        close: jest.fn().mockResolvedValue(undefined),
      }),
      createObjectUrl,
      revokeObjectUrl,
    })).rejects.toThrow('broken entry')

    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:page-1')
  })
})

function file(name: string): File {
  return new File(['zip'], name, {type: 'application/zip'})
}

function imageEntry(
  filename: string,
  uncompressedSize: number,
  error?: Error,
): ShelfViewLocalArchiveEntry {
  return {
    filename,
    directory: false,
    uncompressedSize,
    getBlob: jest.fn(async mimeType => {
      if (error) throw error
      return new Blob(['image'], {type: mimeType})
    }),
  }
}
