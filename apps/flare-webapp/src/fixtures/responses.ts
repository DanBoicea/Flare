// Mock responses, typed against the contract. Simulated data only. D05 extends this file.
import type { MessageResponse } from '../api/types'

export const mockResponses = {
  reported: { outcome: 'REPORTED', questionId: null, answer: null, bounce: null },
  answered: {
    outcome: 'ANSWERED',
    questionId: '00000000-0000-4000-8000-000000000001',
    answer: {
      text: 'Biroul de bagaje se află în zona de sosiri, lângă banda 2 (informație simulată).',
      found: true,
    },
    bounce: null,
  },
  answeredNotFound: {
    outcome: 'ANSWERED',
    questionId: '00000000-0000-4000-8000-000000000002',
    answer: { text: 'Nu am găsit un răspuns la această întrebare.', found: false },
    bounce: null,
  },
  emergency: { outcome: 'EMERGENCY', questionId: null, answer: null, bounce: null },
  bounced: (sendAnywayAvailable: boolean): MessageResponse => ({
    outcome: 'BOUNCED',
    questionId: null,
    answer: null,
    bounce: { reason: 'JOKE_OR_NONSENSE', sendAnywayAvailable },
  }),
} satisfies Record<string, MessageResponse | ((sendAnywayAvailable: boolean) => MessageResponse)>
