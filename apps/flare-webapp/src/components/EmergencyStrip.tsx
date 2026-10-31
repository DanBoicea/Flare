import { useI18n } from '../i18n/context'
import { PhoneIcon } from './PhoneIcon'
import './EmergencyStrip.css'

/**
 * Always visible at the top of every screen (safety boundary in CLAUDE.md). Never blocked by anything.
 * The question on the left, the action on the right: the red button carries a phone icon and a verb
 * ("Call 112"), so it reads as something to press, not as a badge.
 */
export function EmergencyStrip() {
  const { t } = useI18n()
  return (
    <div className="emergency-strip" role="note" aria-label={t('emergency.strip.note')}>
      <p className="emergency-strip__question">{t('emergency.strip')}</p>
      <a className="emergency-strip__call" href="tel:112">
        <PhoneIcon size={20} />
        {t('emergency.strip.call')}
      </a>
    </div>
  )
}
