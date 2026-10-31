import { useEffect, useState } from 'react'
import { Quote } from '../components/Quote'
import { useI18n } from '../i18n/context'

const SLOW_AFTER_MS = 6000

interface Props {
  /** What the Passenger wrote, echoed back so they know what is being checked (design 02). */
  text: string
}

/** Shown while the server checks the message (the LLM call can take a few seconds). */
export function SendingScreen({ text }: Props) {
  const { t } = useI18n()
  const [slow, setSlow] = useState(false)

  useEffect(() => {
    const timer = setTimeout(() => setSlow(true), SLOW_AFTER_MS)
    return () => clearTimeout(timer)
  }, [])

  return (
    <div className="screen sending" aria-busy="true">
      <div className="sending__pulse" aria-hidden="true">
        <span className="sending__ring sending__ring--outer" />
        <span className="sending__ring sending__ring--inner" />
        <span className="sending__dot" />
      </div>
      <h1 className="screen__title" tabIndex={-1}>
        {t('sending.checking')}
      </h1>
      {/* Polite live region: the slow-down line is announced when it appears, without interrupting. */}
      <div className="sending__status" aria-live="polite">
        {slow && <p>{t('sending.slow')}</p>}
        <p>{t('sending.keepOpen')}</p>
      </div>
      <Quote label={t('sending.youWrote')} text={text} />
    </div>
  )
}
