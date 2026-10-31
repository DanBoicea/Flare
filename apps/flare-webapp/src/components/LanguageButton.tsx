import { useI18n } from '../i18n/context'

/** Top right. Shows the other language's name and switches instantly. W02 finalises it. */
export function LanguageButton() {
  const { language, setLanguage, t } = useI18n()
  const other = language === 'ro' ? 'en' : 'ro'
  return (
    <button
      type="button"
      className="language-button"
      lang={other}
      aria-label={t('language.switch')}
      onClick={() => setLanguage(other)}
    >
      {t('language.other')}
    </button>
  )
}
