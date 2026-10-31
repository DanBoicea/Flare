import { useI18n } from '../i18n/context'

/** Placeholder from W01; W03 finalises it. Fixed text only: no medical or security advice. */
export function EmergencyScreen({ onBack }: { onBack: () => void }) {
  const { t } = useI18n()
  return (
    <div className="screen" role="alert">
      <h1 className="screen__title">{t('emergency.title')}</h1>
      <p>{t('emergency.body')}</p>
      <a className="button button--primary" href="tel:112">
        {t('emergency.call')}
      </a>
      <button type="button" className="button" onClick={onBack}>
        {t('emergency.back')}
      </button>
    </div>
  )
}
