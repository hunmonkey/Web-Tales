import Vue from 'vue'
import VueI18n, {LocaleMessages} from 'vue-i18n'

Vue.use(VueI18n)

/**
 * The locales the preferences picker offers. Their messages ship in the initial
 * bundle because the picker labels each entry in its own language — it calls
 * `$t('common.locale_name', locale)` — so those messages have to be present
 * before the user has chosen anything.
 *
 * Everything else is a chunk of its own, fetched only if a stored preference
 * asks for it. All 38 locale files used to be inlined into `app.js`: 1,325 KB
 * of JSON of which one locale is ever read.
 */
const eagerLocaleFiles = require.context('./locales', true, /\.\/(en|ja|ko|zh-Hans)\.json$/i)
// The regex has to be written out here rather than held in a const: webpack
// resolves require.context at build time and only reads literal arguments, so a
// variable pattern silently produces an empty context - no chunks, and an empty
// KNOWN_LOCALES that rejects every locale including the eager ones.
const lazyLocaleFiles = require.context('./locales', true, /[A-Za-z0-9-_,\s]+\.json$/i, 'lazy')

function localeFromKey(key: string): string | undefined {
  const matched = key.match(/([A-Za-z0-9-_]+)\./i)
  return matched && matched.length > 1 ? matched[1] : undefined
}

/**
 * Every locale that has a file, whether or not its messages are loaded yet.
 * `keys()` is synchronous even in lazy mode, so this costs no download.
 * Callers that used to guard on `i18n.availableLocales` must use this instead:
 * a lazy locale is legitimate but not yet available.
 */
export const KNOWN_LOCALES: string[] = lazyLocaleFiles
  .keys()
  .map(localeFromKey)
  .filter((locale): locale is string => locale !== undefined)

function eagerMessages(): LocaleMessages {
  const messages: LocaleMessages = {}
  eagerLocaleFiles.keys().forEach(key => {
    const locale = localeFromKey(key)
    if (locale) messages[locale] = eagerLocaleFiles(key)
  })
  return messages
}

const i18n = new VueI18n({
  locale: process.env.VUE_APP_I18N_LOCALE || 'en',
  fallbackLocale: process.env.VUE_APP_I18N_FALLBACK_LOCALE || 'en',
  messages: eagerMessages(),
  pluralizationRules: {
    /**
     * @param choice {number} a choice index given by the input to $tc: `$tc('path.to.rule', choiceIndex)`
     * @param choicesLength {number} an overall amount of available choices
     * @returns a final choice index to select plural word by
     */
    'pl': function (choice, choicesLength) {
      // brak stron
      if (choice === 0) {
        return 0
      }

      // 1 strona
      if (choice === 1) {
        return 1
      }

      const betweenTwoAndFour = ((choice % 10) >= 2 && (choice % 10) <= 4)
      const lessThanTen = choice < 10
      const moreThanTwenty = choice > 20

      // 2 strony, 3 strony, 4 strony, 22 strony ...
      if (betweenTwoAndFour && (lessThanTen || moreThanTwenty)) {
        return 2
      }

      // other cases, 5 stron, 67 stron, 259 stron and so on
      return (choicesLength < 4) ? 2 : 3
    },
  },
})

/**
 * Makes a locale's messages available, downloading its chunk on first use.
 * Resolves false for a locale with no file, so callers can keep the locale they
 * already have rather than switching to one that would render as fallback text.
 */
export async function loadLocale(locale: string): Promise<boolean> {
  if (!KNOWN_LOCALES.includes(locale)) return false
  if (i18n.availableLocales.includes(locale)) return true

  try {
    const loaded = await lazyLocaleFiles(`./${locale}.json`)
    i18n.setLocaleMessage(locale, loaded.default ?? loaded)
    return true
  } catch (e) {
    return false
  }
}

export default i18n
