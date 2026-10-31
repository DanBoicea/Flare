// Mock responses, typed against the contract. Simulated data only. D05 extends this file.
// Answers come in the Passenger's UI language, like the real back end (uiLanguage in the request).
import type { MessageResponse, UiLanguage } from '../api/types'

const answerText: Record<UiLanguage, { found: string; notFound: string }> = {
  ro: {
    found: 'Biroul de bagaje se află în zona de sosiri, lângă banda 2 (informație simulată).',
    notFound: 'Nu am găsit un răspuns la această întrebare.',
  },
  en: {
    found: 'The baggage office is in the arrivals area, next to belt 2 (simulated information).',
    notFound: "We couldn't find an answer to this question.",
  },
}

export const mockResponses = {
  reported: (): MessageResponse => ({ outcome: 'REPORTED', questionId: null, answer: null, bounce: null }),
  answered: (language: UiLanguage): MessageResponse => ({
    outcome: 'ANSWERED',
    questionId: '00000000-0000-4000-8000-000000000001',
    answer: { text: answerText[language].found, found: true },
    bounce: null,
  }),
  answeredNotFound: (language: UiLanguage): MessageResponse => ({
    outcome: 'ANSWERED',
    questionId: '00000000-0000-4000-8000-000000000002',
    answer: { text: answerText[language].notFound, found: false },
    bounce: null,
  }),
  emergency: (): MessageResponse => ({ outcome: 'EMERGENCY', questionId: null, answer: null, bounce: null }),
  bounced: (sendAnywayAvailable: boolean): MessageResponse => ({
    outcome: 'BOUNCED',
    questionId: null,
    answer: null,
    bounce: { reason: 'JOKE_OR_NONSENSE', sendAnywayAvailable },
  }),
}
