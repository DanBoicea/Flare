import { useI18n } from '../i18n/context'

/** Top right. Shows the other language's name and switches instantly; the text typed so far is kept. */
export function LanguageButton() {
  const { language, setLanguage, t } = useI18n()
  const other = language === 'ro' ? 'en' : 'ro'
  return (
    // The aria-label starts with the visible name, so voice control ("tap English") still works.
    <button type="button" className="language-button" aria-label={t('language.switch')} onClick={() => setLanguage(other)}>
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
        <circle cx="12" cy="12" r="10" />
        <path d="M2 12h20M12 2a15 15 0 0 1 0 20M12 2a15 15 0 0 0 0 20" />
      </svg>
      <span lang={other}>{t('language.other')}</span>
    </button>
  )
}
