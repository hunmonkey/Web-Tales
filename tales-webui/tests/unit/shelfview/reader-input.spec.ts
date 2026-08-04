import {
  shelfViewCenterTapAction,
  shelfViewReaderKeyboardAction,
  shelfViewShouldIgnoreShortcutTarget,
  shelfViewTapAction,
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
    expect(shelfViewReaderKeyboardAction('r', 'LTR')).toBe('ROTATE')
    expect(shelfViewReaderKeyboardAction('R', 'LTR')).toBe('ROTATE')
    expect(shelfViewReaderKeyboardAction('f', 'LTR')).toBe('TOGGLE_FULLSCREEN')
    expect(shelfViewReaderKeyboardAction('F', 'LTR')).toBe('TOGGLE_FULLSCREEN')
    expect(shelfViewReaderKeyboardAction('?', 'LTR')).toBe('SHOW_SHORTCUTS')
    expect(shelfViewReaderKeyboardAction('F1', 'LTR')).toBe('SHOW_SHORTCUTS')
    expect(shelfViewReaderKeyboardAction('x', 'LTR')).toBeUndefined()
  })

  it('maps the tool and zoom keys the fallback readers already use', () => {
    expect(shelfViewReaderKeyboardAction('t', 'LTR')).toBe('TOGGLE_CONTENTS')
    expect(shelfViewReaderKeyboardAction('T', 'LTR')).toBe('TOGGLE_CONTENTS')
    expect(shelfViewReaderKeyboardAction('b', 'LTR')).toBe('TOGGLE_BOOKMARKS')
    expect(shelfViewReaderKeyboardAction('s', 'LTR')).toBe('TOGGLE_SETTINGS')
    expect(shelfViewReaderKeyboardAction('g', 'LTR')).toBe('FOCUS_PAGE_JUMP')
    expect(shelfViewReaderKeyboardAction('+', 'LTR')).toBe('ZOOM_IN')
    expect(shelfViewReaderKeyboardAction('=', 'LTR')).toBe('ZOOM_IN')
    expect(shelfViewReaderKeyboardAction('-', 'LTR')).toBe('ZOOM_OUT')
    expect(shelfViewReaderKeyboardAction('0', 'LTR')).toBe('ZOOM_RESET')
  })

  it('maps horizontal touch swipes and ignores vertical scroll gestures', () => {
    expect(shelfViewTouchAction({x: 200, y: 100}, {x: 40, y: 104}, 'LTR')).toBe('NEXT')
    expect(shelfViewTouchAction({x: 40, y: 100}, {x: 200, y: 104}, 'LTR')).toBe('PREVIOUS')
    expect(shelfViewTouchAction({x: 200, y: 100}, {x: 40, y: 104}, 'RTL')).toBe('PREVIOUS')
    expect(shelfViewTouchAction({x: 40, y: 100}, {x: 200, y: 104}, 'RTL')).toBe('NEXT')
    expect(shelfViewTouchAction({x: 100, y: 100}, {x: 120, y: 260}, 'LTR')).toBeUndefined()
  })

  it('does not turn the page when the swipe panned a zoomed page', () => {
    const swipeLeft = (stage: {canPanHorizontally: boolean, scrolledHorizontally: boolean}) =>
      shelfViewTouchAction({x: 200, y: 100}, {x: 40, y: 104}, 'LTR', stage)

    expect(swipeLeft({canPanHorizontally: false, scrolledHorizontally: false})).toBe('NEXT')
    expect(swipeLeft({canPanHorizontally: true, scrolledHorizontally: false})).toBeUndefined()
    expect(swipeLeft({canPanHorizontally: false, scrolledHorizontally: true})).toBeUndefined()
  })

  it('maps only short center taps to chrome toggle', () => {
    expect(shelfViewCenterTapAction({x: 500, y: 100}, {x: 504, y: 106}, 1000)).toBe('TOGGLE_CHROME')
    expect(shelfViewCenterTapAction({x: 100, y: 100}, {x: 104, y: 106}, 1000)).toBeUndefined()
    expect(shelfViewCenterTapAction({x: 500, y: 100}, {x: 540, y: 106}, 1000)).toBeUndefined()
    expect(shelfViewCenterTapAction({x: 500, y: 100}, {x: 504, y: 126}, 1000)).toBeUndefined()
    expect(shelfViewCenterTapAction({x: 500, y: 100}, {x: 504, y: 106}, 0)).toBeUndefined()
  })

  it('maps tap zones by reading direction only when paging is enabled', () => {
    expect(shelfViewTapAction({x: 100, y: 100}, {x: 104, y: 106}, 1000, 'LTR', true)).toBe('PREVIOUS')
    expect(shelfViewTapAction({x: 900, y: 100}, {x: 904, y: 106}, 1000, 'LTR', true)).toBe('NEXT')
    expect(shelfViewTapAction({x: 500, y: 100}, {x: 504, y: 106}, 1000, 'LTR', true)).toBe('TOGGLE_CHROME')
    expect(shelfViewTapAction({x: 100, y: 100}, {x: 104, y: 106}, 1000, 'RTL', true)).toBe('NEXT')
    expect(shelfViewTapAction({x: 900, y: 100}, {x: 904, y: 106}, 1000, 'RTL', true)).toBe('PREVIOUS')
    expect(shelfViewTapAction({x: 100, y: 100}, {x: 104, y: 106}, 1000, 'LTR', false)).toBeUndefined()
    expect(shelfViewTapAction({x: 500, y: 100}, {x: 504, y: 106}, 1000, 'LTR', false)).toBe('TOGGLE_CHROME')
    expect(shelfViewTapAction({x: 900, y: 100}, {x: 940, y: 106}, 1000, 'LTR', true)).toBeUndefined()
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
