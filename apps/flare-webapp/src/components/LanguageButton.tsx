import { useI18n } from '../i18n/context'
import { Flag } from './Flag'

/**
 * Top right. Shows the *other* language as a flag plus its code ("RO" / "EN") and switches instantly;
 * the text typed so far is kept. The accessible name starts with the visible code, so voice control
 * ("tap EN") works, and then names the language in full for screen readers.
 */
export function LanguageButton() {
  const { language, setLanguage, t } = useI18n()
  const other = language === 'ro' ? 'en' : 'ro'
  return (
    <button type="button" className="language-button" aria-label={t('language.switch')} onClick={() => setLanguage(other)}>
      <Flag language={other} />
      <span lang={other}>{t('language.code')}</span>
    </button>
  )
}
