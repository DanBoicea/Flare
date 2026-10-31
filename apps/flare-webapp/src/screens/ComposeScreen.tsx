import type { ErrorCode } from '../api/types'
import { useI18n } from '../i18n/context'
import { hasKey } from '../i18n/strings'

const MAX_LENGTH = 1000

interface Props {
  text: string
  onTextChange: (text: string) => void
  contact: string
  onContactChange: (contact: string) => void
  website: string
  onWebsiteChange: (website: string) => void
  error?: ErrorCode | 'NETWORK'
  onSend: () => void
}

/** Placeholder from W01; W02 builds the real Compose screen (contact toggle, counter, focus, layout). */
export function ComposeScreen(props: Props) {
  const { t } = useI18n()
  const errorKey = props.error ? `error.${props.error}` : null
  const errorText = errorKey ? (hasKey(errorKey) ? t(errorKey) : t('error.generic')) : null

  return (
    <form
      className="screen"
      onSubmit={(event) => {
        event.preventDefault()
        if (props.text.trim()) props.onSend()
      }}
    >
      <h1 className="screen__title">{t('compose.title')}</h1>

      {errorText && (
        <p className="notice" role="alert">
          {errorText}
        </p>
      )}

      <div className="field">
        <label htmlFor="message">{t('compose.label')}</label>
        <textarea
          id="message"
          rows={5}
          maxLength={MAX_LENGTH}
          placeholder={t('compose.placeholder')}
          value={props.text}
          onChange={(event) => props.onTextChange(event.target.value)}
        />
      </div>

      <div className="field">
        <label htmlFor="contact">{t('compose.contact.label')}</label>
        <input
          id="contact"
          type="tel"
          inputMode="tel"
          maxLength={32}
          autoComplete="tel"
          value={props.contact}
          onChange={(event) => props.onContactChange(event.target.value)}
        />
        <small>{t('compose.contact.note')}</small>
      </div>

      {/* Honeypot (ADR-0019): invisible to people and screen readers, often filled by bots. */}
      <div className="visually-hidden" aria-hidden="true">
        <label htmlFor="website">Website</label>
        <input
          id="website"
          name="website"
          tabIndex={-1}
          autoComplete="off"
          value={props.website}
          onChange={(event) => props.onWebsiteChange(event.target.value)}
        />
      </div>

      <button type="submit" className="button button--primary" disabled={!props.text.trim()}>
        {t('compose.send')}
      </button>
      <p>{t('compose.privacy')}</p>
    </form>
  )
}
