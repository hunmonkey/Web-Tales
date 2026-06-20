import {MediaDto} from '@/types/tales-books'
import {isShelfViewReaderEnabled} from '@/shelfview/feature-flags'
import {SeriesDto} from '@/types/tales-series'
import {RawLocation} from 'vue-router'
import {ReadStatus} from '@/types/enum-books'
import {SearchConditionReadStatus, SearchOperatorIs} from '@/types/tales-search'

export type ShelfViewBookshelfBadgeKind = 'FIXED_PAGE' | 'TEXT' | 'PDF' | 'IMAGE_DIRECTORY'

export interface ShelfViewBookshelfBadge {
  kind: ShelfViewBookshelfBadgeKind,
  icon: string,
  labelKey: string,
  tooltipKey: string,
}

export interface ShelfViewSeriesContinuation {
  count: number,
  icon: string,
  labelKey: string,
  tooltipKey: string,
  to: RawLocation,
}

const BADGE_BASE = {
  labelKey: 'shelfview_bookshelf.badge_label',
}

export function shelfViewBookshelfBadge(media: MediaDto, readerEnabled = isShelfViewReaderEnabled()): ShelfViewBookshelfBadge | undefined {
  if (!readerEnabled || media.status !== 'READY') return undefined

  switch (media.mediaProfile.toLowerCase()) {
    case 'divina':
      return {
        ...BADGE_BASE,
        kind: 'FIXED_PAGE',
        icon: 'mdi-book-open-page-variant',
        tooltipKey: 'shelfview_bookshelf.badge_fixed_page',
      }
    case 'pdf':
      return {
        ...BADGE_BASE,
        kind: 'PDF',
        icon: 'mdi-file-pdf-box',
        tooltipKey: 'shelfview_bookshelf.badge_pdf',
      }
    case 'txt':
      return {
        ...BADGE_BASE,
        kind: 'TEXT',
        icon: 'mdi-format-text',
        tooltipKey: 'shelfview_bookshelf.badge_text',
      }
    case 'image_directory':
      return {
        ...BADGE_BASE,
        kind: 'IMAGE_DIRECTORY',
        icon: 'mdi-folder-image',
        tooltipKey: 'shelfview_bookshelf.badge_image_directory',
      }
    default:
      return undefined
  }
}

export function shelfViewSeriesContinuation(series: SeriesDto): ShelfViewSeriesContinuation | undefined {
  if (series.deleted || series.oneshot || series.booksInProgressCount <= 0) return undefined

  return {
    count: series.booksInProgressCount,
    icon: 'mdi-progress-clock',
    labelKey: 'shelfview_bookshelf.continue_label',
    tooltipKey: 'shelfview_bookshelf.continue_tooltip',
    to: {
      name: 'browse-series',
      params: {seriesId: series.id},
      query: {
        readStatus: [new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.IN_PROGRESS))],
      } as any,
    },
  }
}
