import {
  shelfViewReaderKeyboardAction,
  shelfViewShouldIgnoreShortcutTarget,
  shelfViewTouchAction,
} from '@/shelfview/functions/reader-input'

describe('ShelfView reader input helpers', () => {
  it('maps keyboard navigation for LTR and RTL reading', () => {
    expect(shelfViewReaderKeyboardAction('ArrowRight', 'LTR')).toBe('NEXT')
    expect(shelfViewReaderKeyboardAction('ArrowLeft', 'LTR')).toBe('PREVIOUS')
    expect(shelfViewReaderKeyboardAction('ArrowRight', 'RTL')).toBe('PREVIOUS')
    expect(shelfViewReaderKeyboardAction('ArrowLeft', 'RTL')).toBe('NEXT')
  })

  it('maps paging, boundary and help keys', () => {
    expect(shelfViewReaderKeyboardAction('PageDown', 'LTR')).toBe('NEXT')
    expect(shelfViewReaderKeyboardAction(' ', 'LTR')).toBe('NEXT')
    expect(shelfViewReaderKeyboardAction('PageUp', 'LTR')).toBe('PREVIOUS')
    expect(shelfViewReaderKeyboardAction('Home', 'LTR')).toBe('FIRST')
    expect(shelfViewReaderKeyboardAction('End', 'LTR')).toBe('LAST')
    expect(shelfViewReaderKeyboardAction('?', 'LTR')).toBe('SHOW_SHORTCUTS')
    expect(shelfViewReaderKeyboardAction('F1', 'LTR')).toBe('SHOW_SHORTCUTS')
    expect(shelfViewReaderKeyboardAction('x', 'LTR')).toBeUndefined()
  })

  it('maps horizontal touch swipes and ignores vertical scroll gestures', () => {
    expect(shelfViewTouchAction({x: 200, y: 100}, {x: 40, y: 104}, 'LTR')).toBe('NEXT')
    expect(shelfViewTouchAction({x: 40, y: 100}, {x: 200, y: 104}, 'LTR')).toBe('PREVIOUS')
    expect(shelfViewTouchAction({x: 200, y: 100}, {x: 40, y: 104}, 'RTL')).toBe('PREVIOUS')
    expect(shelfViewTouchAction({x: 40, y: 100}, {x: 200, y: 104}, 'RTL')).toBe('NEXT')
    expect(shelfViewTouchAction({x: 100, y: 100}, {x: 120, y: 260}, 'LTR')).toBeUndefined()
  })

  it('does not intercept shortcuts from editable targets', () => {
    const input = document.createElement('input')
    const textarea = document.createElement('textarea')
    const editable = document.createElement('div')
    editable.contentEditable = 'true'

    expect(shelfViewShouldIgnoreShortcutTarget(input)).toBe(true)
    expect(shelfViewShouldIgnoreShortcutTarget(textarea)).toBe(true)
    expect(shelfViewShouldIgnoreShortcutTarget(editable)).toBe(true)
    expect(shelfViewShouldIgnoreShortcutTarget(document.createElement('button'))).toBe(false)
    expect(shelfViewShouldIgnoreShortcutTarget(null)).toBe(false)
  })
})
