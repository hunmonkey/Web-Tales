import {AxiosInstance} from 'axios'
import ShelfViewStateService from '@/shelfview/services/shelfview-state.service'
import {ShelfViewApiError} from '@/shelfview/models/shelfview-errors'
import {SHELFVIEW_LOCATOR_SCHEMA_VERSION, ShelfViewPageLocator} from '@/shelfview/models/shelfview-locator'
import {SHELFVIEW_HIGHLIGHT_SELECTOR_SCHEMA_VERSION, ShelfViewTextRangeHighlightSelector} from '@/shelfview/models/shelfview-state'

const locator: ShelfViewPageLocator = {
  schemaVersion: SHELFVIEW_LOCATOR_SCHEMA_VERSION,
  kind: 'PAGE',
  page: 1,
  offset: 0,
  contentFingerprint: 'tales-file-state:book-1',
}

const selector: ShelfViewTextRangeHighlightSelector = {
  schemaVersion: SHELFVIEW_HIGHLIGHT_SELECTOR_SCHEMA_VERSION,
  kind: 'TEXT_RANGE',
  startParagraph: 0,
  startCharacterOffset: 0,
  endParagraph: 0,
  endCharacterOffset: 12,
  normalizationVersion: 1,
}

describe('ShelfViewStateService', () => {
  it('fetches and saves reader position', async () => {
    const get = jest.fn().mockResolvedValue({
      status: 200,
      data: {locator, progression: 0.5, contentFingerprint: locator.contentFingerprint, updatedAt: '2026-06-18T00:00:00Z'},
    })
    const put = jest.fn().mockResolvedValue({
      data: {locator, progression: 0.5, contentFingerprint: locator.contentFingerprint, updatedAt: '2026-06-18T00:00:00Z'},
    })
    const service = new ShelfViewStateService({get, put} as unknown as AxiosInstance)

    await expect(service.getPosition('book-1')).resolves.toMatchObject({locator})
    await expect(service.savePosition('book-1', {locator, progression: 0.5})).resolves.toMatchObject({locator})

    expect(get).toHaveBeenCalledWith('/api/shelfview/v1/books/book-1/position')
    expect(put).toHaveBeenCalledWith('/api/shelfview/v1/books/book-1/position', {locator, progression: 0.5})
  })

  it('maps empty reader position responses to undefined', async () => {
    const get = jest.fn().mockResolvedValue({status: 204})
    const service = new ShelfViewStateService({get} as unknown as AxiosInstance)

    await expect(service.getPosition('book-1')).resolves.toBeUndefined()
  })

  it('fetches, creates, updates and deletes bookmarks', async () => {
    const bookmark = {id: 'bookmark-1', bookId: 'book-1', locator, label: 'Page 2', createdAt: 'now', updatedAt: 'now'}
    const get = jest.fn().mockResolvedValue({data: [bookmark]})
    const post = jest.fn().mockResolvedValue({data: bookmark})
    const put = jest.fn().mockResolvedValue({data: {...bookmark, label: 'Updated'}})
    const deleteFn = jest.fn().mockResolvedValue({})
    const service = new ShelfViewStateService({get, post, put, delete: deleteFn} as unknown as AxiosInstance)

    await expect(service.getBookmarks('book-1')).resolves.toEqual([bookmark])
    await expect(service.createBookmark('book-1', {locator, label: 'Page 2'})).resolves.toEqual(bookmark)
    await expect(service.updateBookmark('bookmark-1', {locator, label: 'Updated'})).resolves.toMatchObject({label: 'Updated'})
    await service.deleteBookmark('bookmark-1')

    expect(get).toHaveBeenCalledWith('/api/shelfview/v1/books/book-1/bookmarks')
    expect(post).toHaveBeenCalledWith('/api/shelfview/v1/books/book-1/bookmarks', {locator, label: 'Page 2'})
    expect(put).toHaveBeenCalledWith('/api/shelfview/v1/bookmarks/bookmark-1', {locator, label: 'Updated'})
    expect(deleteFn).toHaveBeenCalledWith('/api/shelfview/v1/bookmarks/bookmark-1')
  })

  it('fetches, creates, updates and deletes highlights', async () => {
    const highlight = {
      id: 'highlight-1',
      bookId: 'book-1',
      locator,
      selector,
      contentFingerprint: locator.contentFingerprint,
      color: 'YELLOW',
      note: 'Important',
      textQuote: 'Opening text',
      createdAt: 'now',
      updatedAt: 'now',
    }
    const highlightUpdate = {locator, selector, color: 'BLUE' as const, note: 'Updated', textQuote: 'Updated text'}
    const get = jest.fn().mockResolvedValue({data: [highlight]})
    const post = jest.fn().mockResolvedValue({data: highlight})
    const put = jest.fn().mockResolvedValue({data: {...highlight, ...highlightUpdate}})
    const deleteFn = jest.fn().mockResolvedValue({})
    const service = new ShelfViewStateService({get, post, put, delete: deleteFn} as unknown as AxiosInstance)

    await expect(service.getHighlights('book-1')).resolves.toEqual([highlight])
    await expect(service.createHighlight('book-1', highlightUpdate)).resolves.toEqual(highlight)
    await expect(service.updateHighlight('highlight-1', highlightUpdate)).resolves.toMatchObject({color: 'BLUE'})
    await service.deleteHighlight('highlight-1')

    expect(get).toHaveBeenCalledWith('/api/shelfview/v1/books/book-1/highlights')
    expect(post).toHaveBeenCalledWith('/api/shelfview/v1/books/book-1/highlights', highlightUpdate)
    expect(put).toHaveBeenCalledWith('/api/shelfview/v1/highlights/highlight-1', highlightUpdate)
    expect(deleteFn).toHaveBeenCalledWith('/api/shelfview/v1/highlights/highlight-1')
  })

  it('fetches and saves preferences', async () => {
    const preference = {
      scopeType: 'BOOK',
      scopeId: 'book-1',
      format: 'FIXED_PAGE_SEQUENCE',
      effectiveSettings: {layout: 'DOUBLE_PAGE'},
      sources: [],
    }
    const get = jest.fn().mockResolvedValue({data: preference})
    const put = jest.fn().mockResolvedValue({data: preference})
    const service = new ShelfViewStateService({get, put} as unknown as AxiosInstance)

    await expect(service.getPreference({scopeType: 'BOOK', scopeId: 'book-1', format: 'FIXED_PAGE_SEQUENCE'})).resolves.toEqual(preference)
    await expect(service.savePreference({
      scopeType: 'BOOK',
      scopeId: 'book-1',
      format: 'FIXED_PAGE_SEQUENCE',
      settings: {layout: 'DOUBLE_PAGE'},
    })).resolves.toEqual(preference)

    expect(get).toHaveBeenCalledWith('/api/shelfview/v1/preferences', {
      params: {scopeType: 'BOOK', scopeId: 'book-1', format: 'FIXED_PAGE_SEQUENCE'},
    })
    expect(put).toHaveBeenCalledWith('/api/shelfview/v1/preferences', {
      scopeType: 'BOOK',
      scopeId: 'book-1',
      format: 'FIXED_PAGE_SEQUENCE',
      settings: {layout: 'DOUBLE_PAGE'},
    })
  })

  it('maps state API failures to ShelfView errors', async () => {
    const get = jest.fn().mockRejectedValue({
      response: {
        status: 503,
        data: {message: 'Offline'},
      },
    })
    const service = new ShelfViewStateService({get} as unknown as AxiosInstance)

    await expect(service.getBookmarks('book-1')).rejects.toEqual(
      new ShelfViewApiError('An error occurred while trying to retrieve ShelfView bookmarks: Offline', 'UNAVAILABLE', 503),
    )
  })
})
