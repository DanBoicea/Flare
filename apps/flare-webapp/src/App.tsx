import { useEffect, useRef, useState } from 'react'
import { ApiError, sendMessage, sendQuestionToStaff } from './api/client'
import type { Answer, Bounce, ErrorCode } from './api/types'
import { EmergencyStrip } from './components/EmergencyStrip'
import { LanguageButton } from './components/LanguageButton'
import { useI18n } from './i18n/context'
import { getDeviceId } from './lib/deviceId'
import { elapsedMs } from './lib/elapsed'
import { readQrCodeId } from './lib/qrCode'
import { ComposeScreen } from './screens/ComposeScreen'
import { SendingScreen } from './screens/SendingScreen'
import { AnswerScreen } from './screens/AnswerScreen'
import { BouncedScreen } from './screens/BouncedScreen'
import { ConfirmationScreen } from './screens/ConfirmationScreen'
import { EmergencyScreen } from './screens/EmergencyScreen'
import './App.css'

/** Screen state machine (frontends.md). No router: the URL only carries the QR code ID. */
type Screen =
  | { name: 'compose'; error?: ErrorCode | 'NETWORK'; focusField?: boolean }
  | { name: 'sending' }
  | { name: 'answer'; questionId: string; answer: Answer }
  | { name: 'bounced'; bounce: Bounce }
  | { name: 'confirmation' }
  | { name: 'emergency' }

const qrCodeId = readQrCodeId()
const showDemoLabel = import.meta.env.VITE_DEMO_LABEL !== 'false'

export default function App() {
  const { language, t } = useI18n()
  const [screen, setScreen] = useState<Screen>({ name: 'compose' })
  // Kept across screens so "Edit message" and errors never lose what the Passenger typed.
  const [text, setText] = useState('')
  const [wantsCallBack, setWantsCallBack] = useState(false)
  const [contact, setContact] = useState('')
  const [website, setWebsite] = useState('') // honeypot (ADR-0019)

  // On every screen change, move focus to the new screen's heading, so screen readers announce it
  // and keyboard users start from the top. Not on first load: the Passenger has not acted yet.
  // "Edit message" puts focus straight into the field instead, ready to type.
  const mainRef = useRef<HTMLElement>(null)
  const firstScreen = useRef(true)
  useEffect(() => {
    if (firstScreen.current) {
      firstScreen.current = false
      return
    }
    if (screen.name === 'compose' && screen.focusField) {
      mainRef.current?.querySelector<HTMLElement>('#message')?.focus()
      return
    }
    const heading = mainRef.current?.querySelector<HTMLElement>('h1')
    if (!heading) return
    heading.tabIndex = -1
    heading.focus()
  }, [screen])

  const fail = (error: unknown) =>
    setScreen({ name: 'compose', error: error instanceof ApiError ? error.code : 'NETWORK' })

  async function send(sendAnyway = false) {
    setScreen({ name: 'sending' })
    try {
      const response = await sendMessage({
        text: text.trim(),
        uiLanguage: language,
        qrCodeId,
        deviceId: getDeviceId(),
        // An unticked call-back box means no contact, even if a number was typed before.
        contact: (wantsCallBack && contact.trim()) || null,
        sendAnyway,
        website,
        elapsedMs: elapsedMs(),
      })
      switch (response.outcome) {
        case 'ANSWERED':
          if (response.questionId && response.answer) {
            setScreen({ name: 'answer', questionId: response.questionId, answer: response.answer })
          } else {
            setScreen({ name: 'confirmation' })
          }
          break
        case 'BOUNCED':
          if (response.bounce) setScreen({ name: 'bounced', bounce: response.bounce })
          else setScreen({ name: 'confirmation' })
          break
        case 'EMERGENCY':
          setScreen({ name: 'emergency' })
          break
        case 'REPORTED':
          setScreen({ name: 'confirmation' })
          break
      }
    } catch (error) {
      fail(error)
    }
  }

  async function sendToStaff(questionId: string) {
    setScreen({ name: 'sending' })
    try {
      await sendQuestionToStaff(questionId, getDeviceId())
      setScreen({ name: 'confirmation' })
    } catch (error) {
      fail(error)
    }
  }

  function startOver() {
    setText('')
    setWantsCallBack(false)
    setContact('')
    setScreen({ name: 'compose' })
  }

  return (
    <div className="app">
      <EmergencyStrip />
      <header className="app__header">
        <div className="brand">
          <svg className="brand__mark" width="32" height="32" viewBox="0 0 32 32" aria-hidden="true">
            <rect width="32" height="32" rx="10" fill="var(--cobalt)" />
            <g transform="translate(4 4)" fill="none" stroke="#fff" strokeWidth="2.4" strokeLinecap="round">
              <circle cx="12" cy="12" r="3.5" fill="#fff" />
              <path d="M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3" />
            </g>
          </svg>
          <div>
            <p className="brand__name">Flare</p>
            <p className="brand__place">{t('app.airport')}</p>
          </div>
        </div>
        <LanguageButton />
      </header>

      <main className="app__main" ref={mainRef}>
        {screen.name === 'compose' && (
          <ComposeScreen
            text={text}
            onTextChange={setText}
            wantsCallBack={wantsCallBack}
            onWantsCallBackChange={setWantsCallBack}
            contact={contact}
            onContactChange={setContact}
            website={website}
            onWebsiteChange={setWebsite}
            error={screen.error}
            onSend={() => send()}
          />
        )}
        {screen.name === 'sending' && <SendingScreen text={text.trim()} />}
        {screen.name === 'answer' && (
          <AnswerScreen
            question={text.trim()}
            answer={screen.answer}
            onHelped={startOver}
            onSendToStaff={() => sendToStaff(screen.questionId)}
          />
        )}
        {screen.name === 'bounced' && (
          <BouncedScreen
            text={text.trim()}
            bounce={screen.bounce}
            onEdit={() => setScreen({ name: 'compose', focusField: true })}
            onSendAnyway={() => send(true)}
          />
        )}
        {screen.name === 'confirmation' && <ConfirmationScreen onAnother={startOver} />}
        {/* The emergency message was already sent, so going back starts fresh (no accidental resend). */}
        {screen.name === 'emergency' && <EmergencyScreen onBack={startOver} />}
      </main>

      {showDemoLabel && <footer className="app__footer">{t('demo.simulated')}</footer>}
    </div>
  )
}
