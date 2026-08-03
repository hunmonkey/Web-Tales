import {shelfViewBookshelfBadge, shelfViewSeriesContinuation} from '@/shelfview/functions/bookshelf-experience'
import {MediaDto} from '@/types/tales-books'
import {SeriesDto} from '@/types/tales-series'
import {ReadStatus} from '@/types/enum-books'

const media = (mediaProfile: string, status = 'READY'): MediaDto => ({
  status,
  mediaType: 'application/zip',
  pagesCount: 12,
  comment: '',
  mediaProfile,
  epubDivinaCompatible: false,
})

const series = (overrides: Partial<SeriesDto> = {}): SeriesDto => ({
  id: 'series-1',
  libraryId: 'library-1',
  name: 'Series',
  url: '',
  created: new Date('2026-06-20T00:00:00Z'),
  lastModified: new Date('2026-06-20T00:00:00Z'),
  booksCount: 4,
  booksReadCount: 1,
  booksUnreadCount: 1,
  booksInProgressCount: 2,
  metadata: {
    status: 'ONGOING',
    statusLock: false,
    created: '',
    lastModified: '',
    title: 'Series',
    titleLock: false,
    titleSort: 'Series',
    titleSortLock: false,
    summary: '',
    summaryLock: false,
    readingDirection: '',
    readingDirectionLock: false,
    publisher: '',
    publisherLock: false,
    language: '',
    languageLock: false,
    ageRatingLock: false,
    genres: [],
    genresLock: false,
    tags: [],
    tagsLock: false,
    totalBookCountLock: false,
    sharingLabels: [],
    sharingLabelsLock: false,
    links: [],
    linksLock: false,
    alternateTitles: [],
    alternateTitlesLock: false,
  },
  booksMetadata: {
    created: '',
    lastModified: '',
    authors: [],
    tags: [],
    releaseDate: '',
    summary: '',
    summaryNumber: '',
  },
  deleted: false,
  oneshot: false,
  context: undefined as any,
  ...overrides,
})

describe('ShelfView bookshelf experience', () => {
  it('shows a Tales badge for ready fixed-page books when the reader flag is enabled', () => {
    expect(shelfViewBookshelfBadge(media('DIVINA'), true)).toMatchObject({
      kind: 'FIXED_PAGE',
      icon: 'mdi-book-open-page-variant',
      labelKey: 'shelfview_bookshelf.badge_label',
    })
  })

  it.each([
    ['PDF', 'PDF', 'mdi-file-pdf-box'],
    ['TXT', 'TEXT', 'mdi-format-text'],
    ['IMAGE_DIRECTORY', 'IMAGE_DIRECTORY', 'mdi-folder-image'],
  ])('uses a format-specific badge for %s books', (mediaProfile, kind, icon) => {
    expect(shelfViewBookshelfBadge(media(mediaProfile), true)).toMatchObject({
      kind,
      icon,
    })
  })

  it('hides the badge when the new reader flag is disabled', () => {
    expect(shelfViewBookshelfBadge(media('DIVINA'), false)).toBeUndefined()
  })

  it('hides the badge for books that are not ready', () => {
    expect(shelfViewBookshelfBadge(media('TXT', 'UNKNOWN'), true)).toBeUndefined()
  })

  it('hides the badge for formats that still use a legacy route', () => {
    expect(shelfViewBookshelfBadge(media('EPUB'), true)).toBeUndefined()
  })

  it('shows a continuation route for series with in-progress books', () => {
    expect(shelfViewSeriesContinuation(series())).toMatchObject({
      count: 2,
      icon: 'mdi-progress-clock',
      labelKey: 'shelfview_bookshelf.continue_label',
      to: {
        name: 'browse-series',
        params: {seriesId: 'series-1'},
        query: {
          readStatus: [
            {
              readStatus: {
                operator: 'is',
                value: ReadStatus.IN_PROGRESS,
              },
            },
          ],
        },
      },
    })
  })

  it.each([
    ['no in-progress books', {booksInProgressCount: 0}],
    ['deleted series', {deleted: true}],
    ['oneshots', {oneshot: true}],
  ])('hides the continuation route for %s', (_label, overrides) => {
    expect(shelfViewSeriesContinuation(series(overrides))).toBeUndefined()
  })
})
