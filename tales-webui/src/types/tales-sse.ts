export interface LibrarySseDto {
  libraryId: string,
}

export interface SeriesSseDto {
  seriesId: string,
  libraryId: string,
}

export interface BookSseDto {
  bookId: string,
  seriesId: string,
  libraryId: string,
}

export interface ReadProgressSseDto {
  bookId: string,
  userId: string,
}

export interface ReadProgressSeriesSseDto {
  seriesId: string,
  userId: string,
}

export interface ThumbnailBookSseDto {
  bookId: string,
  seriesId: string,
  selected: boolean,
}

export interface ThumbnailSeriesSseDto {
  seriesId: string,
  selected: boolean,
}

export interface SessionExpiredDto {
  userId: string,
}

export interface TaskQueueSseDto {
  count: number,
  countByType: { [key: string]: number }
}

export interface BookImportSseDto {
  bookId?: string,
  sourceFile: string,
  success: boolean,
  message?: string,
}
