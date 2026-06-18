import {isShelfViewReaderEnabled} from '@/shelfview/feature-flags'

describe('ShelfView feature flags', () => {
  it.each(['1', 'true', 'TRUE', 'yes', 'on'])('enables the reader for %s', value => {
    expect(isShelfViewReaderEnabled(value)).toBe(true)
  })

  it.each([undefined, '', '0', 'false', 'off'])('keeps the reader disabled for %s', value => {
    expect(isShelfViewReaderEnabled(value)).toBe(false)
  })
})
