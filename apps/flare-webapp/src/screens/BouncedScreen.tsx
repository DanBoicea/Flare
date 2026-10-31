import type { Bounce } from '../api/types'
import { Quote } from '../components/Quote'
import { useI18n } from '../i18n/context'
import { hasKey } from '../i18n/strings'

interface Props {
  text: string
  bounce: Bounce
  onEdit: () => void
  onSendAnyway: () => void
}

/**
 * Bounced (design 04). Never accusing, never revealing thresholds: the screen says what to change,
 * not how many tries are left. "Send anyway" only appears when the server allows it.
 */
export function BouncedScreen({ text, bounce, onEdit, onSendAnyway }: Props) {
  const { t } = useI18n()
  // A reason added to the contract before the string files falls back to the clearest generic text.
  const reason = hasKey(`bounce.${bounce.reason}`) ? bounce.reason : 'TOO_UNCLEAR'

  return (
    <div className="screen">
      <section className="bounce">
        <span className="bounce__icon" aria-hidden="true">
          ?
        </span>
        <h1 className="screen__title">{t(`bounce.title.${reason}`)}</h1>
        <p>{t(`bounce.${reason}`)}</p>
      </section>

      <Quote label={t('sending.youWrote')} text={text} />

      <div className="screen__actions">
        <button type="button" className="button button--primary" onClick={onEdit}>
          {t('bounce.edit')}
        </button>
        {bounce.sendAnywayAvailable && (
          <button type="button" className="button" onClick={onSendAnyway}>
            {t('bounce.sendAnyway')}
          </button>
        )}
      </div>
    </div>
  )
}
