import {normalizeLibrarySort, sortLibraries} from '@/functions/library-sort'
import {LibraryDto} from '@/types/tales-libraries'

function library(fields: Partial<LibraryDto>): LibraryDto {
  return {
    id: fields.name ?? 'library',
    name: 'library',
    createdDate: '2026-01-01T00:00:00',
    ...fields,
  } as LibraryDto
}

describe('sortLibraries', () => {
  it('keeps the hand-made order for MANUAL', () => {
    const libraries = [
      library({name: 'Comics', order: 2}),
      library({name: 'Art', order: 0}),
      library({name: 'Books', order: 1}),
    ]

    expect(sortLibraries(libraries, 'MANUAL').map(it => it.name)).toEqual(['Art', 'Books', 'Comics'])
  })

  it('leaves a folder with no saved order where the server put it', () => {
    // The comparator returns NaN for a missing order, which sorts as equal, so
    // the server's own name order survives. This is what the list did before
    // there was a choice of order, and MANUAL has to keep doing it.
    const libraries = [
      library({name: 'Art'}),
      library({name: 'Books'}),
      library({name: 'Comics'}),
    ]

    expect(sortLibraries(libraries, 'MANUAL').map(it => it.name)).toEqual(['Art', 'Books', 'Comics'])
  })

  it('orders by name, counting numbers as numbers', () => {
    const libraries = [
      library({name: 'Folder 10', order: 0}),
      library({name: 'Folder 2', order: 1}),
      library({name: 'folder 1', order: 2}),
    ]

    expect(sortLibraries(libraries, 'NAME').map(it => it.name)).toEqual(['folder 1', 'Folder 2', 'Folder 10'])
  })

  it('orders by name for a non-latin script', () => {
    const libraries = [
      library({name: '만화', order: 0}),
      library({name: '그림', order: 1}),
      library({name: '소설', order: 2}),
    ]

    expect(sortLibraries(libraries, 'NAME').map(it => it.name)).toEqual(['그림', '만화', '소설'])
  })

  it('orders by creation date, oldest first, and falls back to the name on a tie', () => {
    const libraries = [
      library({name: 'Third', createdDate: '2026-03-01T09:00:00', order: 0}),
      library({name: 'Second B', createdDate: '2026-02-01T09:00:00', order: 1}),
      library({name: 'Second A', createdDate: '2026-02-01T09:00:00', order: 2}),
      library({name: 'First', createdDate: '2026-01-01T09:00:00', order: 3}),
    ]

    expect(sortLibraries(libraries, 'CREATED').map(it => it.name))
      .toEqual(['First', 'Second A', 'Second B', 'Third'])
  })

  it('falls back to the name when a server sends no creation date', () => {
    const libraries = [
      library({name: 'Comics', createdDate: undefined as unknown as string, order: 0}),
      library({name: 'Art', createdDate: undefined as unknown as string, order: 1}),
    ]

    expect(sortLibraries(libraries, 'CREATED').map(it => it.name)).toEqual(['Art', 'Comics'])
  })

  it('does not reorder the array it was given', () => {
    const libraries = [
      library({name: 'Comics', order: 0}),
      library({name: 'Art', order: 1}),
    ]

    sortLibraries(libraries, 'NAME')

    expect(libraries.map(it => it.name)).toEqual(['Comics', 'Art'])
  })
})

describe('normalizeLibrarySort', () => {
  it('accepts the three known orders', () => {
    expect(normalizeLibrarySort('MANUAL')).toBe('MANUAL')
    expect(normalizeLibrarySort('NAME')).toBe('NAME')
    expect(normalizeLibrarySort('CREATED')).toBe('CREATED')
  })

  it('falls back to the manual order for anything else', () => {
    expect(normalizeLibrarySort('name')).toBe('MANUAL')
    expect(normalizeLibrarySort(undefined)).toBe('MANUAL')
    expect(normalizeLibrarySort('SIZE')).toBe('MANUAL')
  })
})
