import type { Bounce } from '../api/types'
import { useI18n } from '../i18n/context'

interface Props {
  bounce: Bounce
  onEdit: () => void
  onSendAnyway: () => void
}

/** Placeholder from W01; W03 finalises it. Never accusing, never revealing thresholds. */
export function BouncedScreen({ bounce, onEdit, onSendAnyway }: Props) {
  const { t } = useI18n()
  return (
    <div className="screen">
      <h1 className="screen__title">{t(`bounce.${bounce.reason}`)}</h1>
      <button type="button" className="button button--primary" onClick={onEdit}>
        {t('bounce.edit')}
      </button>
      {bounce.sendAnywayAvailable && (
        <button type="button" className="button" onClick={onSendAnyway}>
          {t('bounce.sendAnyway')}
        </button>
      )}
    </div>
  )
}
