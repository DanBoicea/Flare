import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { I18nProvider } from '../i18n/I18nProvider'
import { SendingScreen } from './SendingScreen'

describe('SendingScreen', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    localStorage.setItem('flare.language', 'en')
  })
  afterEach(() => vi.useRealTimers())

  it('adds the slow message after 6 seconds, inside a polite live region', () => {
    render(
      <I18nProvider>
        <SendingScreen text="Broken lift" />
      </I18nProvider>,
    )
    expect(screen.queryByText('This can take a few seconds.')).not.toBeInTheDocument()
    act(() => vi.advanceTimersByTime(5999))
    expect(screen.queryByText('This can take a few seconds.')).not.toBeInTheDocument()
    act(() => vi.advanceTimersByTime(1))
    const slow = screen.getByText('This can take a few seconds.')
    expect(slow.closest('[aria-live="polite"]')).not.toBeNull()
    expect(screen.getByText('Broken lift')).toBeInTheDocument()
  })
})
