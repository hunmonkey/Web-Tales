import {resolveShellShortcut} from '@/functions/shell-shortcuts'

function makeEvent(key: string, overrides: Record<string, unknown> = {}): KeyboardEvent {
  return {
    key,
    target: document.createElement('div'),
    defaultPrevented: false,
    ctrlKey: false,
    metaKey: false,
    altKey: false,
    ...overrides,
  } as unknown as KeyboardEvent
}

const overlayDrawer = {drawerVisible: true, drawerIsOverlay: true}

describe('resolveShellShortcut', () => {
  it('maps slash to focusing the search box', () => {
    expect(resolveShellShortcut(makeEvent('/'), overlayDrawer)).toBe('FOCUS_SEARCH')
  })

  it('never steals a key press while the user is typing', () => {
    for (const tag of ['input', 'textarea', 'select']) {
      const target = document.createElement(tag)
      expect(resolveShellShortcut(makeEvent('/', {target}), overlayDrawer)).toBeUndefined()
    }

    const editable = document.createElement('div')
    editable.setAttribute('contenteditable', 'true')
    expect(resolveShellShortcut(makeEvent('/', {target: editable}), overlayDrawer)).toBeUndefined()
  })

  it('leaves browser and system chords alone', () => {
    expect(resolveShellShortcut(makeEvent('/', {ctrlKey: true}), overlayDrawer)).toBeUndefined()
    expect(resolveShellShortcut(makeEvent('/', {metaKey: true}), overlayDrawer)).toBeUndefined()
    expect(resolveShellShortcut(makeEvent('/', {altKey: true}), overlayDrawer)).toBeUndefined()
    expect(resolveShellShortcut(makeEvent('/', {defaultPrevented: true}), overlayDrawer)).toBeUndefined()
  })

  it('closes the drawer on Escape only when it is an overlay', () => {
    expect(resolveShellShortcut(makeEvent('Escape'), overlayDrawer)).toBe('CLOSE_DRAWER')
    expect(resolveShellShortcut(makeEvent('Escape'), {drawerVisible: true, drawerIsOverlay: false})).toBeUndefined()
    expect(resolveShellShortcut(makeEvent('Escape'), {drawerVisible: false, drawerIsOverlay: true})).toBeUndefined()
  })

  it('ignores keys it does not handle', () => {
    expect(resolveShellShortcut(makeEvent('a'), overlayDrawer)).toBeUndefined()
    expect(resolveShellShortcut(makeEvent('Enter'), overlayDrawer)).toBeUndefined()
  })
})
