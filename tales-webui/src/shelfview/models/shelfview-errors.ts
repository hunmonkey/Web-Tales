export type ShelfViewApiErrorCode =
  | 'BAD_REQUEST'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'PAYLOAD_TOO_LARGE'
  | 'UNAVAILABLE'
  | 'UNSUPPORTED_SCHEMA'

export class ShelfViewApiError extends Error {
  readonly code: ShelfViewApiErrorCode
  readonly status?: number

  constructor(message: string, code: ShelfViewApiErrorCode, status?: number) {
    super(message)
    this.name = 'ShelfViewApiError'
    this.code = code
    this.status = status
  }
}

export function toShelfViewApiError(error: any, fallbackMessage: string): ShelfViewApiError {
  const status = error?.response?.status
  const serverMessage = error?.response?.data?.message
  const message = serverMessage ? `${fallbackMessage}: ${serverMessage}` : fallbackMessage

  return new ShelfViewApiError(message, codeFromStatus(status), status)
}

function codeFromStatus(status?: number): ShelfViewApiErrorCode {
  switch (status) {
    case 400:
      return 'BAD_REQUEST'
    case 403:
      return 'FORBIDDEN'
    case 404:
      return 'NOT_FOUND'
    case 413:
      return 'PAYLOAD_TOO_LARGE'
    default:
      return 'UNAVAILABLE'
  }
}
