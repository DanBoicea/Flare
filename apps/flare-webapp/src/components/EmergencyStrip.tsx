import { useI18n } from '../i18n/context'
import './EmergencyStrip.css'

/** Always visible at the top of every screen (safety boundary in CLAUDE.md). Never blocked by anything. */
export function EmergencyStrip() {
  const { t } = useI18n()
  return (
    <div className="emergency-strip" role="note" aria-label={t('emergency.strip.note')}>
      <p>{t('emergency.strip')}</p>
      <a className="emergency-strip__call" href="tel:112">
        {t('emergency.strip.call')}
        <span className="visually-hidden">, {t('emergency.strip.callSuffix')}</span>
      </a>
    </div>
  )
}
