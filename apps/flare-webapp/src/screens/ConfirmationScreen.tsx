import { useI18n } from '../i18n/context'

/**
 * Confirmation (design 05). No status page (hotline model). The wording stays true for every path here,
 * including "Send anyway", whose Report waits for the duty manager's review instead of going to a Team.
 */
export function ConfirmationScreen({ onAnother }: { onAnother: () => void }) {
  const { t } = useI18n()
  return (
    <div className="screen confirmation">
      <div className="confirmation__check" aria-hidden="true">
        <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
          <path d="M5 12.5l4.5 4.5L19 7.5" />
        </svg>
      </div>
      <h1 className="screen__title">{t('confirm.title')}</h1>
      <p className="confirmation__next">{t('confirm.next')}</p>
      <button type="button" className="button" onClick={onAnother}>
        {t('confirm.another')}
      </button>
    </div>
  )
}
