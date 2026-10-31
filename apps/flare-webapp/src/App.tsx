import { useState } from 'react'
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
  | { name: 'compose'; error?: ErrorCode | 'NETWORK' }
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
  const [contact, setContact] = useState('')
  const [website, setWebsite] = useState('') // honeypot (ADR-0019)

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
        contact: contact.trim() || null,
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
    setContact('')
    setScreen({ name: 'compose' })
  }

  return (
    <div className="app">
      <EmergencyStrip />
      <header className="app__header">
        <span className="brand__plate">Flare</span>
        <LanguageButton />
        <span className="brand__place">{t('app.airport')}</span>
      </header>

      <main className="app__main">
        {screen.name === 'compose' && (
          <ComposeScreen
            text={text}
            onTextChange={setText}
            contact={contact}
            onContactChange={setContact}
            website={website}
            onWebsiteChange={setWebsite}
            error={screen.error}
            onSend={() => send()}
          />
        )}
        {screen.name === 'sending' && <SendingScreen />}
        {screen.name === 'answer' && (
          <AnswerScreen
            answer={screen.answer}
            onHelped={startOver}
            onSendToStaff={() => sendToStaff(screen.questionId)}
          />
        )}
        {screen.name === 'bounced' && (
          <BouncedScreen
            bounce={screen.bounce}
            onEdit={() => setScreen({ name: 'compose' })}
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
