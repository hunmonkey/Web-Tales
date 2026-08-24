import {LibraryDto} from '@/types/tales-libraries'
import {LIBRARY_SORT_DEFAULT, LIBRARY_SORTS, LibrarySort} from '@/types/tales-clientsettings'

/**
 * Orders the folder list in the side menu.
 *
 * `MANUAL` is the order the reorder panel writes, and is what the list did
 * before there was a choice, so it stays the default. `NAME` and `CREATED` are
 * derived from the folders themselves and never touch that hand-made order, so
 * going back to `MANUAL` restores it exactly.
 */
export function sortLibraries(
  libraries: LibraryDto[],
  sort: LibrarySort,
): LibraryDto[] {
  const ordered = [...libraries]

  switch (sort) {
    case 'NAME':
      return ordered.sort(compareLibraryName)
    case 'CREATED':
      return ordered.sort(compareLibraryCreated)
    default:
      // Left exactly as it was: a folder with no saved order compares as NaN,
      // which leaves it where the server put it rather than moving it to one end.
      return ordered.sort((a, b) => a.order - b.order)
  }
}

export function normalizeLibrarySort(value: unknown): LibrarySort {
  return LIBRARY_SORTS.includes(value as LibrarySort) ? value as LibrarySort : LIBRARY_SORT_DEFAULT
}

/**
 * `numeric` so "Folder 2" comes before "Folder 10", and the collation follows
 * the browser locale so Korean names sort as Korean rather than by code point.
 */
function compareLibraryName(a: LibraryDto, b: LibraryDto): number {
  return (a.name || '').localeCompare(b.name || '', undefined, {numeric: true, sensitivity: 'base'})
}

/** Oldest first, which is the order the folders were added in. */
function compareLibraryCreated(a: LibraryDto, b: LibraryDto): number {
  const left = Date.parse(a.createdDate ?? '')
  const right = Date.parse(b.createdDate ?? '')
  // A server that does not send the date yet leaves nothing to order by, so the
  // list falls back to names instead of shuffling.
  if (Number.isNaN(left) || Number.isNaN(right)) return compareLibraryName(a, b)
  return left - right || compareLibraryName(a, b)
}
