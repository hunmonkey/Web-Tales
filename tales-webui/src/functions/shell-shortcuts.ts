import {shelfViewShouldIgnoreShortcutTarget} from '@/shelfview/functions/reader-input'

export type ShellShortcut = 'FOCUS_SEARCH' | 'CLOSE_DRAWER'

export interface ShellShortcutContext {
  drawerVisible: boolean
  // False when the drawer is permanently docked, where closing it on Escape
  // would remove navigation the reader did not ask to hide.
  drawerIsOverlay: boolean
}

/**
 * Decides which shell shortcut a key press maps to, or none at all.
 * Typing is never interrupted, and browser or system chords are left alone.
 */
export function resolveShellShortcut(
  event: KeyboardEvent,
  context: ShellShortcutContext,
): ShellShortcut | undefined {
  if (event.defaultPrevented || event.ctrlKey || event.metaKey || event.altKey) return undefined
  if (shelfViewShouldIgnoreShortcutTarget(event.target)) return undefined

  if (event.key === '/') return 'FOCUS_SEARCH'
  if (event.key === 'Escape' && context.drawerVisible && context.drawerIsOverlay) return 'CLOSE_DRAWER'

  return undefined
}
