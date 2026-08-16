export enum ContextOrigin {
  SERIES = 'SERIES',
  LIBRARY = 'LIBRARY',
}

export interface Context {
  origin: ContextOrigin,
  id: string,
}
