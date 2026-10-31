import type { KeyboardEvent } from 'react'
import type { ErrorCode } from '../api/types'
import { useI18n } from '../i18n/context'
import { hasKey } from '../i18n/strings'

export const MAX_LENGTH = 1000
/** The counter only appears near the limit, so it never distracts a Passenger writing a short message. */
const COUNTER_FROM = 800

interface Props {
  text: string
  onTextChange: (text: string) => void
  wantsCallBack: boolean
  onWantsCallBackChange: (wants: boolean) => void
  contact: string
  onContactChange: (contact: string) => void
  website: string
  onWebsiteChange: (website: string) => void
  error?: ErrorCode | 'NETWORK'
  onSend: () => void
}

/** Compose (frontends.md, design 01/10). Focus order: strip, language, field, call back, Send. */
export function ComposeScreen(props: Props) {
  const { t } = useI18n()
  const errorKey = props.error ? `error.${props.error}` : null
  const errorText = errorKey ? (hasKey(errorKey) ? t(errorKey) : t('error.generic')) : null
  const canSend = props.text.trim().length > 0
  const showCounter = props.text.length >= COUNTER_FROM

  function send() {
    if (canSend) props.onSend()
  }

  // The keyboard's key says "Send" (enterKeyHint), so Enter sends; Shift+Enter still adds a new line.
  // isComposing: Enter that confirms an IME or dictation suggestion must not send.
  function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault()
      send()
    }
  }

  return (
    <form
      className="screen compose"
      noValidate
      onSubmit={(event) => {
        event.preventDefault()
        send()
      }}
    >
      {/* The title is the field's visible label (design 01). */}
      <h1 className="screen__title" tabIndex={-1}>
        <label htmlFor="message">{t('compose.title')}</label>
      </h1>

      {errorText && (
        <p className="notice notice--error" role="alert">
          {errorText}
        </p>
      )}

      <div className="field">
        <textarea
          id="message"
          className="compose__message"
          rows={5}
          maxLength={MAX_LENGTH}
          enterKeyHint="send"
          placeholder={t('compose.placeholder')}
          aria-describedby={showCounter ? 'message-hint message-counter' : 'message-hint'}
          value={props.text}
          onChange={(event) => props.onTextChange(event.target.value)}
          onKeyDown={onKeyDown}
        />
        <div className="compose__under">
          <p id="message-hint" className="field__hint">
            {t('compose.hint')}
          </p>
          {showCounter && (
            <p id="message-counter" className="compose__counter">
              {t('compose.counter', { count: props.text.length, max: MAX_LENGTH })}
            </p>
          )}
        </div>
      </div>

      <div className="callback">
        <label className="callback__toggle">
          <input
            type="checkbox"
            checked={props.wantsCallBack}
            aria-controls="callback-details"
            onChange={(event) => props.onWantsCallBackChange(event.target.checked)}
          />
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <path d="M5 3h4l2 5-2.5 1.5a11 11 0 0 0 6 6L16 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 5a2 2 0 0 1 2-2z" />
          </svg>
          <span>
            {t('compose.contact.toggle')} {t('compose.optional')}
          </span>
        </label>
        <div id="callback-details" className="field callback__details" hidden={!props.wantsCallBack}>
          <label htmlFor="contact">{t('compose.contact.label')}</label>
          <input
            id="contact"
            type="tel"
            inputMode="tel"
            maxLength={32}
            autoComplete="tel"
            aria-describedby="contact-note"
            value={props.contact}
            onChange={(event) => props.onContactChange(event.target.value)}
          />
          <p id="contact-note" className="field__hint">
            {t('compose.contact.note')}
          </p>
        </div>
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

      <button type="submit" className="button button--primary" disabled={!canSend}>
        {t('compose.send')}
      </button>

      <p className="compose__privacy">
        {t('compose.privacy')}
        <br />
        {t('emergency.strip.note')}
      </p>
    </form>
  )
}
