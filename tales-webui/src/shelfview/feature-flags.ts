const enabledValues = ['1', 'true', 'yes', 'on']

export function isShelfViewReaderEnabled(value: string | undefined = process.env.VUE_APP_SHELFVIEW_READER): boolean {
  return enabledValues.includes((value || '').toLowerCase())
}

export function isShelfViewEpubBridgeEnabled(value: string | undefined = process.env.VUE_APP_SHELFVIEW_EPUB_BRIDGE): boolean {
  return enabledValues.includes((value || '').toLowerCase())
}
