import { useI18n } from '../i18n/context'

/** Placeholder from W01; W03 finalises it. No status page (hotline model). */
export function ConfirmationScreen({ onAnother }: { onAnother: () => void }) {
  const { t } = useI18n()
  return (
    <div className="screen">
      <h1 className="screen__title">{t('confirm.title')}</h1>
      <p>{t('confirm.next')}</p>
      <button type="button" className="button" onClick={onAnother}>
        {t('confirm.another')}
      </button>
    </div>
  )
}
