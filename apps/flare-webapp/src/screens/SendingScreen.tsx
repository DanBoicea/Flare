import { useEffect, useState } from 'react'
import { useI18n } from '../i18n/context'

const SLOW_AFTER_MS = 6000

/** Shown while the server checks the message (the LLM call can take a few seconds). */
export function SendingScreen() {
  const { t } = useI18n()
  const [slow, setSlow] = useState(false)

  useEffect(() => {
    const timer = setTimeout(() => setSlow(true), SLOW_AFTER_MS)
    return () => clearTimeout(timer)
  }, [])

  return (
    <div className="screen" aria-live="polite" aria-busy="true">
      <h1 className="screen__title">{t('sending.checking')}</h1>
      {slow && <p>{t('sending.slow')}</p>}
    </div>
  )
}
