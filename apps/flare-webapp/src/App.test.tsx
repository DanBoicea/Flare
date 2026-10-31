import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { ApiError } from './api/client'
import type { MessageRequest, MessageResponse } from './api/types'
import { I18nProvider } from './i18n/I18nProvider'

// The API client is replaced, so each test decides what the server answers.
const sendMessage = vi.fn<(request: MessageRequest) => Promise<MessageResponse>>()
const sendQuestionToStaff = vi.fn()
vi.mock('./api/client', async (importActual) => {
  const actual = await importActual<typeof import('./api/client')>()
  return {
    ...actual,
    sendMessage: (request: MessageRequest) => sendMessage(request),
    sendQuestionToStaff: (...args: unknown[]) => sendQuestionToStaff(...args),
  }
})
const reply = (response: Partial<MessageResponse>): MessageResponse => ({
  outcome: 'REPORTED',
  questionId: null,
  answer: null,
  bounce: null,
  ...response,
})

function start(language: 'ro' | 'en' = 'en') {
  localStorage.setItem('flare.language', language)
  const user = userEvent.setup()
  render(
    <I18nProvider>
      <App />
    </I18nProvider>,
  )
  return user
}

const field = () => screen.getByRole('textbox', { name: /how can we help|cu ce te putem ajuta/i })
const sendButton = () => screen.getByRole('button', { name: /^(send|trimite)$/i })

beforeEach(() => {
  sendMessage.mockReset()
  sendQuestionToStaff.mockReset()
})

describe('Compose', () => {
  it('disables Send until something other than spaces is typed', async () => {
    const user = start()
    expect(sendButton()).toBeDisabled()
    await user.type(field(), '   ')
    expect(sendButton()).toBeDisabled()
    await user.type(field(), 'Broken lift')
    expect(sendButton()).toBeEnabled()
  })

  it('sends the trimmed text with the request fields, and no contact when the call-back box is unticked', async () => {
    sendMessage.mockResolvedValue(reply({ outcome: 'REPORTED' }))
    const user = start()
    await user.type(field(), '  Broken lift  ')
    // Typed a number, then unticked: the number must not be sent.
    await user.click(screen.getByRole('checkbox'))
    await user.type(screen.getByLabelText(/phone number/i), '0700000000')
    await user.click(screen.getByRole('checkbox'))
    await user.click(sendButton())

    await screen.findByRole('heading', { name: /thank you/i })
    const request = sendMessage.mock.calls[0][0]
    expect(request).toMatchObject({ text: 'Broken lift', uiLanguage: 'en', contact: null, sendAnyway: false, website: '' })
    expect(request.deviceId).toMatch(/^[0-9a-f-]{36}$/)
    expect(typeof request.elapsedMs).toBe('number')
  })

  it('sends the phone number when the call-back box is ticked', async () => {
    sendMessage.mockResolvedValue(reply({ outcome: 'REPORTED' }))
    const user = start()
    await user.type(field(), 'Broken lift')
    await user.click(screen.getByRole('checkbox'))
    await user.type(screen.getByLabelText(/phone number/i), ' 0700 000 000 ')
    await user.click(sendButton())
    await screen.findByRole('heading', { name: /thank you/i })
    expect(sendMessage.mock.calls[0][0].contact).toBe('0700 000 000')
  })

  it('sends on Enter, and adds a new line on Shift+Enter', async () => {
    sendMessage.mockResolvedValue(reply({ outcome: 'REPORTED' }))
    const user = start()
    await user.type(field(), 'Line one{Shift>}{Enter}{/Shift}Line two')
    expect(sendMessage).not.toHaveBeenCalled()
    await user.keyboard('{Enter}')
    await screen.findByRole('heading', { name: /thank you/i })
    expect(sendMessage.mock.calls[0][0].text).toBe('Line one\nLine two')
  })

  it('shows the counter only near the 1000-character limit', async () => {
    const user = start()
    await user.click(field())
    await user.paste('x'.repeat(799))
    expect(screen.queryByText(/of 1000 characters/)).not.toBeInTheDocument()
    await user.paste('x')
    expect(screen.getByText('800 of 1000 characters')).toBeInTheDocument()
  })

  it('keeps the honeypot out of reach of people and screen readers', () => {
    start()
    const honeypot = document.getElementById('website')!
    expect(honeypot).toHaveAttribute('tabindex', '-1')
    expect(honeypot.closest('[aria-hidden="true"]')).not.toBeNull()
  })
})

describe('errors', () => {
  it('returns to Compose with the error and the text kept when the network fails', async () => {
    sendMessage.mockRejectedValue(new ApiError(0, 'NETWORK'))
    const user = start()
    await user.type(field(), 'Broken lift')
    await user.click(sendButton())
    expect(await screen.findByRole('alert')).toHaveTextContent(/couldn't send your message/i)
    expect(field()).toHaveValue('Broken lift')
  })

  it('shows the generic error for an unknown server code', async () => {
    sendMessage.mockRejectedValue(new ApiError(500, 'SOMETHING_NEW' as never))
    const user = start()
    await user.type(field(), 'Broken lift')
    await user.click(sendButton())
    expect(await screen.findByRole('alert')).toHaveTextContent(/something went wrong/i)
  })
})

describe('outcomes', () => {
  it('Answer: labels the answer as automatic and possibly wrong, and moves focus to the heading', async () => {
    sendMessage.mockResolvedValue(
      reply({ outcome: 'ANSWERED', questionId: 'q1', answer: { text: 'Next to belt 2.', found: true } }),
    )
    const user = start()
    await user.type(field(), 'Where is the baggage office?')
    await user.click(sendButton())

    const heading = await screen.findByRole('heading', { name: 'Automatic answer' })
    expect(heading).toHaveFocus()
    expect(screen.getByText('Next to belt 2.')).toBeInTheDocument()
    expect(screen.getByText(/it can be wrong/i)).toBeInTheDocument()
    expect(screen.getByText('Where is the baggage office?')).toBeInTheDocument()
    expect(document.title).toBe('Automatic answer – Flare')
  })

  it('Answer not found: "Ask staff" is the main action and sends the Question to staff', async () => {
    sendMessage.mockResolvedValue(
      reply({ outcome: 'ANSWERED', questionId: 'q2', answer: { text: 'unused', found: false } }),
    )
    sendQuestionToStaff.mockResolvedValue({ outcome: 'REPORTED' })
    const user = start()
    await user.type(field(), 'Is there wifi?')
    await user.click(sendButton())

    const askStaff = await screen.findByRole('button', { name: 'Ask staff' })
    expect(askStaff).toHaveClass('button--primary')
    expect(screen.queryByText('unused')).not.toBeInTheDocument()
    await user.click(askStaff)
    await screen.findByRole('heading', { name: /thank you/i })
    expect(sendQuestionToStaff).toHaveBeenCalledWith('q2', expect.any(String))
  })

  it('Bounced: "Edit message" returns with the text kept and focus in the field', async () => {
    sendMessage.mockResolvedValue(
      reply({ outcome: 'BOUNCED', bounce: { reason: 'JOKE_OR_NONSENSE', sendAnywayAvailable: false } }),
    )
    const user = start()
    await user.type(field(), 'asdf')
    await user.click(sendButton())

    await screen.findByRole('heading', { name: "We couldn't understand your message." })
    expect(screen.queryByRole('button', { name: 'Send anyway' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Edit message' }))
    expect(field()).toHaveValue('asdf')
    expect(field()).toHaveFocus()
  })

  it('Bounced: "Send anyway" resends with sendAnyway when the server allows it', async () => {
    sendMessage
      .mockResolvedValueOnce(
        reply({ outcome: 'BOUNCED', bounce: { reason: 'TOO_UNCLEAR', sendAnywayAvailable: true } }),
      )
      .mockResolvedValueOnce(reply({ outcome: 'REPORTED' }))
    const user = start()
    await user.type(field(), 'lift')
    await user.click(sendButton())
    await user.click(await screen.findByRole('button', { name: 'Send anyway' }))
    await screen.findByRole('heading', { name: /thank you/i })
    expect(sendMessage.mock.calls[1][0]).toMatchObject({ text: 'lift', sendAnyway: true })
  })

  it('Confirmation: "Send another message" starts with an empty field', async () => {
    sendMessage.mockResolvedValue(reply({ outcome: 'REPORTED' }))
    const user = start()
    await user.type(field(), 'Broken lift')
    await user.click(sendButton())
    await user.click(await screen.findByRole('button', { name: 'Send another message' }))
    expect(field()).toHaveValue('')
  })

  it('Emergency: shows 112 instructions in both languages and a call button', async () => {
    sendMessage.mockResolvedValue(reply({ outcome: 'EMERGENCY' }))
    const user = start('ro')
    await user.type(field(), 'Foc la poarta 2')
    await user.click(sendButton())

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Dacă cineva este în pericol, sună acum la 112.')
    expect(alert).toHaveTextContent('If someone is in danger, call 112 now.')
    expect(alert.querySelector('[lang="en"]')).not.toBeNull()
    expect(screen.getAllByRole('link', { name: /112/ }).every((a) => a.getAttribute('href') === 'tel:112')).toBe(true)
  })
})

describe('on every screen', () => {
  it('shows the 112 strip on Compose and on every outcome', async () => {
    const outcomes: MessageResponse[] = [
      reply({ outcome: 'REPORTED' }),
      reply({ outcome: 'EMERGENCY' }),
      reply({ outcome: 'BOUNCED', bounce: { reason: 'ABUSIVE', sendAnywayAvailable: false } }),
      reply({ outcome: 'ANSWERED', questionId: 'q', answer: { text: 'A', found: true } }),
    ]
    for (const outcome of outcomes) {
      sendMessage.mockResolvedValueOnce(outcome)
      const user = start()
      expect(screen.getByRole('note', { name: /not an emergency line/i })).toBeInTheDocument()
      await user.type(field(), 'text')
      await user.click(sendButton())
      await waitFor(() => expect(screen.queryByRole('heading', { name: /checking/i })).not.toBeInTheDocument())
      const strip = screen.getByRole('note', { name: /not an emergency line/i })
      expect(strip.querySelector('a[href="tel:112"]')).not.toBeNull()
      cleanup()
    }
  })
})

describe('language', () => {
  it('switches instantly, keeps the typed text, and updates lang and the button', async () => {
    const user = start('en')
    await user.type(field(), 'My bag is missing')
    const button = screen.getByRole('button', { name: /^RO, Română/ })
    expect(button).toHaveTextContent('RO')

    await user.click(button)
    expect(document.documentElement.lang).toBe('ro')
    expect(screen.getByRole('heading', { name: 'Cu ce te putem ajuta?' })).toBeInTheDocument()
    expect(field()).toHaveValue('My bag is missing')
    expect(screen.getByRole('button', { name: /^EN, English/ })).toHaveTextContent('EN')
    expect(document.title).toBe('Trimite un mesaj – Flare')
  })
})
