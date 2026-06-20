import { PageDto } from '@/types/tales-books'

export function isPageLandscape (p: PageDto): boolean {
  return (p?.width ?? 0) > (p?.height ?? 0)
}
